package edu.gemini.catalog.image

import java.io._
import java.net.URL
import java.nio.channels.FileLock
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.concurrent.ExecutorService
import java.util.logging.Logger

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}

import scala.util.matching.Regex
import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

case class ImageSearchQuery(catalog: ImageCatalog, coordinates: Coordinates) {
  import ImageSearchQuery._

  def url: NonEmptyList[URL] = catalog.queryUrl(coordinates)

  def fileName(suffix: String): String = s"img_${catalog.id}_${coordinates.toFilePart}$suffix"

  def isNearby(query: ImageSearchQuery): Boolean =
    catalog === query.catalog && isNearby(query.coordinates)

  def isNearby(c: Coordinates): Boolean = {
    val (diffRa, diffDec) = coordinates.diff(c)
    (diffRa <= maxDistance || (Angle.zero - diffRa) <= maxDistance) && (diffDec <= maxDistance || (Angle.zero - diffDec) <= maxDistance)
  }
}

object ImageSearchQuery {
  implicit val equals: Equal[ImageSearchQuery] = Equal.equalA[ImageSearchQuery]
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
  implicit val equals: Equal[ImageEntry] = Equal.equalA[ImageEntry]

  val fileRegex: Regex = """img_(.*)_ra_(.*)_dec_(.*)\.fits.*""".r

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
  def loadImage(query: ImageSearchQuery, dir: Path, listener: ImageLoadingListener)(pool: ExecutorService): Task[Option[ImageEntry]] = {

    def addToCacheAndGet(f: Path): Task[ImageEntry] = {
      val i = ImageEntry(query, f, f.toFile.length())
      // Add to cache and prune the cache
      // Note that cache pruning goes in a different thread
      StoredImagesCache.add(i) *> ImageCacheOnDisk.pruneCache *> Task.now(i)
    }

    def readImageToFile: NonEmptyList[Task[Path]] =
      query.url.map(ImageCatalogClient.downloadImageToFile(dir, _, query.fileName))

    def downloadImage: Task[ImageEntry] = {
      val task = for {
        _ <- ImagesInProgress.start(query)
        _ <- Task.delay(listener.downloadStarts()) // Inform the listener
        f <- TaskHelper.selectFirstToComplete(readImageToFile)(pool)
        e <- addToCacheAndGet(f)
      } yield e

      // Remove query from registry and Inform listeners at the end
      task.onFinish {
        case Some(_) => ImagesInProgress.failed(query) *> Task.now(listener.downloadError())
        case _       => ImagesInProgress.completed(query) *> Task.now(listener.downloadCompletes())
      }
    }

    def checkIfNeededAndDownload: Task[Option[ImageEntry]] =
      ImagesInProgress.inProgress(query) >>= { inProcess => if (inProcess) Task.now(None) else downloadImage.map(Some.apply) }

    // Try to find the image on the cache, else download
    StoredImagesCache.find(query) >>= { _.filter(_.file.toFile.exists()).fold(checkIfNeededAndDownload)(f => Task.now(Some(f))) }
  }

  /**
    * Download the given image URL to a temporary file and return the file
    * Note that to support the legacy progress bar we explicitly expect a ProgressBarFilterInputStream
    */
  private def downloadImageToFile(cacheDir: Path, url: URL, fileName: String => String): Task[Path] = {
    case class ConnectionDescriptor(contentType: Option[String], contentEncoding: Option[String]) {

      def extension: String = (contentEncoding, contentType) match {
          case (Some("x-gzip"), _) => ".fits.gz"
          // REL-2776 At some places on the sky DSS returns an error, the HTTP error code is ok but the body contains no image
          case (None, Some(s)) if s.contains("text/html") && url.getPath.contains("dss_search") => throw new RuntimeException("Image not found at image server")
          case (None, Some(s)) if s.endsWith("fits") => ".fits"
          case _ => ".tmp"
        }
    }

    def createTmpFile: Task[File] = Task.delay {
      File.createTempFile(".img", ".fits", cacheDir.toFile)
    }

    def moveFile(suffix: String, tmpFile: File): Task[Path] = Task.delay {
      val destFileName = cacheDir.resolve(fileName(suffix))
      // If the destination file is present don't overwrite
      if (!destFileName.toFile.exists()) {
        Files.move(tmpFile.toPath, destFileName, StandardCopyOption.ATOMIC_MOVE)
      } else {
        tmpFile.delete()
        destFileName
      }
    }

    def lockTmpFile(file: File): Task[OutputStream] = Task.delay {
      new FileOutputStream(file)
    }

    def readFile(out: OutputStream): Task[Unit] = Task.delay {
      val in = url.openStream()
      val buffer = new Array[Byte](8 * 1024)
      Iterator
        .continually(in.read(buffer))
        .takeWhile(-1 != _)
        .foreach(read => out.write(buffer, 0, read))
    }

    def openConnection: Task[ConnectionDescriptor] = Task.delay {
      Log.info(s"Downloading image at $url")
      val connection = url.openConnection()
      ConnectionDescriptor(Option(connection.getContentType), Option(connection.getContentEncoding))
    }

    for {
      tempFile <- createTmpFile
      desc     <- openConnection
      output   <- lockTmpFile(tempFile)
      _        <- readFile(output)
      file     <- moveFile(desc.extension, tempFile)
    } yield file
  }
}

// Make it easier to call from Java
abstract class ImageCatalogClient
