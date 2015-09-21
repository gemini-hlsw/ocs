package edu.gemini.catalog.votable

import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

import edu.gemini.catalog.api.{ConeSearchCatalogQuery, CatalogQuery}
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.Target.SiderealTarget
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import org.apache.commons.httpclient.methods.GetMethod

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future, future}
import scala.util.{Failure, Success}

import scalaz._
import Scalaz._

trait VoTableBackend {
  protected [votable] def doQuery(query: CatalogQuery, url: String): Future[QueryResult]
}

trait CachedBackend extends VoTableBackend {
  case class SearchKey(query: CatalogQuery, url: String)

  case class CacheEntry[K, V](k: K, v: V)

  /**
   * A function memoization strategy closely based on scalaz's Memo
   */
  sealed abstract class Cache[K, V] {
    def apply(z: K => V): K => V
  }

  /**
   * Cache for CatalogQueries. Key-based caching as in Memo doesn't fulfill the needs in this case as we can reuse queries
   * done for different combinations of targets and radius even if the query 'key' doesn't match
   * Instead we need a function that can traverse the cache finding matches to reuse the result
   */
  object QueryCache {

    // Container used internally on the cache, the type definition leaks into the client but that helps performance
    // Vector was selected as it gives effective constant time for prepend, patch and dropRight
    type CacheContainer[K, V] = Vector[CacheEntry[K, V]]
    // Function provided by the caller to check if a value is on the cache
    type FindFunction[K, V] = (CacheContainer[K, V], K) => Option[(Int, V)]

    // based on scalaz's Memo.memo function
    private def cache[K, V](f: (K => V) => K => V): Cache[K, V] = new Cache[K, V] {
      def apply(z: K => V) = f(z)
    }

    // Build an LRU cache with a function that will traverse the cache finding suitable queries
    private def lruCache[K, V](a: CacheContainer[K, V], findByKey: FindFunction[K, V], maxSize: Int = 100): Cache[K, V] = {
      // Access needs to be synchronized
      var m = a

      cache[K, V](f => k => {
        val value = a.synchronized {
          val r = findByKey(m, k)
          // Put it at the head if found
          r.foreach {
            case (pos, x) =>
              // Remove it from its current position
              m = m.patch(pos, Nil, 1)
              // Move it to the front
              m = CacheEntry(k, x) +: m
          }
          r.map(_._2)
        }
        value.getOrElse {
          val r = f(k)
          // Using smaller locks increases the chances to make multiple calls to f(k) but
          // calling f(k) inside the lock notably reduces concurrency doing remote queries
          a.synchronized {
            // prepend the result at the beginning
            m = CacheEntry(k, r) +: m
            // Keep size constrained
            if (m.size > maxSize) {
              m = m.dropRight(1)
            }
          }
          r
        }
      })
    }

    /**
     * Builds a cache with a contains function to find cache hits
     */
    def buildCache[K, V](contains: FindFunction[K, V], maxSize: Int = 100) = lruCache(Vector.empty, contains, maxSize)

  }

  private val cache:Cache[SearchKey, QueryResult] = {
    // Find if a search is already in the cache
    // Note that this assumes all catalogues give the same result for a given query
    def contains(a: QueryCache.CacheContainer[SearchKey, QueryResult], k: SearchKey):Option[(Int, QueryResult)] = {
      @tailrec
      def go(pos: Int):Option[(Int, QueryResult)] =
        a.lift(pos) match {
          case None                                                            => None
          case Some(CacheEntry(SearchKey(query:ConeSearchCatalogQuery, r), v)) => if (query.isSuperSetOf(k.query)) Some((pos, v)) else go(pos + 1)
          case Some(_)                                                         => None // Not caching named queries so far
        }
      go(0)
    }

    QueryCache.buildCache(contains)
  }

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


  // Cache the query not the future so that failed queries are executed again
  protected val cachedQuery = cache(query)

  // Do a query to the appropriate backend
  protected def query(e: SearchKey): QueryResult

  // Cache the query not the future so that failed queries are executed again
  protected [votable] def doQuery(query: CatalogQuery, url: String): Future[QueryResult] = future {
    val qr = cachedQuery(SearchKey(query, url))
    // Filter on the cached query results
    qr.copy(query = query, result = qr.result.filter(query))
  }

}

case object RemoteBackend extends CachedBackend {
  val instance = this

  val Log = Logger.getLogger(getClass.getName)

  private val timeout = 30 * 1000 // Max time to wait

  private def format(a: Angle)= f"${a.toDegrees}%4.03f"

  protected [votable] def queryParams(q: CatalogQuery): Array[NameValuePair] = q match {
    case qs: ConeSearchCatalogQuery => Array(
      new NameValuePair("CATALOG", qs.catalog.id),
      new NameValuePair("RA", format(qs.base.ra.toAngle)),
      new NameValuePair("DEC", f"${qs.base.dec.toDegrees}%4.03f"),
      new NameValuePair("SR", format(qs.radiusConstraint.maxLimit)))
    case _ => Array.empty
  }

  override protected def query(e: SearchKey): QueryResult = {
    val method = new GetMethod(s"${e.url}/cgi-bin/conesearch.py")
    val qs = queryParams(e.query)
    method.setQueryString(qs)
    Log.info(s"Catalog query to ${method.getURI}")

    val client = new HttpClient
    client.setConnectionTimeout(timeout)

    try {
      client.executeMethod(method)
      VoTableParser.parse(e.url, method.getResponseBodyAsStream).fold(p => QueryResult(e.query, CatalogQueryResult(TargetsTable.Zero, List(p))), y => QueryResult(e.query, CatalogQueryResult(y)))
    } finally {
      method.releaseConnection()
    }
  }

}

case class CannedBackend(results: List[SiderealTarget]) extends VoTableBackend {
  override protected[votable] def doQuery(query: CatalogQuery, url: String): Future[QueryResult] =
    Future.successful {
      QueryResult(query, CatalogQueryResult(TargetsTable(results), Nil))
    }
}

trait VoTableClient {
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

  protected def doQuery(query: CatalogQuery, url: String, backend: VoTableBackend): Future[QueryResult] =
    backend.doQuery(query, url)

}

object VoTableClient extends VoTableClient {
  val catalogUrls = List("http://gscatalog.gemini.edu", "http://gncatalog.gemini.edu")

  /**
   * Do a query for targets, it returns a list of targets and possible problems found
   */
  def catalog(query: CatalogQuery, backend: VoTableBackend = RemoteBackend): Future[QueryResult] = {
    val f = for {
      url <- catalogUrls
    } yield doQuery(query, url, backend)
    selectOne(f).recover {
       case t:UnknownHostException => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(s"Unreachable host ${t.getMessage}"))))
       case t                      => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(t.getMessage))))
    }
  }

  /**
   * Do multiple parallel queries, it returns a consolidated list of targets and possible problems found
   */
  def catalogs(queries: List[CatalogQuery], backend: VoTableBackend = RemoteBackend): Future[List[QueryResult]] = {
    val r = queries.strengthR(backend).map(Function.tupled(catalog))
    Future.sequence(r)
  }

}
