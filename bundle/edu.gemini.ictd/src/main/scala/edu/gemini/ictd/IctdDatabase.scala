package edu.gemini.ictd

import edu.gemini.spModel.core.Site
import edu.gemini.spModel.ictd.{ Availability, CustomMaskKey }

import doobie.imports._

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap

import scalaz.effect.IO


/** Entry point for clients. Two categories of availability information are
  * obtainable.  One is the availability of custom masks and the other is the
  * availability of standard instrument features like filters, gratings, etc.
  */
object IctdDatabase {

  val Driver: String =
    "com.mysql.jdbc.Driver"

  /** Configuration for a database connection. */
  final case class Configuration(
    connectUrl: String,
    userName:   String,
    password:   String
  ) {

    /** A transactor in IO using the connection values provided by this configuration. */
    val transactor: Transactor[IO] =
      DriverManagerTransactor[IO](Driver, connectUrl, userName, password)

  }

  object Configuration {

    /** A configuration for local testing. */
    def forTesting: Configuration =
      Configuration("jdbc:mysql://localhost/ictd?useSSL=false", "ocs", "password")

  }

  object mask {

    def select(c: Configuration, s: Site): IO[TreeMap[CustomMaskKey, Availability]] =
      dao.CustomMaskDao.select(s).transact(c.transactor)

    def unsafeSelect(c: Configuration, s: Site): TreeMap[CustomMaskKey, Availability] =
      select(c, s).unsafePerformIO

  }


  object feature {

    def select(c: Configuration): IO[FeatureAvailability] =
      dao.FeatureTablesDao.select.transact(c.transactor).map(FeatureAvailability.fromTables)

    def unsafeSelect(c: Configuration): FeatureAvailability =
      select(c).unsafePerformIO

  }

  def asJava: asJavaStub.type =
    asJavaStub

  object asJavaStub {

    val testConfiguration: Configuration =
      Configuration.forTesting

    def unsafeSelectMaskAvailability(
      c: Configuration,
      s: Site
    ): java.util.Map[CustomMaskKey, Availability] =
      mask.unsafeSelect(c, s).asJava

    def unsafeSelectFeatureAvailability(
      c: Configuration,
      s: Site
    ): java.util.Map[Enum[_], Availability] =
      feature.unsafeSelect(c).availabilityMap(s).asJava

  }
}
