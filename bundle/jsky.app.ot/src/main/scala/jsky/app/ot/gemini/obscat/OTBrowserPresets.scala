package jsky.app.ot.gemini.obscat

import jsky.app.ot.gemini.obscat.OTBrowserPresetChoice.SavedPreset

import java.io._
import java.util.logging.{Level, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._

/**
 * Support for loading and storing the browser query history.
 */
object OTBrowserPresets {
  val Log = Logger.getLogger(getClass.getName)

  val otBrowserFile = "otBrowserPresets.ser"

  // Must be initialized from the outside, e.g. Activator
  var dir: Option[File] = None

  private def catchingAll(block: => Unit): Unit =
    try {
      block
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Log.log(Level.WARNING, "Problem with browser query save/restore", ex)
    }

  def load(): Unit = catchingAll {
    dir.foreach { d =>
      Log.info("Loading catalog query history")

      val f = new File(d, otBrowserFile)
      if (f.exists()) {
        val in = new ObjectInputStream(new FileInputStream(f))
        val p = (0 until in.readInt()).map { _ =>
          in.readObject().asInstanceOf[SavedPreset]
        }
        ObsCatalogFrame.loadPresets(p.toList)
      }
      Log.info("Finished loading catalog query history")
    }
    dir.ifNone(Log.warning("Must initialize the OTBrowser history"))
  }

  def saveAsync(presets: List[OTBrowserPresetChoice.ObsQueryPreset])(implicit ctx: ExecutionContext): Future[Unit] = Future.apply(save(presets))

  private def save(presets: List[OTBrowserPresetChoice.ObsQueryPreset]): Unit = catchingAll {
    dir.foreach { d =>
      Log.info("Saving catalog query history")
      val f = new File(d, otBrowserFile)
      val out = new ObjectOutputStream(new FileOutputStream(f))
      out.writeInt(presets.length)
      presets.foreach { ql =>
        out.writeObject(ql)
      }
      out.close()

      Log.info("Finished saving catalog query history")
    }
    dir.ifNone(Log.warning("Must initialize the OTBrowser history"))
  }
}
