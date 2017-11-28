package edu.gemini.qv.plugin
package filter.core

import java.time.Instant

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.{Band, Obs, Prog}
import edu.gemini.qv.plugin.QvStore.NamedElement
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.spModel.core.Semester.Half
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.gnirs.{GNIRSParams, InstGNIRS}
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.inst.InstRegistry
import edu.gemini.spModel.gemini.nici.{InstNICI, NICIParams}
import edu.gemini.spModel.gemini.nifs.{InstNIFS, NIFSParams}
import edu.gemini.spModel.gemini.niri.{InstNIRI, Niri}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.gemini.texes.{InstTexes, TexesParams}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.SPObservation.Priority
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.too.TooType
import edu.gemini.spModel.`type`.DisplayableSpType
import java.util.TimeZone

import edu.gemini.shared.util.DateTimeUtils

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer


trait Filter extends Ordered[Filter] {
  def label: String
  def name: String
  def desc: String = ""
  def categoryName: String = name
  def predicate(o: Obs, ctx: QvContext): Boolean
  def and(f: Filter): Filter = FilterAnd(this, f)
  def or(f: Filter): Filter = FilterOr(this, f)
  def elements: Set[Filter] = Set(this)
  def compare(that: Filter): Int =  this.name.compare(that.name)
  def isEmpty = false
  def isActive = true
}

trait OptionsFilter[A] extends Filter {
  def values: Set[A]
  def sortedValues: Seq[A]
  def selection: Set[A]
  def updated(selection: Set[A]): OptionsFilter[A]
  def collector(o: Obs, ctx: QvContext): Set[A]
  def valueName: A => String
  override def isEmpty = selection.isEmpty
  override def isActive = !selection.equals(values)
}

case class EnumFilter[A](label: String, values: Set[A], getter: (Obs, QvContext) => A, selection: Set[A], valueName: A => String) extends OptionsFilter[A] {
  val sortedValues: Seq[A] = values.toList.sortBy(valueName)
  def name = selection.map(valueName).mkString(",")
  def collector(o: Obs, ctx: QvContext): Set[A] = Set(getter(o, ctx))
  def predicate(o: Obs, ctx: QvContext): Boolean = selection.contains(getter(o, ctx))
  def updated(selection: Set[A]) = copy(selection = selection)        // implementation of copy method based on the case class copy
}

case class ConfigurationFilter[A](label: String, instrument: SPComponentType, group: String, values: Set[A], selection: Set[A], valueName: A => String) extends OptionsFilter[A] {
  val sortedValues: Seq[A] = values.toList.sortBy(valueName)
  def name = selection.map(valueName).mkString(",")
  def predicate(o: Obs, ctx: QvContext) =
    if (!o.getInstrumentComponentType.equals(instrument)) false
    else selection.intersect(collector(o, ctx)).nonEmpty

  // implementation of copy method based on the case class copy
  def updated(selection: Set[A]) = copy(selection = selection)

  // helper to retrieve all configuration options of a specific type from the observation
  // Note: for configurations like dispersers and filters there can be several values because configurations
  // can change between steps of an observation (concept of iterators)
  def collector(o: Obs, ctx: QvContext): Set[A] =
    o.getOptions.
      filter(values.contains(_)).
      map(_.asInstanceOf[A]).
      toSet // convert from mutable to immutable..

}

sealed trait RangeFilter extends Filter {
  def highest: Double
  def lowest: Double
  def getter: (Obs, QvContext) => Double
  def min: Double
  def max: Double
  def name = toName

  override def categoryName = toCategoryName
  def maxFromString(s: String): Double = try { s.toDouble } catch { case _: Throwable => highest }
  def minFromString(s: String): Double = try { s.toDouble } catch { case _: Throwable => lowest }
  def predicate(o: Obs, ctx: QvContext) = {
    val value = getter(o, ctx)
    value >= min && value < max
  }
  override def isEmpty = min <= lowest && max >= highest

  override def compare(that: Filter) = that match {
    // comparing with another range filter use the order defined on the filter values instead of the name
    // (important to avoid sorting like 1.0,11.0,5.0,55.0,7.0 etc.)
    case that: RangeFilter => if (this.min < that.min) -1 else if (this.min == that.min) 0 else 1
    case _ => super.compare(that)
  }

  private def toName: String =
    if (max-min < 0.5)        f"$min%.2f"
    else if (max-min < 1.0)   f"$min%.1f"
    else                      f"$min%.0f"

  private def toCategoryName: String =
    if (max-min < 0.5)        f"$label:$min%.2f-$max%.2f"
    else if (max-min < 1.0)   f"$label:$min%.1f-$max%.1f"
    else                      f"$label:$min%.0f-$max%.0f"

}

case class EmptyFilter(name: String = "Empty") extends Filter {
  def label = name
  def predicate(o: Obs, ctx: QvContext) = true
  override def isEmpty = true
  override def isActive = false
}

case class FilterOr(a: Filter, b: Filter) extends Filter {
  def label = name
  def name = a.name + ":" + b.name
  def predicate(o: Obs, ctx: QvContext) = a.predicate(o, ctx) || b.predicate(o, ctx)
  override def elements = a.elements ++ b.elements
}

case class FilterAnd(a: Filter, b: Filter) extends Filter {
  def label = name
  def name = a.name + "; " + b.name
  def predicate(o: Obs, ctx: QvContext) = a.predicate(o, ctx) && b.predicate(o, ctx)
  override def elements = a.elements ++ b.elements
  override def isEmpty = a.isEmpty && b.isEmpty
  override def isActive = a.isActive || b.isActive
}

/** Helper class to represent main filters. */
case class FilterSet(label: String, filters: Set[Filter]) extends NamedElement


object Filter {

  // All possible filter definitions. Any changes here MUST be reflected in the XML reading and writing mechanism.
  // See FilterXMLParser and FilterXMLFormatter.

  /** A filter that represents an arbitrary set of observations. */
  case class Other(observations: Set[Obs]) extends Filter {
    def name = "Other"
    def label = "Other"
    def predicate(o: Obs, ctx: QvContext) = observations.contains(o)
  }

  case class Ambiguous(observations: Set[Obs]) extends Filter {
    def name = "Ambiguous"
    def label = "Ambiguous"
    def predicate(o: Obs, ctx: QvContext) = observations.contains(o)
  }

  case class ObservationSet(observations: Set[Obs], name: String = "Observations") extends Filter {
    def label = "Observation"
    def predicate(o: Obs, ctx: QvContext) = observations.contains(o)
  }

  case class Observation(observation: Obs) extends Filter {
    def name = observation.getObsId
    def label = "Observation"
    def predicate(o: Obs, ctx: QvContext) = o == observation
  }

  object Observation {
    def forObservations(data: Set[Obs], max: Int = Int.MaxValue): Seq[Filter] = {
      data.take(max).toSeq.sortBy(_.getObsId).map(o => {
        Filter.Observation(o)
      })
    }
  }

  case class Program(program: Prog) extends Filter {
    def name = program.getProgramId.stringValue
    def label = "Program"
    def predicate(o: Obs, ctx: QvContext) = o.getProg == program
  }

  object Program {
    def forPrograms(data: Set[Obs], max: Int = Int.MaxValue): Seq[Filter] = {
      data.map(_.getProg).take(max).toSeq.sortBy(_.getProgramId.stringValue()).map(p => {
        Filter.Program(p)
      })
    }
  }

  // =======================  *** Range Filters ***
  sealed trait SimpleRangeFilter extends RangeFilter

  object RA {
    val MinValue = 0.0
    val MaxValue = 24.0

    def apply(min: RightAscension, max: RightAscension) =
      new RA(min.toAngle.toDegrees / 15.0, max.toAngle.toDegrees / 15.0)

  }
  case class RA(min: Double = RA.MinValue, max: Double = RA.MaxValue) extends SimpleRangeFilter {
    def label = "RA"
    def lowest = RA.MinValue
    def highest = RA.MaxValue
    def getter = (o: Obs, ctx: QvContext) => o.raDeg(ctx) / 15.0
    override def desc = "Filter for target right ascension, wraps around 24hrs if min > max (e.g. [18..5])."
    override def predicate(o: Obs, ctx: QvContext) = {
      val ra = getter(o, ctx)
      if (o.hasDummyTarget) {
        false
      } else if (min <= max) {
        min <= ra && ra < max
      } else {
        (0.0 <= ra && ra < max) || (min <= ra)
      }
    }
  }

  object Dec {
    val MinValue: Double = -90.0
    val MaxValue: Double = 90.0
  }
  case class Dec(min: Double = Dec.MinValue, max: Double = Dec.MaxValue) extends SimpleRangeFilter {
    def label = "Dec"
    def lowest = Dec.MinValue
    def highest = Dec.MaxValue
    def getter = (o: Obs, ctx: QvContext) => o.decDeg(ctx)
    override def desc = "Filter for targets with min ≤ declination < max."
  }

  // =======================  *** Set time filter ***
  case class SetTime(ctx: QvContext, min: Double = 0, max: Double = 15) extends SimpleRangeFilter {
    def label = "Set Time Hrs"
    def getter = (o: Obs, ctx: QvContext) => SolutionProvider(ctx).remainingHours(ctx, o).getOrElse(min.toLong).toDouble / DateTimeUtils.MillisecondsPerHour
    def lowest = 0
    def highest = Double.MaxValue
    override def desc = "Filter for targets with number of hours before the target sets tonight in the range min ≤ hours < max."
  }

  // =======================  *** Remaining time filters ***
  /**
   * Filters for the time an observation will be observable from now til the end of the current or the next semester.
   * In order to be able to calculate those times these filters are dependent on the QV context they are evaluated in.
   * These filters can be deactivated (they will always return true in that case) and they allow to select if the time
   * for this semester, the next semester or for both semesters should be taken into account.
   * Note: Alternatively we could also pass in a method that does the calculations to remove the dependency from
   * the context but for now having a dependency on the context seems to be ok.
   */
  sealed trait RemainingTimeFilter extends RangeFilter {
    def enabled: Boolean
    def thisSemester: Boolean
    def nextSemester: Boolean
    def lowest = 0
    def highest = Double.MaxValue
    override def predicate(o: Obs, ctx: QvContext)  = if (enabled) super.predicate(o, ctx) else true
  }

  /** Filters on the numbers of nights this observation is still observable. */
  case class RemainingNights(ctx: QvContext, min: Double = 0, max: Double = 3, enabled: Boolean = false, thisSemester: Boolean = true, nextSemester: Boolean = false) extends RemainingTimeFilter {
    def label = "Rem. Nights"
    def getter = (o: Obs, ctx: QvContext) => SolutionProvider(ctx).remainingNights(ctx, o, thisSemester, nextSemester)
    override def desc = "Show only observations which are observable for a number of nights in the range min ≤ nights < max."
  }

  /** Filters on the number of hours this observation is still observable. */
  case class RemainingHours(ctx: QvContext, min: Double = 0, max: Double = 24, enabled: Boolean = false, thisSemester: Boolean = true, nextSemester: Boolean = false) extends RemainingTimeFilter {
    def label = "Rem. Hours"
    def getter = (o: Obs, ctx: QvContext) => SolutionProvider(ctx).remainingTime(ctx, o, thisSemester, nextSemester).toDouble / DateTimeUtils.MillisecondsPerHour
    override def desc = "Show only observations which are observable for a number of hours in the range min ≤ hours < max."
  }

  /**
   * Filters on the fraction of observable time divided by remaining time.
   * A fraction smaller than one means we won't have enough time to finish this observation, 1 means we have just
   * about the time it takes, and a value > 1 means we have more time available than needed. The smaller the value
   * the higher the probability we are going to loose this observation.
   */
  case class RemainingHoursFraction(ctx: QvContext, min: Double = 0, max: Double = 3, enabled: Boolean = false, thisSemester: Boolean = true, nextSemester: Boolean = false) extends RemainingTimeFilter {
    def label = "Rem. Frac."
    def getter = (o: Obs, ctx: QvContext) =>  SolutionProvider(ctx).remainingTime(ctx, o, thisSemester, nextSemester).toDouble / o.getRemainingTime
    override def desc = "Filter observations with fraction of remaining observable hours divided by observation remaining hours in the range min ≤ fraction < max."
  }

  // =======================  Boolean Filters =======================
  /**
   * Base trait for boolean filters.
   * Boolean filters use a [[scala.Option]] to represent their current value in order to be able to not only
   * model the states {{{true}}} and {{{false}}} but also "I don't care" or "accept both values", which is
   * represented by {{{None}}}.
   */
  sealed trait BooleanFilter extends Filter {
    def getter: (Obs, QvContext)  => Boolean
    def value: Option[Boolean]
    override def name = value.toString
    override def categoryName = f"$label:$value"
    override def predicate(o: Obs, ctx: QvContext)  = value.forall(_ == getter(o, ctx))
  }

  /** Checks if the observation's program is active. */
  case class IsActive(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Active"
    def getter = (o: Obs, ctx: QvContext) => o.getProg.isActive
  }
  /** Checks if the observation's program is completed. */
  case class IsCompleted(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Completed"
    def getter = (o: Obs, ctx: QvContext) => o.getProg.isCompleted
  }
  /** Checks if the observation's program is marked for rollover. */
  case class IsRollover(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Rollover"
    def getter = (o: Obs, ctx: QvContext) => o.getProg.getRollover
  }
  /** Checks if the observation has time constraints (time windows). */
  case class HasTimingConstraints(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Timing Constraints"
    def getter = (o: Obs, ctx: QvContext) => o.hasTimingConstraints
  }
  /** Checks if the observation has elevation constraints (hour angle or airmass). */
  case class HasElevationConstraints(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Elevation Constraints"
    def getter = (o: Obs, ctx: QvContext) => o.hasElevationConstraints
  }
  /** Checks if the observation uses pre-imaging. */
  case class HasPreImaging(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Pre-Imaging"
    def getter = (o: Obs, ctx: QvContext) => o.getPreImaging.getOrElse(false)
  }
  /** Checks if the observation's science target is non-sidereal. */
  case class HasNonSidereal(value: Option[Boolean] = None) extends BooleanFilter {
    def label = "Non Sidereal"
    def getter = (o: Obs, ctx: QvContext) => o.hasNonSidereal
  }

  /** Checks if this observation has a dummy target, i.e. ra == 0 and dec == 0.
      This filter is used in histograms with RA axes to collect all dummy targets. */
  case class HasDummyTarget(value: Option[Boolean] = None) extends BooleanFilter {
    val label = "Dummy"
    override val name = "Dummy"
    def getter = (o: Obs, ctx: QvContext) => o.hasDummyTarget
  }


  // =======================  String Filters =======================
  /**
   * Base trait for string filters.
   * Checks if the given string is a part of the corresponding string in the observation's data.
   */
  sealed trait StringFilter extends Filter {
    def getter: (Obs, QvContext) => String
    def value: String
    override def name = value
    override def categoryName = f"$label:$value"
    // when comparing strings in this filter we don't care about upper/lower case
    override def predicate(o: Obs, ctx: QvContext)  = getter(o, ctx).toLowerCase.contains(value.toLowerCase)
    override def isEmpty = value.isEmpty
  }

  case class ProgPi(value: String = "")  extends StringFilter {
    val label = "PI Last Name"
    def getter = (o: Obs, ctx: QvContext) => o.getProg.getPiLastName
  }
  case class ProgContact(value: String = "")  extends StringFilter {
    val label = "Program Contact"
    // search in Gemini and NGO contacts
    def getter = (o: Obs, ctx: QvContext) => s"${o.getProg.getContactEmail} ${o.getProg.getNgoEmail}"
  }
  case class ProgId(value: String = "")  extends StringFilter {
    val label = "Program ID"
    def getter = (o: Obs, ctx: QvContext) => o.getProg.getProgramId.stringValue()
  }
  case class ObsId(value: String = "")  extends StringFilter {
    val label = "Observation ID"
    def getter = (o: Obs, ctx: QvContext) => o.getObsId
  }


  // =======================  Enumeration Filters =======================

  private val year = Instant.now.atZone(DateTimeUtils.DefaultZone).getYear - 8
  private val yearRange = Range(year, year + 10)
  private val semesters: Set[Semester] = yearRange.flatMap(year => Set(new Semester(year, Half.A), new Semester(year, Half.B))).toSet
  object Semester extends EnumFilterFactory[Semester](
    "Semesters",
    semesters,
    (o: Obs, ctx: QvContext) => ProgramId.parse(o.getProg.getProgramId.stringValue).semester.get
  )

  object Bands extends EnumFilterFactory[Band](
    "Band",
    Band.values().toSet,
    (o: Obs, ctx: QvContext) => o.getProg.getBandEnum,
    _.displayValue
  )

  object Instruments extends EnumFilterFactory[SPComponentType](
    "Instruments",
    InstRegistry.instance.types.iterator().toSet,
    (o: Obs, ctx: QvContext) => o.getInstrumentComponentType,
    _.readableStr
  )

  object Ao extends EnumFilterFactory[AoUsage](
    "AO",
    Set(AoUsage.None, AoUsage.Ngs, AoUsage.Lgs),
    (o: Obs, ctx: QvContext) => FilterUtil.aoUsage(o)
  )

  object Priorities extends EnumFilterFactory[Priority](
    "Priority",
    Priority.values.toSet,
    (o: Obs, ctx: QvContext) => o.getPriority,
    _.displayValue
  )

  object IQs extends EnumFilterFactory[ImageQuality](
    "Image Quality",
    ImageQuality.values.toSet,
    (o: Obs, ctx: QvContext) => o.getImageQuality
  )

  object CCs extends EnumFilterFactory[CloudCover](
    "Cloud Cover",
    CloudCover.values.toSet,
    (o: Obs, ctx: QvContext) => o.getCloudCover
  )

  object WVs extends EnumFilterFactory[WaterVapor](
    "Water Vapor",
    WaterVapor.values.toSet,
    (o: Obs, ctx: QvContext) => o.getWaterVapor
  )

  object SBs extends EnumFilterFactory[SkyBackground](
    "Sky Background",
    SkyBackground.values.toSet,
    (o: Obs, ctx: QvContext) => o.getSkyBackground
  )

  object TOOs extends EnumFilterFactory[TooType](
    "TOO Type",
    TooType.values.toSet,
    (o: Obs, ctx: QvContext) => o.getTooPriority,
    _.getDisplayValue
  )

  object Types extends EnumFilterFactory[ProgramType] (
    "Program Types",
    ProgramType.All.toSet,
    (o: Obs, ctx: QvContext) => ProgramId.parse(o.getProg.getProgramId.stringValue).ptype.orNull,
    { t: ProgramType => f"${t.name} (${t.abbreviation})" }
  )

  object Statuses extends EnumFilterFactory[ObservationStatus] (
    "Observation Status",
    ObservationStatus.values.toSet,
    (o: Obs, ctx: QvContext) => o.getObsStatus
  )

  object Classes extends EnumFilterFactory[ObsClass] (
    "Observation Class",
    ObsClass.values.toSet,
    (o: Obs, ctx: QvContext) => o.getObsClass
  )

  /** Helper object that allows to deal with the fact that the affiliate can indeed be null. */
  sealed case class Partner(affiliate: Option[Affiliate]) {
    def displayValue: String = affiliate.map(_.displayValue).getOrElse("None")
  }
  /** Partner filter uses the partner class which wraps an optional affiliate value. */
  object Partners extends EnumFilterFactory[Partner] (
    "Partner",
    Affiliate.values().map(a => Partner(Some(a))).toSet + Partner(None),
    (o: Obs, ctx: QvContext) => Partner(Option(o.getProg.getPartner)),
    _.displayValue
  )

  object GmosN {
    object Dispersers extends ConfigurationFilterFactory(InstGmosNorth.SP_TYPE, "Dispersers", DisperserNorth.values.toSet)
    object Filters extends ConfigurationFilterFactory[FilterNorth](InstGmosNorth.SP_TYPE, "Filters", FilterNorth.values.toSet)
    object FocalPlanes extends ConfigurationFilterFactory[FPUnitNorth](InstGmosNorth.SP_TYPE, "Focal Planes", FPUnitNorth.values.toSet)
    object CcdManufacturers extends ConfigurationFilterFactory[DetectorManufacturer](InstGmosNorth.SP_TYPE, "CCD", DetectorManufacturer.values.toSet)
  }

  object GmosS {
    object Dispersers extends ConfigurationFilterFactory[DisperserSouth](InstGmosSouth.SP_TYPE, "Dispersers", DisperserSouth.values.toSet)
    object Filters extends ConfigurationFilterFactory[FilterSouth](InstGmosSouth.SP_TYPE, "Filters", FilterSouth.values.toSet)
    object FocalPlanes extends ConfigurationFilterFactory[FPUnitSouth](InstGmosSouth.SP_TYPE, "Focal Planes", FPUnitSouth.values.toSet)
    object CcdManufacturers extends ConfigurationFilterFactory[DetectorManufacturer](InstGmosSouth.SP_TYPE, "CCD", DetectorManufacturer.values.toSet)
  }

  object GNIRS {
    object Dispersers extends ConfigurationFilterFactory(InstGNIRS.SP_TYPE, "Dispersers", GNIRSParams.Disperser.values.toSet)
    object Filters extends ConfigurationFilterFactory(InstGNIRS.SP_TYPE, "Filters", GNIRSParams.Filter.values.toSet)
    object CrossDispersers extends ConfigurationFilterFactory(InstGNIRS.SP_TYPE, "Cross Dispersers", GNIRSParams.CrossDispersed.values.toSet)
    object Cameras extends ConfigurationFilterFactory(InstGNIRS.SP_TYPE, "Cameras", GNIRSParams.Camera.values.toSet)
    object FocalPlanes extends ConfigurationFilterFactory(InstGNIRS.SP_TYPE, "Focal Planes", GNIRSParams.SlitWidth.values.toSet)
  }

  object F2 {
    object Dispersers extends ConfigurationFilterFactory(Flamingos2.SP_TYPE, "Dispersers", Flamingos2.Disperser.values.toSet)
    object Filters extends ConfigurationFilterFactory(Flamingos2.SP_TYPE, "Filters", Flamingos2.Filter.values.toSet)
    object FocalPlanes extends ConfigurationFilterFactory(Flamingos2.SP_TYPE, "Focal Planes", Flamingos2.FPUnit.values.toSet)
  }

  object GSAOI {
    object Filters extends ConfigurationFilterFactory(Gsaoi.SP_TYPE, "Filters", Gsaoi.Filter.values.toSet)
  }

  object NIFS {
    object Dispersers extends ConfigurationFilterFactory(InstNIFS.SP_TYPE, "Dispersers", NIFSParams.Disperser.values.toSet)
    object Filters extends ConfigurationFilterFactory(InstNIFS.SP_TYPE, "Filters", NIFSParams.Filter.values.toSet)
    object Masks extends ConfigurationFilterFactory(InstNIFS.SP_TYPE, "Masks", NIFSParams.Mask.values.toSet)
  }

  object NICI {
    object FocalPlanes extends ConfigurationFilterFactory(InstNICI.SP_TYPE, "Focal Planes", NICIParams.FocalPlaneMask.values.toSet)
    object DichroicWheels extends ConfigurationFilterFactory(InstNICI.SP_TYPE, "Dichroic Wheel", NICIParams.DichroicWheel.values.toSet)
    object RedFilters extends ConfigurationFilterFactory(InstNICI.SP_TYPE, "Filter Red Channel", NICIParams.Channel1FW.values.toSet)
    object BlueFilters extends ConfigurationFilterFactory(InstNICI.SP_TYPE, "Filter Blue Channel", NICIParams.Channel2FW.values.toSet)
  }

  object NIRI {
    object Dispersers extends ConfigurationFilterFactory(InstNIRI.SP_TYPE, "Dispersers", Niri.Disperser.values.toSet)
    object Filters extends ConfigurationFilterFactory(InstNIRI.SP_TYPE, "Filters", Niri.Filter.values.toSet)
    object Cameras extends ConfigurationFilterFactory(InstNIRI.SP_TYPE, "Cameras", Niri.Camera.values.toSet)
    object Masks extends ConfigurationFilterFactory(InstNIRI.SP_TYPE, "Masks", Niri.Mask.values.toSet)
  }

  object Texes {
    object Dispersers extends ConfigurationFilterFactory(InstTexes.SP_TYPE, "Dispersers", TexesParams.Disperser.values.toSet)
  }

  // Factory for enum filter creation.
  class EnumFilterFactory[A](label: String, values: Set[A], getter: (Obs, QvContext) => A, valueName: A => String = {v: A => v.toString}) {
    def apply(selection: Set[A]): EnumFilter[A] = EnumFilter(label, values, getter, selection, valueName)
    def apply(selection: A): EnumFilter[A] = apply(Set(selection))
    def apply(): EnumFilter[A] = apply(values)
  }

  // Factory for enum filter creation.
  class ConfigurationFilterFactory[A <: DisplayableSpType](instrument: SPComponentType, group: String, values: Set[A], valueName: A => String = {v: A => v.displayValue}) {
    def apply(selection: Set[A]): ConfigurationFilter[A] = ConfigurationFilter(instrument.readableStr + " " + group, instrument, group, values, selection, valueName)
    def apply(selection: A): ConfigurationFilter[A] = apply(Set(selection))
    def apply(): ConfigurationFilter[A] = apply(values)
  }

}
