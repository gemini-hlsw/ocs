package edu.gemini.catalog.ui.tpe

import java.io._
import java.net.URL

import jsky.catalog.{URLQueryResult, QueryResult}
import jsky.util.Preferences
import jsky.util.gui._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing
import scala.util.{Success, Failure}
import scalaz.{-\/, \/-, \/}

object ImageCatalogLoader {
  val instance = this

  /**
    * Load an image and display it on the TPE or display an error
    */
  def display4Java(display: CatalogImageDisplay, url: URL):Unit = {
    val f = new ImageCatalogLoader().queryImage(url)
    f.onComplete {
      case Failure(t) =>
        DialogUtil.error(t)
      case Success(-\/(t)) =>
        DialogUtil.error(t)
      case Success(\/-(t)) =>
        Swing.onEDT {
          display.setFilename(t._1.getAbsolutePath, t._2)
        }
    }
  }
}

/**
  * Class able to retrieve images from the old catalog and put them on display
  */
class ImageCatalogLoader {

  /**
    * Download the given image URL to a temporary file and return the file
    * Note that to support the legacy progress bar we explicitly expect a ProgressBarFilterInputStream
    */
  private def imageToTmpFile(url: URL, contentType: String, in: ProgressBarFilterInputStream): Throwable \/ (File, URL) = {
    val dir = Preferences.getPreferences.getCacheDir.getPath
    val suffix = Option(contentType) match {
      case Some(s) if s.endsWith("hfits")                        =>  ".hfits"
      case Some(s) if s.endsWith("zfits") || s == "image/x-fits" => ".fits.gz"
      case Some(s) if s.endsWith("fits")                         => ".fits"
      case _                                                     => ".tmp"
    }

    def openTmpFile(): (File, OutputStream) = {
      val file = File.createTempFile("jsky", suffix, new File(dir))
      (file, new FileOutputStream(file))
    }

    def readFile(in: ProgressBarFilterInputStream, out: OutputStream): Unit = {
      val buffer = new Array[Byte](8 * 1024)
      Iterator
        .continually(in.read(buffer))
        .takeWhile(-1 != _)
        .foreach(read => out.write(buffer, 0, read))
    }

    for {
      f <- \/.fromTryCatch(openTmpFile())
      i <- \/.fromTryCatch(readFile(in, f._2))
    } yield (f._1, url)
  }

  /**
    * Retrieve image query and pass it to the display
    */
  def queryImage(url: URL):Future[Throwable \/ (File, URL)] = {
    // This isn't very nice, we are mixing UI with IO but the ProgressPanel is required for now
    val progress = ProgressPanel.makeProgressPanel("Accessing catalog server ...")

    def imageLoad(url: URL): Future[Throwable \/ (File, URL)] = Future.apply {
      val connection = progress.openConnection(url)
      val in = progress.getLoggedInputStream(url)
      imageToTmpFile(url, connection.getContentType, in)
    }

    val f = imageLoad(url)
    // Always stop the progress panel
    f.onComplete(_ => progress.stop())
    f
  }
}
