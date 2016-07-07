package edu.gemini.catalog.ui.tpe

import java.io._
import java.net.{URL, URLConnection}
import java.util.logging.Logger

import edu.gemini.catalog.image.ImageCatalog
import edu.gemini.spModel.core.{Coordinates, Declination, RightAscension}
import jsky.util.Preferences
import jsky.util.gui._

import scala.swing.Swing
import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class ImageEntry(coordinates: Coordinates, catalog: ImageCatalog, file: File)

object ImageCatalogLoader {
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

  def loadImage(c: Coordinates): Task[ImageEntry] = {
    val catalog = ImageCatalog.user()
    val url = catalog.queryUrl(c)
    Task {
      val connection = url.openConnection()
      val in = url.openStream()
      (c, catalog, connection.getContentType, in)
    } >>= { Function.tupled(ImageCatalogLoader.imageToTmpFile) } >>= { case (f, _) => Task.now(ImageEntry(c, catalog, f)) }
  }

  /**
    * Load an image and display it on the TPE or display an error
    */
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
  }

  /**
    * Download the given image URL to a temporary file and return the file
    * Note that to support the legacy progress bar we explicitly expect a ProgressBarFilterInputStream
    */
  private def imageToTmpFile(c: Coordinates, catalog: ImageCatalog, contentType: String, in: InputStream): Task[(File, URL)] = {
    val url = catalog.queryUrl(c)
    Log.info(s"Downloading image at $url")
    val dir = Preferences.getPreferences.getCacheDir

    def suffix: Task[String] =
      Option(contentType) match {
        case Some(s) if s.endsWith("hfits")                                           => Task.now(".hfits")
        case Some(s) if s.endsWith("zfits") || s == "image/x-fits"                    => Task.now(".fits.gz")
        case Some(s) if s.endsWith("fits")                                            => Task.now(".fits")
        // REL-2776 At some places on the sky DSS returns an error, the HTTP error code is ok but the body contains no image
        case Some(s) if s.contains("text/html") && url.getPath.contains("dss_search") => Task.fail(new RuntimeException("Image not found at image server"))
        case _                                                                        => Task.now(".tmp")
      }

    def createTmpFile(suffix: String): Task[File] = Task {
      new File(dir, s"img_${catalog.id}_${c.toFilePart}")
    }

    def openTmpFile(file: File): Task[OutputStream] = Task {
      new FileOutputStream(file)
    }

    def readFile(in: InputStream, out: OutputStream): Task[Unit] = Task {
      val buffer = new Array[Byte](8 * 1024)
      Iterator
        .continually(in.read(buffer))
        .takeWhile(-1 != _)
        .foreach(read => out.write(buffer, 0, read))
    }

    for {
      s <- suffix
      t <- createTmpFile(s)
      o <- openTmpFile(t)
      r <- readFile(in, o)
    } yield (t, url)
  }
}

/**
  * Class able to retrieve images from the old catalog and put them on display
  */
class ImageCatalogLoader {

  /**
    * Retrieve image query and pass it to the display
    */
  def queryImage(c: Coordinates, catalog: ImageCatalog): (ProgressPanel, Task[(File, URL)]) = {

    // This isn't very nice, we are mixing UI with IO but the ProgressPanel is required for now
    val progress = ProgressPanel.makeProgressPanel("Accessing catalog server ...")

    def imageStreams: Task[(URLConnection, ProgressBarFilterInputStream)] = Task {
      val url = catalog.queryUrl(c)
      val connection = progress.openConnection(url)
      val in = progress.getLoggedInputStream(url)
      (connection, in)
    }

    val r = imageStreams.flatMap { case (conn, u) => ImageCatalogLoader.imageToTmpFile(c, catalog, conn.getContentType, u) }
    (progress, r)
  }
}
