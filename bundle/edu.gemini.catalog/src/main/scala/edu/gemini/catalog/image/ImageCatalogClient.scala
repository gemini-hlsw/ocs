package edu.gemini.catalog.image

import java.io._
import java.net.URL
import java.nio.channels.FileLock
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.logging.Logger

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}

import scala.util.matching.Regex
import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

case class ImageSearchQuery(catalog: ImageCatalog, coordinates: Coordinates) {
  import ImageSearchQuery._

  def url: URL = catalog.queryUrl(coordinates)

  def fileName(suffix: String): String = s"img_${catalog.id}_${coordinates.toFilePart}$suffix"

  def isNearby(query: ImageSearchQuery): Boolean =
    catalog === query.catalog && isNearby(query.coordinates)

  def isNearby(c: Coordinates): Boolean = {
    val (diffRa, diffDec) = coordinates.diff(c)
    (diffRa <= maxDistance || (Angle.zero - diffRa) <= maxDistance) && (diffDec <= maxDistance || (Angle.zero - diffDec) <= maxDistance)
  }
}

object ImageSearchQuery {
  implicit val equals = Equal.equalA[ImageSearchQuery]
  val maxDistance: Angle = (ImageCatalog.defaultSize / 2).getOrElse(Angle.zero)

  implicit class DeclinationShow(val d: Declination) extends AnyVal {
    def toFilePart: String = Declination.formatDMS(d, ":", 2)
  }

  implicit class RightAscensionShow(val a: RightAscension) extends AnyVal {
    def toFilePart: String = a.toAngle.formatHMS
  }

  implicit class CoordinatesShow(val c: Coordinates) extends AnyVal {
    def toFilePart: String = s"ra_${c.ra.toFilePart}_dec_${c.dec.toFilePart}"
  }
}

case class ImageEntry(query: ImageSearchQuery, file: Path, fileSize: Long)

object ImageEntry {
  implicit val equals = Equal.equalA[ImageEntry]

  // TODO Support multiple suffixes
  val fileRegex: Regex = """img_(.*)_ra_(.*)_dec_(.*)\.fits\.gz""".r

  /**
    * Decode a file name to an image entry
    */
  def entryFromFile(file: File): Option[ImageEntry] = {
    file.getName match {
      case fileRegex(c, raStr, decStr) =>
        for {
          catalog <- ImageCatalog.byName(c)
          ra      <- Angle.parseHMS(raStr).map(RightAscension.fromAngle).toOption
          dec     <- Angle.parseDMS(decStr).toOption.map(_.toDegrees).flatMap(Declination.fromDegrees)
        } yield ImageEntry(ImageSearchQuery(catalog, Coordinates(ra, dec)), file.toPath, file.length())
      case _ => None
    }
  }
}

/**
  * This interface can be used to listen when the image is being loaded
  */
trait ImageLoadingListener {
  def downloadStarts(): Unit
  def downloadCompletes(): Unit
  def downloadError(): Unit
}

object ImageLoadingListener {
  val zero = new ImageLoadingListener {
    override def downloadStarts(): Unit = {}

    override def downloadCompletes(): Unit = {}

    override def downloadError(): Unit = {}
  }
}

object ImageCatalogClient {
  val Log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * Load an image for the given query
    */
  def loadImage(query: ImageSearchQuery, dir: Path, listener: ImageLoadingListener): Task[Option[ImageEntry]] = {

    def addToCacheAndGet(f: Path): Task[ImageEntry] = {
      val i = ImageEntry(query, f, f.toFile.length())
      // Add to cache and prune the cache
      // Note that cache pruning goes in a different thread
      StoredImagesCache.add(i) *> ImageCacheOnDisk.pruneCache *> Task.now(i)
    }

    def readImageToFile: Task[Path] =
      Task.delay {
        Log.info(s"Downloading image at ${query.url}")
        // Open the connection to the remote
        val connection = query.url.openConnection()
        val in = query.url.openStream()
        (connection.getContentType, in)
      } >>= { Function.tupled(ImageCatalogClient.imageToTmpFile(dir, query)) }

    def downloadImage: Task[ImageEntry] = {
      val task = for {
        _ <- ImagesInProgress.add(query)
        _ <- Task.delay(listener.downloadStarts()) // Inform the listener
        f <- readImageToFile
        e <- addToCacheAndGet(f)
      } yield e

      // Inform listeners at the end
      task.onFinish {
        case Some(x) => ImagesInProgress.remove(query) *> Task.now(listener.downloadError()) // Ignore
        case _       => ImagesInProgress.remove(query) *> Task.now(listener.downloadCompletes())
      }
    }

    def checkIfNeededAndDownload: Task[Option[ImageEntry]] =
      ImagesInProgress.contains(query) >>= { inProcess => if (inProcess) Task.now(None) else downloadImage.map(Some.apply) }

    // Try to find the image on the cache, else download
    StoredImagesCache.find(query) >>= { _.filter(_.file.toFile.exists()).fold(checkIfNeededAndDownload)(f => Task.now(Some(f))) }
  }

  /**
    * Download the given image URL to a temporary file and return the file
    * Note that to support the legacy progress bar we explicitly expect a ProgressBarFilterInputStream
    */
  private def imageToTmpFile(cacheDir: Path, query: ImageSearchQuery)(contentType: String, in: InputStream): Task[Path] = {
    val url = query.url

    def suffix: Task[String] =
      Option(contentType) match {
        case Some(s) if s.endsWith("hfits")                                           => Task.now(".hfits")
        case Some(s) if s.endsWith("zfits") || s == "image/x-fits"                    => Task.now(".fits.gz")
        case Some(s) if s.endsWith("fits")                                            => Task.now(".fits")
        // REL-2776 At some places on the sky DSS returns an error, the HTTP error code is ok but the body contains no image
        case Some(s) if s.contains("text/html") && url.getPath.contains("dss_search") => Task.fail(new RuntimeException("Image not found at image server"))
        case _                                                                        => Task.now(".tmp")
      }

    /**
      * Creates a filename to store the image
      */
    def tmpFileName(suffix: String): Task[String] = Task.now(query.fileName(suffix))

    def createTmpFile(fileName: String): Task[File] = Task.delay {
      File.createTempFile(".img", ".fits", cacheDir.toFile)
    }

    def moveFile(suffix: String, tmpFile: File): Task[Path] = Task.delay {
      Files.move(tmpFile.toPath, cacheDir.resolve(query.fileName(suffix)), StandardCopyOption.ATOMIC_MOVE)
    }

    def openTmpFile(file: File): Task[(FileLock, OutputStream)] = Task.delay {
      val stream = new FileOutputStream(file)
      val lock = stream.getChannel.lock()
      (lock, stream)
    }

    def readFile(in: InputStream, out: OutputStream): Task[Unit] = Task.delay {
      val buffer = new Array[Byte](8 * 1024)
      Iterator
        .continually(in.read(buffer))
        .takeWhile(-1 != _)
        .foreach(read => out.write(buffer, 0, read))
    }

    for {
      s <- suffix
      f <- tmpFileName(s)
      t <- createTmpFile(f)
      o <- openTmpFile(t)
      _ <- readFile(in, o._2).onFinish(_ => Task.delay(o._1.release()))
      f <- moveFile(s, t)
    } yield f
  }
}

// Make it easier to call from Java
abstract class ImageCatalogClient
