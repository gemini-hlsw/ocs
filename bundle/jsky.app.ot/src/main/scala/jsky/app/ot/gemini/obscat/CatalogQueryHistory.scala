package jsky.app.ot.gemini.obscat

import jsky.catalog.gui.{CatalogQueryList, CatalogQueryItem}
import jsky.navigator.NavigatorManager

import java.io._
import java.util.logging.{Level, Logger}

/**
 * Support for loading and storing the browser query history.
 */
object CatalogQueryHistory {
  val Log = Logger.getLogger(getClass.getName)

  val Filename = "catalogQueryHistory.ser"

  private def catchingAll(block: => Unit): Unit =
    try {
      block
    } catch {
      case ex: Exception =>
        Log.log(Level.WARNING, "Problem with browser query save/restore", ex)
    }

  def load(dir: File): Unit = catchingAll {
    Log.info("Loading catalog query history")

    val f = new File(dir, Filename)
    if (f.exists()) {
      val ql = NavigatorManager.create().getQueryList

      val in = new ObjectInputStream(new FileInputStream(f))
      (0 until in.readInt()).foreach { _ =>
        val name   = in.readObject().asInstanceOf[String]
        val query  = in.readObject()
        val result = in.readObject()
        ql.add(new CatalogQueryItem(name, query, result))
      }
    }
    Log.info("Finished loading catalog query history")
  }

  def save(dir: File): Unit = catchingAll {
    Log.info("Saving catalog query history")

    def queryList: Option[CatalogQueryList] =
      Option(NavigatorManager.get()).flatMap(n => Option(n.getQueryList))

    queryList.foreach { ql =>
      val f   = new File(dir, Filename)
      val out = new ObjectOutputStream(new FileOutputStream(f))
      out.writeInt(ql.size())
      val it = ql.iterator()
      while (it.hasNext) {
        val cqi = it.next()
        out.writeObject(cqi.getName)
        out.writeObject(cqi.getQueryInfo)
        out.writeObject(cqi.getResultInfo)
      }
      out.close()
    }

    Log.info("Finished saving catalog query history")
  }
}
