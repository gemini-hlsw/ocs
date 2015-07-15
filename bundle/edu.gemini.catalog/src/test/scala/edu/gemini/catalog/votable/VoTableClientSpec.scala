package edu.gemini.catalog.votable

import java.util.concurrent.atomic.AtomicInteger

import edu.gemini.catalog.api._
import edu.gemini.spModel.core.{MagnitudeBand, Angle, Coordinates}
import org.apache.commons.httpclient.NameValuePair
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class VoTableClientSpec extends SpecificationWithJUnit with VoTableClient with NoTimeConversions {
  val noMagnitudeConstraint = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(100), None)
  "The VoTable client" should {

    val query = CatalogQuery(Coordinates.zero, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.2)), noMagnitudeConstraint, ucac4)
    case class CountingCachedBackend(counter: AtomicInteger, file: String) extends CachedBackend {
      override protected def query(e: SearchKey) = {
        counter.incrementAndGet()
        VoTableParser.parse(e.url, this.getClass.getResourceAsStream(file)).fold(p => QueryResult(e.query, CatalogQueryResult(TargetsTable.Zero, List(p))), y => QueryResult(e.query, CatalogQueryResult(y).filter(e.query)))
      }

    }

    "produce query params" in {
      RemoteBackend.queryParams(query) should beEqualTo(Array(new NameValuePair("CATALOG", "ucac4"), new NameValuePair("RA", "0.000"), new NameValuePair("DEC", "0.000"), new NameValuePair("SR", "0.200")))
    }
    "make a query to a bad site" in {
      Await.result(doQuery(query, "unknown site", RemoteBackend), 1.seconds) should throwA[IllegalArgumentException]
    }
    "be able to select the first successful of several futures" in {
      def f1 = Future { Thread.sleep(1000); throw new RuntimeException("oops") }
      def f2 = Future { Thread.sleep(2000); 42 } // this one should complete first
      def f3 = Future { Thread.sleep(3000); 99 }

      Await.result(selectOne(List(f1, f2, f3)), 3.seconds) should beEqualTo(42)
    }
    "make a query" in {
      // This test loads a file. There is not much to test but it exercises the query backend chain
      Await.result(VoTableClient.catalog(query, TestVoTableBackend("/votable-ucac4.xml")), 5.seconds).result.containsError should beFalse
    }
    "use the cache to skip queries" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, countingBackend)
          f2 <- VoTableClient.catalog(query, countingBackend)
        } yield f2
      Await.result(r, 10.seconds)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(VoTableClient.catalogUrls.size)
    }
    "use the cache to skip queries that occupy a subset" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has smaller radius
      val query2 = CatalogQuery(Coordinates.zero, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), noMagnitudeConstraint, ucac4)
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, countingBackend)
          f2 <- VoTableClient.catalog(query2, countingBackend)
        } yield f2
      Await.result(r, 10.seconds)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(VoTableClient.catalogUrls.size)
    }
    "include query params" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val query = CatalogQuery(Coordinates.zero, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), mc, ucac4)

      val result = Await.result(VoTableClient.catalog(query, countingBackend), 10.seconds)
      // Extract the query params from the results
      result.query.base should beEqualTo(Coordinates.zero)
      result.query.radiusConstraint should beEqualTo(RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)))
      result.query.magnitudeConstraints.head should beEqualTo(mc)
    }

  }
}
