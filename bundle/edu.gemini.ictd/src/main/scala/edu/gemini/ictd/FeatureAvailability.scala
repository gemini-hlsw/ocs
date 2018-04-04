package edu.gemini.ictd

import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.Instrument._
import edu.gemini.spModel.ictd.{ Availability, IctdType }
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.{ Filter => F2Filter, FPUnit => F2FPUnit }
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{ DisperserNorth, FilterNorth, FPUnitNorth }
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{ DisperserSouth, FilterSouth, FPUnitSouth }

import FeatureAvailability._

import scala.collection.breakOut
import scala.collection.immutable.TreeMap

import scala.reflect._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/** The collection of instruments tracked by the ICTD database and the
  * availability of their various features.  This class contains availability
  * information for standard instrument facilities like filters and dispersers,
  * but not custom masks.
  */
final case class FeatureAvailability(
  flamingos2: Flamingos2Config,
  gmosNorth:  GmosNorthConfig,
  gmosSouth:  GmosSouthConfig
) {

  val all: List[InstrumentAvailability] =
    List(flamingos2, gmosNorth, gmosSouth)

  /** A map of availability information keyed by OCS heterogeneous enum values
    * corresponding to filters, etc. across all instruments.
    */
  def availabilityMap(s: Site): AvailabilityMap =
    all.filter(_.instrument.existsAt(s)).foldLeft(EmptyAvailabilityMap) {
      _ ++ _.availabilityMap
    }

  def javaAvailabilityMap(s: Site): java.util.Map[java.lang.Enum[_], Availability] =
    availabilityMap(s).asJava
}

object FeatureAvailability {

  type AvailabilityMap = Map[java.lang.Enum[_], Availability]

  val EmptyAvailabilityMap: AvailabilityMap =
    Map.empty

  sealed trait InstrumentAvailability {
    def instrument: Instrument

    def availabilityMap: AvailabilityMap
  }

  /** Creates a FeatureAvailability instance given the collection of every
    * instrument's FeatureTables from the ICTD database.
    */
  def fromTables(tabs: Map[Instrument, FeatureTables]): FeatureAvailability = {
    val forInst = tabs.withDefaultValue(FeatureTables.empty)

    FeatureAvailability(
      Flamingos2Config.fromTables(forInst(Flamingos2)),
      GmosNorthConfig.fromTables(forInst(GmosNorth)),
      GmosSouthConfig.fromTables(forInst(GmosSouth))
    )
  }

  final case class Flamingos2Config(
    filters: Map[F2Filter, Availability],
    fpus:    Map[F2FPUnit, Availability]
  ) extends InstrumentAvailability {

    def instrument: Instrument =
      Instrument.Flamingos2

    def availabilityMap: AvailabilityMap =
      filters ++ fpus

  }

  object Flamingos2Config {
    def fromTables(tabs: FeatureTables): Flamingos2Config =
      Flamingos2Config(
        resolve[F2Filter](tabs.filter),
        resolve[F2FPUnit](tabs.longslit ++ tabs.circular)
      )
  }


  final case class GmosNorthConfig(
    dispersers: Map[DisperserNorth, Availability],
    filters:    Map[FilterNorth,    Availability],
    fpus:       Map[FPUnitNorth,    Availability]
  ) extends InstrumentAvailability {

    def instrument: Instrument =
      Instrument.GmosNorth

    def availabilityMap: AvailabilityMap =
      dispersers ++ filters ++ fpus

  }

  object GmosNorthConfig {
    def fromTables(tabs: FeatureTables): GmosNorthConfig =
      GmosNorthConfig(
        resolve[DisperserNorth](tabs.grating),
        resolve[FilterNorth   ](tabs.filter),
        resolve[FPUnitNorth   ](tabs.ifu ++ tabs.longslit ++ tabs.nsLongslit)
      )
  }

  final case class GmosSouthConfig(
    dispersers: Map[DisperserSouth, Availability],
    filters:    Map[FilterSouth,    Availability],
    fpus:       Map[FPUnitSouth,    Availability]
  ) extends InstrumentAvailability {

    def instrument: Instrument =
      Instrument.GmosSouth

    def availabilityMap: AvailabilityMap =
      dispersers ++ filters ++ fpus

  }

  object GmosSouthConfig {
    def fromTables(tabs: FeatureTables): GmosSouthConfig =
      GmosSouthConfig(
        resolve[DisperserSouth](tabs.grating),
        resolve[FilterSouth   ](tabs.filter),
        resolve[FPUnitSouth   ](tabs.ifu ++ tabs.longslit ++ tabs.nsLongslit)
      )
  }

  // Given a map of String key to Availability drawn from the ICTD, and an Enum
  // type A, find the availability of each instance of A.
  private def resolve[A <: java.lang.Enum[A] with IctdType : ClassTag](
    tab: AvailabilityTable
  ): Map[A, Availability] = {

    val c  = classTag[A].runtimeClass.asInstanceOf[Class[A]]
    val as = c.getEnumConstants.toList

    as.zip(as.map(_.ictdTracking.resolve(tab.lift)))(breakOut)
  }

}
