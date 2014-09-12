package edu.gemini.pit.catalog

import edu.gemini.model.p1.immutable.Target

import java.io.IOException
import java.net.URL
import java.util.logging.Level
import scala.actors.Actor._
import votable._

// Type of catalogs that fetch a VOTable
trait VOTableCatalog extends Catalog {

  def url(id: String): URL
  def host: String
  def decode(vot: VOTable): Seq[Target]

  def find(id: String)(callback: Result => Unit) {
    actor {
      val f = callback.safe
      try {

        val conn = url(id).openConnection
        conn.setReadTimeout(1000 * 10) // 10 secs?

        // Parse the URL data into a VOTable
        val vot = VOTable(conn.getInputStream)
        val ts = decode(vot)
        if (!ts.isEmpty) f(Success(ts.toList, Nil)) else f(NotFound(id))

      } catch {
        case e:IOException =>
          Log.warning("%s: %s(%s)".format(host, e.getClass.getSimpleName, e.getMessage))
          f(Offline)
        case t:Throwable =>
          Log.log(Level.WARNING, "Unexpected trouble looking up %s on %s.".format(id, host), t)
          f(Error(t))
      }
    }
  }

}