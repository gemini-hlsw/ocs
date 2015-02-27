package edu.gemini.catalog.votable

import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.core.Angle
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import org.apache.commons.httpclient.methods.GetMethod

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future, future}
import scala.util.{Failure, Success}

import scalaz._
import Scalaz._

trait VoTableClient {
  val Log = Logger.getLogger(getClass.getName)

  private val timeout = 30 * 1000 // Max time to wait

  private def format(a: Angle)= f"${a.toDegrees}%4.03f"

  protected def queryParams(qs: CatalogQuery): Array[NameValuePair] = Array(
    new NameValuePair("CATALOG", qs.catalog.id),
    new NameValuePair("RA", format(qs.base.ra.toAngle)),
    new NameValuePair("DEC", f"${qs.base.dec.toDegrees}%4.03f"),
    new NameValuePair("SR", format(qs.radiusConstraint.maxLimit)))

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

  // weak hash map should clean itself if we are using too much memory
  val memo:Memo[(CatalogQuery, String), QueryResult] = Memo.weakHashMapMemo

  // Cache the query not the future so that failed queries are executed again
  val memoizedQuery = memo {
    case (query, url) =>
      val method = new GetMethod(s"$url/cgi-bin/conesearch.py")
      val qs = queryParams(query)
      method.setQueryString(qs)
      Log.info(s"Catalog query to ${method.getURI}")

      val client = new HttpClient
      client.setConnectionTimeout(timeout)

      try {
        client.executeMethod(method)
        VoTableParser.parse(url, method.getResponseBodyAsStream).fold(p => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(p))), y => QueryResult(query, CatalogQueryResult(y).filter(query)))
      }
      finally {
        method.releaseConnection()
      }
  }

  protected def doQuery(query: CatalogQuery, url: String): Future[QueryResult] = future {
    memoizedQuery((query, url))
  }

}

object VoTableClient extends VoTableClient {
  val catalogUrls = List("http://cpocatalog2.cl.gemini.edu", "http://mkocatalog2.hi.gemini.edu")

  /**
   * Do a query for targets, it returns a list of targets and possible problems found
   */
  def catalog(query: CatalogQuery): Future[QueryResult] = {
    val f = for {
      url <- catalogUrls
    } yield doQuery(query, url)
    selectOne(f).recover {
       case t:UnknownHostException => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(s"Unreachable host ${t.getMessage}"))))
       case t                      => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(t.getMessage))))
    }
  }

  /**
   * Do multiple parallel queries, it returns a consolidated list of targets and possible problems found
   */
  def catalog(queries: List[CatalogQuery]): Future[List[QueryResult]] = {
    val r = queries.map(catalog)
    Future.sequence(r)
  }

}
