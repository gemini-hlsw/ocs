package edu.gemini.catalog.image

import java.io._
import java.net.URL
import java.util.logging.Logger

import edu.gemini.spModel.core.{Coordinates, Declination, RightAscension}

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

case class ImageEntry(coordinates: Coordinates, catalog: ImageCatalog, file: File)

object ImageCatalogClient {
  val instance = this

  val Log = Logger.getLogger(this.getClass.getName)

  implicit class DeclinationShow(val d: Declination) extends AnyVal {
    def toFilePart: String = Declination.formatDMS(d, ":", 2)
  }

  implicit class RightAscensionShow(val a: RightAscension) extends AnyVal {
    def toFilePart: String = a.toAngle.formatHMS
  }

  implicit class CoordinatesShow(val c: Coordinates) extends AnyVal {
    def toFilePart: String = s"ra_${c.ra.toFilePart}_dec_${c.dec.toFilePart}"
  }

  def loadImage(cacheDir: File)(c: Coordinates): Task[ImageEntry] = {
    val catalog = ImageCatalog.user()
    val url = catalog.queryUrl(c)
    Task.delay {
      val connection = url.openConnection()
      val in = url.openStream()
      (c, catalog, connection.getContentType, in, cacheDir)
    } >>= { Function.tupled(ImageCatalogClient.imageToTmpFile) } >>= { case (f, _) => Task.now(ImageEntry(c, catalog, f)) }
  }

  /**
    * Attempts to find if there is an image previously downloaded at the given coordinates
    */
  def findImage(c: Coordinates, cacheDir: File): Task[Option[File]] = {
    val catalog = ImageCatalog.user()
    tmpFileName(catalog, c, ".fits.gz").map { filename =>
      val f = new File(cacheDir, filename)
      f.exists option f
    }
  }

  /**
    * Load an image and display it on the TPE or display an error
    */
  /*@deprecated
  def display4Java(display: CatalogImageDisplay, c: Coordinates, catalog: ImageCatalog): Unit = {
    val (p, f) = new ImageCatalogLoader().queryImage(c, catalog)
    f.unsafePerformAsync {
      case -\/(t) =>
        p.stop()
        DialogUtil.error(t)
      case \/-(t) =>
        Swing.onEDT {
          p.stop()
          display.setFilename(t._1.getAbsolutePath, t._2)
        }
    }
  }*/

  def tmpFileName(catalog: ImageCatalog, c: Coordinates, suffix: String): Task[String] = Task.now(s"img_${catalog.id}_${c.toFilePart}$suffix")

  /**
    * Download the given image URL to a temporary file and return the file
    * Note that to support the legacy progress bar we explicitly expect a ProgressBarFilterInputStream
    */
  private def imageToTmpFile(c: Coordinates, catalog: ImageCatalog, contentType: String, in: InputStream, cacheDir: File): Task[(File, URL)] = {
    val url = catalog.queryUrl(c)
    Log.info(s"Downloading image at $url")

    def suffix: Task[String] =
      Option(contentType) match {
        case Some(s) if s.endsWith("hfits")                                           => Task.now(".hfits")
        case Some(s) if s.endsWith("zfits") || s == "image/x-fits"                    => Task.now(".fits.gz")
        case Some(s) if s.endsWith("fits")                                            => Task.now(".fits")
        // REL-2776 At some places on the sky DSS returns an error, the HTTP error code is ok but the body contains no image
        case Some(s) if s.contains("text/html") && url.getPath.contains("dss_search") => Task.fail(new RuntimeException("Image not found at image server"))
        case _                                                                        => Task.now(".tmp")
      }

    def createTmpFile(fileName: String): Task[File] = Task.delay {
      new File(cacheDir, fileName)
    }

    def openTmpFile(file: File): Task[OutputStream] = Task.delay {
      new FileOutputStream(file)
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
      f <- tmpFileName(catalog, c, s)
      t <- createTmpFile(f)
      o <- openTmpFile(t)
      r <- readFile(in, o)
    } yield (t, url)
  }
}

abstract class ImageCatalogClient
