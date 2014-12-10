package edu.gemini.catalog.votable

import java.util.concurrent.atomic.AtomicInteger

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.VoTableParser.CatalogResult
import edu.gemini.spModel.core.Angle
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import org.apache.commons.httpclient.methods.GetMethod

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future, future}
import scala.util.{Failure, Success}
import scalaz.\/

trait VoTableClient {
  private def format(a: Angle)= f"${a.toDegrees}%4.03f"

  protected def queryParams(qs: CatalogQuery): Array[NameValuePair] = Array(
    new NameValuePair("CATALOG", qs.catalog.id),
    new NameValuePair("RA", format(qs.base.ra.toAngle)),
    new NameValuePair("DEC", format(qs.base.dec.toAngle)),
    new NameValuePair("SR", format(qs.coneRadius)))

  // First success or last failure
  protected def selectOne[A](fs: List[Future[A]]): Future[A] = {
    val p = Promise[A]()
    val n = new AtomicInteger(fs.length)
    fs.foreach { f =>
      f.onComplete {
        case Success(a) => p.trySuccess(a)
        case Failure(e) => if (n.decrementAndGet == 0) p.tryFailure(e)
      }
    }
    p.future
  }

  protected def doQuery(query: CatalogQuery, url: String): Future[CatalogResult] = future {
    val method = new GetMethod(s"$url/cgi-bin/conesearch.py")
    val qs = queryParams(query)
    method.setQueryString(qs)

    val client = new HttpClient

    try {
      client.executeMethod(method)
      VoTableParser.parse(url, method.getResponseBodyAsStream)
    }
    finally {
      method.releaseConnection()
    }
  }

}

object VoTableClient extends VoTableClient {
  val catalogUrls = List("http://cpocatalog2.cl.gemini.edu", "http://mkocatalog2.hi.gemini.edu")

  def catalog(query: CatalogQuery): Future[CatalogResult] = {
    val f = for {
      url <- catalogUrls
    } yield doQuery(query, url)
    selectOne(f).recover {
       case t => \/.left(GenericError(t.getMessage))
    }
  }

}
