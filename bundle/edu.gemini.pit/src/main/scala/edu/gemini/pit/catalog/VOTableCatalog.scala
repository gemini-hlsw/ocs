package edu.gemini.pit.catalog

import edu.gemini.model.p1.immutable.Target

import java.io.IOException
import java.net.URL
import java.util.logging.Level
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import votable._

// Type of catalogs that fetch a VOTable
trait VOTableCatalog extends Catalog {

  def url(id: String): URL
  def host: String
  def decode(vot: VOTable): Seq[Target]

  override def find(id: String)(implicit ex: ExecutionContext): Future[Result] =
    Future {
      val conn = url(id).openConnection
      conn.setReadTimeout(1000 * 10) // 10 secs?

      // Parse the URL data into a VOTable
      val vot = VOTable(conn.getInputStream)
      decode(vot)

    }.map {
      case ts if ts.nonEmpty => Success(ts.toList, Nil)
      case _                 => NotFound(id)

    }.recover {
      case e: IOException =>
        Log.warning("%s: %s(%s)".format(host, e.getClass.getSimpleName, e.getMessage))
        Offline
      case t              =>
        Log.log(Level.WARNING, "Unexpected trouble looking up %s on %s.".format(id, host), t)
        Error(t)
    }

}