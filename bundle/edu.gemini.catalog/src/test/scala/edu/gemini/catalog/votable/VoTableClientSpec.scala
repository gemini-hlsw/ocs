package edu.gemini.catalog.votable

import java.net.{UnknownHostException, URL}
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import edu.gemini.catalog.api._
import edu.gemini.catalog.api.CatalogName._
import edu.gemini.spModel.core._
import org.specs2.mutable.Specification

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz.NonEmptyList

class VoTableClientSpec extends Specification with VoTableClient {
  val noMagnitudeConstraint = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(100), None)
  "The VoTable client" should {

    val ra = RightAscension.fromAngle(Angle.fromDegrees(10))
    val dec = Declination.fromAngle(Angle.fromDegrees(20)).getOrElse(Declination.zero)
    val coordinates = Coordinates(ra, dec)

    val query = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), noMagnitudeConstraint, CatalogName.UCAC4)
    case class CountingCachedBackend(counter: AtomicInteger, file: String) extends CachedBackend {
      override val catalogUrls = NonEmptyList(new URL(s"file://$file"))
      override protected def query(e: SearchKey) = {
        counter.incrementAndGet()
        VoTableParser.parse(CatalogName.UCAC4, this.getClass.getResourceAsStream(file)).fold(p => QueryResult(e.query, CatalogQueryResult(TargetsTable.Zero, List(p))), y => QueryResult(e.query, CatalogQueryResult(y)))
      }
    }

    "produce query params" in {
      ConeSearchBackend.queryParams(query) should beEqualTo(Array(("CATALOG", "ucac4"), ("RA", "10.000"), ("DEC", "20.000"), ("SR", "0.100"), ("VER", "1.3")))
    }
    "make a query to a bad site" in {
      Await.result(doQuery(query, new URL("http://unknown.site.7732BFA1-9937-4D1E-8969-B02782608DAD"), ConeSearchBackend)(implicitly), 30.seconds) should throwA[UnknownHostException]
    }
    "be able to select the first successful of several futures" in {
      def f1 = Future { Thread.sleep(1000); throw new RuntimeException("oops") }
      def f2 = Future { Thread.sleep(2000); 42 } // this one should complete first
      def f3 = Future { Thread.sleep(3000); 99 }

      Await.result(selectOne(NonEmptyList(f1, f2, f3))(implicitly), 3.seconds) should beEqualTo(42)
    }
    "make a query" in {
      // This test loads a file. There is not much to test but it exercises the query backend chain
      Await.result(VoTableClient.catalog(query, Some(TestVoTableBackend("/votable-ucac4.xml")))(implicitly), 5.seconds).result.containsError should beFalse
    }
    "use the cache to skip queries" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          f2 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
        } yield (f1, f2)
      // Check both have the same results
      val result = Await.result(r, 10.seconds)
      result._1 should beEqualTo(result._2)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(countingBackend.catalogUrls.size)
    }
    "use the cache to skip queries that occupy a subset" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has smaller radius
      val query2 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), noMagnitudeConstraint, CatalogName.UCAC4)
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          f2 <- VoTableClient.catalog(query2, Some(countingBackend))(implicitly)
        } yield (f1, f2)
      Await.result(r, 10.seconds)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(countingBackend.catalogUrls.size)
    }
    "cache should widen the search to improve efficiency" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has a bit bigger radius but the widening effect should avoid doing another query
      val query = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromArcmin(10)), noMagnitudeConstraint, CatalogName.UCAC4)
      val query2 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromArcmin(12)), noMagnitudeConstraint, CatalogName.UCAC4)
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          _  <- Future.successful(TimeUnit.SECONDS.sleep(1)) // Give it time to hit the first query
          f2 <- VoTableClient.catalog(query2, Some(countingBackend))(implicitly)
        } yield (f1, f2)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(countingBackend.catalogUrls.size)
    }
    "cache should widen the search to improve efficiency, part 2" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has a bit bigger radius but the widening effect should avoid doing another query
      val query2 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.11)), noMagnitudeConstraint, CatalogName.UCAC4)
      // query3 is wider it should hit the catalog again
      val query3 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.2)), noMagnitudeConstraint, CatalogName.UCAC4)
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          _  <- Future.successful(TimeUnit.SECONDS.sleep(1)) // Give it time to hit the first query
          f2 <- VoTableClient.catalog(query2, Some(countingBackend))(implicitly)
          f3 <- VoTableClient.catalog(query3, Some(countingBackend))(implicitly)
        } yield (f1, f2, f3)
      // Depending on timing it could hit all or less than all parallel urls
      counter.get() should be_<=(2 * countingBackend.catalogUrls.size)
    }
    "cache hits should preserve the queries" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has smaller radius
      val query2 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), noMagnitudeConstraint, CatalogName.UCAC4)
      // Backend should be hit at most once per url
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          f2 <- VoTableClient.catalog(query2, Some(countingBackend))(implicitly)
        } yield (f1, f2)
      val result = Await.result(r, 10.seconds)
      // Check that each query is matched
      result._1.query should beEqualTo(query)
      result._2.query should beEqualTo(query2)
    }
    "cache hits should be filtered per query result" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      // query2 has smaller radius
      val query2 = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.05)), noMagnitudeConstraint, CatalogName.UCAC4)
      //
      val r = for {
          f1 <- VoTableClient.catalog(query, Some(countingBackend))(implicitly)
          _  <- Future.successful(TimeUnit.SECONDS.sleep(1)) // Give it time to hit the first query
          f2 <- VoTableClient.catalog(query2, Some(countingBackend))(implicitly)
        } yield (f1, f2)
      val result = Await.result(r, 10.seconds)
      // Check that the second query has less hits than the first given its smaller range
      result._2.result.targets.rows.length should beLessThan(result._1.result.targets.rows.length)
    }
    "include query params" in {
      val counter = new AtomicInteger(0)
      val countingBackend = CountingCachedBackend(counter, "/votable-ucac4.xml")
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val query = CatalogQuery.coneSearch(coordinates, RadiusConstraint.between(Angle.fromDegrees(0), Angle.fromDegrees(0.1)), mc, CatalogName.UCAC4)

      val result = Await.result(VoTableClient.catalog(query, Some(countingBackend))(implicitly), 10.seconds)
      // Extract the query params from the results
      result.query should beEqualTo(query)
    }

  }
}
