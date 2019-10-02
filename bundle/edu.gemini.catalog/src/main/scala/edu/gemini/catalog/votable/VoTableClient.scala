package edu.gemini.catalog.votable

import java.net.{URL, UnknownHostException}
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

import edu.gemini.catalog.api.{CatalogName, RadiusConstraint, MagnitudeConstraints, NameCatalogQuery, ConeSearchCatalogQuery, CatalogQuery}
import edu.gemini.spModel.core.{Angle, Magnitude, MagnitudeBand, MagnitudeSystem, NiciBandsList, NoBands, RBandsList, SiderealTarget, SingleBand}
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import org.apache.commons.httpclient.methods.GetMethod

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.math.min

import scalaz._
import Scalaz._

trait VoTableBackend {
  def catalogUrls: NonEmptyList[URL]
  protected [votable] def doQuery(query: CatalogQuery, url: URL)(ec: ExecutionContext): Future[QueryResult]
}

trait CachedBackend extends VoTableBackend {
  val Log = Logger.getLogger(this.getClass.getName)

  case class SearchKey(query: CatalogQuery, url: URL)

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
              val ce = m(pos)
              m = m.patch(pos, Nil, 1)
              // Move it to the front
              m = ce +: m
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
          case Some(CacheEntry(SearchKey(query:ConeSearchCatalogQuery, _), v)) =>
            // Note we need to compare against the widened query
            if (widen(query).isSuperSetOf(k.query)) Some((pos, v)) else go(pos + 1)
          case _                                                               => None // Not caching named queries so far
        }
      go(0)
    }

    QueryCache.buildCache(contains)
  }

  // Make the query wider increasing cache efficiency
  protected def widen(q: CatalogQuery): CatalogQuery = q match {
    case c: ConeSearchCatalogQuery =>
      val widerLimit = min(c.radiusConstraint.maxLimit.toArcmins + 10, c.radiusConstraint.maxLimit.toArcmins * 1.5)
      c.copy(radiusConstraint = RadiusConstraint.between(c.radiusConstraint.minLimit, Angle.fromArcmin(widerLimit)))
    case x => x
  }

  // Cache the query not the future so that failed queries are executed again
  protected val cachedQuery = cache(query)

  // Do a query to the appropriate backend
  protected def query(e: SearchKey): QueryResult

  // Cache the query not the future so that failed queries are executed again
  override protected [votable] def doQuery(query: CatalogQuery, url: URL)(ec: ExecutionContext): Future[QueryResult] = Future {
    Log.fine(s"Starting catalog lookup on ${Thread.currentThread}")
    val qr = cachedQuery(SearchKey(query, url))
    // Filter on the cached query results
    qr.copy(query = query, result = qr.result.filter(query))
  } (ec)

}

/**
 * Common methods to do query calls to remote servers
 */
trait RemoteCallBackend { this: CachedBackend =>
  private val timeout = 30 * 1000 // Max time to wait

  protected [votable] def queryParams(q: CatalogQuery): Array[NameValuePair]
  protected [votable] def queryUrl(e: SearchKey): String

  override protected def query(e: SearchKey): QueryResult = {
    val method = new GetMethod(queryUrl(e))
    val widerQuery = widen(e.query)
    val qs = queryParams(widerQuery)
    method.setQueryString(qs)
    Log.info(s"Catalog query to ${method.getURI}")

    val client = new HttpClient
    client.setConnectionTimeout(timeout)

    try {
      client.executeMethod(method)
      VoTableParser.parse(e.query.catalog, method.getResponseBodyAsStream) match {
        case -\/(p) => QueryResult(widerQuery, CatalogQueryResult(TargetsTable.Zero, List(p)))
        case \/-(y) => QueryResult(widerQuery, CatalogQueryResult(y))
      }
    } finally {
      method.releaseConnection()
    }
  }
}

case object ConeSearchBackend extends CachedBackend with RemoteCallBackend {
  val instance = this
  override val catalogUrls = NonEmptyList(new URL("http://gscatalog.gemini.edu"), new URL("http://gncatalog.gemini.edu"))

  private def format(a: Angle)= f"${a.toDegrees}%4.03f"

  protected [votable] def queryParams(q: CatalogQuery): Array[NameValuePair] = q match {
    case qs: ConeSearchCatalogQuery => Array(
      new NameValuePair("CATALOG", qs.catalog.id),
      new NameValuePair("RA", format(qs.base.ra.toAngle)),
      new NameValuePair("DEC", f"${qs.base.dec.toDegrees}%4.03f"),
      new NameValuePair("SR", format(qs.radiusConstraint.maxLimit)))
    case _                          => Array.empty
  }

  override def queryUrl(e: SearchKey): String = s"${e.url}/cgi-bin/conesearch.py"
}

case object GaiaBackend extends CachedBackend with RemoteCallBackend {
  // For Java
  val instance = this

  val MaxResultCount: Int         = 50000  // Arbitrary max limit -- for a crowded field this needs to be largish
  val BrightLimit: Int            =     0  // Bright limit if none can be computed
  val FaintLimit: Int             =   100  // Faint limit if none can be computed
  val ProperMotionLimitMasYr: Int =   100

  // Override the cache `widen` to widen significantly less for Gaia because it
  // is so deep. Instead of 10', use 0.5' (which is roughly half the AO port size).
  override protected def widen(q: CatalogQuery): CatalogQuery =
    q match {
      case c: ConeSearchCatalogQuery =>
        val widerLimit = min(c.radiusConstraint.maxLimit.toArcmins + 0.5, c.radiusConstraint.maxLimit.toArcmins * 1.5)
        c.copy(radiusConstraint = RadiusConstraint.between(c.radiusConstraint.minLimit, Angle.fromArcmin(widerLimit)))

      case _                         =>
        q
    }

  def adql(cs: ConeSearchCatalogQuery): String = {
    import CatalogAdapter.Gaia

    val fields = Gaia.allFields.map(_.id).mkString(",")

    // The magnitude limits to pass to the server are on g-band but are
    // specified in another band.  We have to unconvert to get back the g-band
    // limits.

    val magc   = cs.magnitudeConstraints.reduceOption { (c0, c1) =>
      c0.union(c1).getOrElse(MagnitudeConstraints.unbounded(c0.searchBands))
    }

    val band   = magc.flatMap {
      _.searchBands match {
        case RBandsList    => Some(MagnitudeBand.R)
        case NiciBandsList => Some(MagnitudeBand.R)
        case SingleBand(b) => Some(b)
        case NoBands       => None
      }
    }

    def mag(v: Double, b: MagnitudeBand): Magnitude =
      Magnitude(v, b, None, MagnitudeSystem.Vega)

    val faint  =
      for {
        b <- band
        v <- magc.map(_.faintnessConstraint.brightness)
        g <- CatalogAdapter.Gaia.unconvertMin(mag(v, b))
      } yield g.ceil.round.toInt

    val bright =
      for {
        b <- band
        v <- magc.flatMap(_.saturationConstraint.map(_.brightness))
        g <- CatalogAdapter.Gaia.unconvertMax(mag(v, b))
      } yield g.floor.round.toInt

    f"""|SELECT TOP $MaxResultCount $fields
        |      FROM gaiadr2.gaia_source
        |     WHERE CONTAINS(POINT('ICRS',${Gaia.raField.id},${Gaia.decField.id}),CIRCLE('ICRS', ${cs.base.ra.toDegrees}%9.8f, ${cs.base.dec.toDegrees}%9.8f, ${cs.radiusConstraint.maxLimit.toDegrees}%9.8f))=1
        |       AND (${Gaia.plxField.id} > 0)
        |       AND (${Gaia.gMagField.id} BETWEEN ${bright.getOrElse(BrightLimit)} AND ${faint.getOrElse(FaintLimit)})
        |       AND (${Gaia.bpRpField.id} IS NOT NULL)
        |       AND (SQRT(POWER(${Gaia.pmRaField.id}, 2.0) + POWER(${Gaia.pmDecField.id}, 2.0)) < ${ProperMotionLimitMasYr})
        |  ORDER BY ${Gaia.gMagField.id}
      """.stripMargin
  }

  override protected [votable] def queryParams(q: CatalogQuery): Array[NameValuePair] =
    q match {

      case cs: ConeSearchCatalogQuery =>
        Array(
          new NameValuePair("REQUEST", "doQuery"      ),
          new NameValuePair("LANG",    "ADQL"         ),
          new NameValuePair("FORMAT",  "votable_plain"),
          new NameValuePair("QUERY",   adql(cs)       )
        )

      case _                          =>
        Array.empty
    }


  override val catalogUrls: NonEmptyList[URL] =
    NonEmptyList(new URL("http://gea.esac.esa.int/tap-server/tap/sync"))

  override def queryUrl(e: SearchKey): String =
    e.url.toExternalForm
}

case object SimbadNameBackend extends CachedBackend with RemoteCallBackend {
  override val catalogUrls = NonEmptyList(new URL("http://simbad.cfa.harvard.edu/simbad"), new URL("http://simbad.u-strasbg.fr/simbad"))

  protected [votable] def queryParams(q: CatalogQuery): Array[NameValuePair] = q match {
    case qs: NameCatalogQuery => Array(
      new NameValuePair("output.format", "VOTable"),
      new NameValuePair("Ident", qs.search))
    case _                    => Array.empty
  }

  override def queryUrl(e: SearchKey): String = s"${e.url}/sim-id"
}

case class CannedBackend(results: List[SiderealTarget]) extends VoTableBackend {
  // Needs some fake list of urls to hit
  override val catalogUrls = NonEmptyList(new URL("file:////"))
  override protected[votable] def doQuery(query: CatalogQuery, url: URL)(ec: ExecutionContext): Future[QueryResult] =
    Future.successful {
      QueryResult(query, CatalogQueryResult(TargetsTable(results), Nil))
    }
}

trait VoTableClient {
  // First success or last failure
  protected def selectOne[A](fs: NonEmptyList[Future[A]])(ec: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    val n = new AtomicInteger(fs.size)
    fs.foreach { f =>
      f.onComplete {
        case Success(a) => p.trySuccess(a)
        case Failure(e) => if (n.decrementAndGet == 0) p.tryFailure(e)
      }
    }
    p.future
  }

  protected def doQuery(query: CatalogQuery, url: URL, backend: VoTableBackend)(ec: ExecutionContext): Future[QueryResult] =
    backend.doQuery(query, url)(ec)

}

object VoTableClient extends VoTableClient {

  def defaultBackend(n: CatalogName): VoTableBackend =
    n match {
      case CatalogName.Gaia   => GaiaBackend
      case CatalogName.SIMBAD => SimbadNameBackend
      case _                  => ConeSearchBackend
    }

  /**
   * Do a query for targets, it returns a list of targets and possible problems found
   */
  def catalog(query: CatalogQuery, explicitBackend: Option[VoTableBackend])(ec: ExecutionContext): Future[QueryResult] = {
    val backend = explicitBackend.getOrElse(defaultBackend(query.catalog))

    val f = for {
      url <- backend.catalogUrls
    } yield doQuery(query, url, backend)(ec)
    selectOne(f)(ec).recover {
       case t:UnknownHostException => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(s"Unreachable host ${t.getMessage}"))))
       case t                      => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(GenericError(t.getMessage))))
    }
  }

  /**
   * Do multiple parallel queries, it returns a consolidated list of targets and possible problems found
   */
  def catalogs(queries: List[CatalogQuery], explicitBackend: Option[VoTableBackend])(ec: ExecutionContext): Future[List[QueryResult]] = {
    val r = queries.strengthR(explicitBackend).map { case (a, b) => catalog(a, b)(ec) }
    Future.sequence(r)
  }

}
