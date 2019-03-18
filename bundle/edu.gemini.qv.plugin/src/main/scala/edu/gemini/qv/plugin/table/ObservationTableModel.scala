package edu.gemini.qv.plugin
package table

import java.util.{Comparator, TimeZone}
import javax.swing.table.{AbstractTableModel, TableRowSorter}

import edu.gemini.qpt.shared.sp.{Band, Obs}
import edu.gemini.qv.plugin.table.ObservationTableModel.Columns
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.`type`.{DisplayableSpType, LoggableSpType}
import edu.gemini.spModel.core.{Affiliate, Angle}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{ElevationConstraintType, TimingWindow}
import edu.gemini.spModel.ictd.CustomMaskKey
import edu.gemini.spModel.ictd.Availability.Installed
import jsky.coords.DMS

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

object ObservationTableModel {

  case class Column[T: Manifest](name: String, tip: String, value: Obs => T, comparator: Option[Comparator[T]] = None, visibleAtStart: Boolean = true) {
    def myClass: Class[T] = manifest[T].runtimeClass.asInstanceOf[Class[T]]
  }

  final class Columns(ctx: QvContext) {

    val installationStateColumn: Column[InstallationState] =
      Column(
        "Installed",
        "Instrument configuration installed",
        installationState(ctx, _)
      )

    val header: Vector[Column[_]] = Vector(
      Column[String](
        "Observation ID",
        "",
        _.getObsId, Some(ObsIdComparator), visibleAtStart = false
      )
    )

    val data: Vector[Column[_]] = Vector(
      Column[String](
        "Partner",
        "Partner country",
        o => asString(o.getProg.getPartner),
        visibleAtStart = true
      ),
      Column[String](
        "Band",
        "Priority Band; empty for classical",
        o => asString(o.getProg.getBandEnum)
      ),
      Column[RaValue](
        "RA",
        "Right ascension of science target",
        o => RaValue(o.raDeg(ctx))
      ),
      Column[DecValue](
        "Dec",
        "Declination of science target",
        o => DecValue(o.decDeg(ctx))
      ),
      Column[String](
        "Instrument",
        "The instrument",
        _.getInstrumentString
      ),
      Column[String](
        "AO",
        "Instrument configuration includes AO?",
        o => asString(o.getAO)
      ),
      Column[String](
        "LGS",
        "Instrument configuration includes laser?",
        o => asString(o.getLGS)
      ),
      Column[String](
        "Priority",
        "User priority",
        _.getPriority.displayValue
      ),
      Column[String](
        "Status",
        "Observation status", _.getObsStatus.displayValue
      ),
      Column[String](
        "Class",
        "Observation class",
        _.getObsClass.displayValue,
        visibleAtStart = false
      ),
      Column[String](
        "TOO",
        "Target of opportunity type",
        _.getTooPriority.getDisplayValue
      ),
      Column[String](
        "NonSid",
        "Has Non sidereal target?",
        o => asString(o.hasNonSidereal),
        visibleAtStart = true
      ),

      // conditions
      Column[String](
        "IQ",
        "Image quality", o => asString(o.getImageQuality),
        visibleAtStart = true
      ),
      Column[String](
        "CC",
        "Cloud coverage",
        o => asString(o.getCloudCover),
        visibleAtStart = true
      ),
      Column[String](
        "WV",
        "Water vapor",
        o => asString(o.getWaterVapor),
        visibleAtStart = true
      ),
      Column[String](
        "SB",
        "Sky brightness",
        o => asString(o.getSkyBackground),
        visibleAtStart = true
      ),

      // general instrument stuff
      Column[String](
        "Filter",
        "Configured filters",
        _.getFilters.asScala.map(asString).mkString(", ")
      ),
      Column[String](
        "Disperser",
        "Configured disperses ",
        _.getDispersers.asScala.map(asString).mkString(", ")
      ),
      Column[String](
        "FPU",
        "Configured FPUs",
        _.getFocalPlanUnits.asScala.map(asString).mkString(", ")
      ),
      Column[String](
        "Camera",
        "Configured cameras",
        _.getCamera.asScala.map(asString).mkString(", "),
        visibleAtStart = false
      ),

      // ==== time information
      Column[TimeValue](
        "Prog Planned Time",
        "Planned time for program",
        o => TimeValue(o.getProg.getPlannedTime),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Prog Used Time",
        "Used time for program",
        o => TimeValue(o.getProg.getUsedTime),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Prog Remaining Time",
        "Remaining time for program",
        o => TimeValue(o.getProg.getRemainingProgramTime),
        visibleAtStart = true
      ),
      Column[String](
        "Prog PI",
        "Program PI",
        _.getProg.getPiLastName,
        visibleAtStart = false
      ),
      Column[String](
        "Prog NGO Contacts",
        "Program NGO contacts",
        _.getProg.getNgoEmail,
        visibleAtStart = false
      ),
      Column[String](
        "Prog Gemini Contacts",
        "Program Gemini contacts",
        _.getProg.getContactEmail,
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Obs Pi Planned Time", "Planned time for observation",
        o => TimeValue(o.getPiPlannedTime),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Obs Exec Planned Time",
        "Planned execution time for observation",
        o => TimeValue(o.getExecPlannedTime),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Obs Elapsed Time",
        "Elapsed time for observation",
        o => TimeValue(o.getElapsedTime),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "Obs Remaining Time",
        "Remaining time for observation",
        o => TimeValue(o.getRemainingTime),
        visibleAtStart = true
      ),

      // ==== Remaining time
      Column[java.lang.Integer](
        "Sem. Nts",
        "Remaining nights from today until the end of current semester.",
        o => SolutionProvider(ctx).remainingNights(ctx, o, thisSemester = true, nextSemester = false),
        visibleAtStart = true
      ),
      Column[TimeValue](
        "Sem. Hrs",
        "Remaining hours from today until the end of current semester.",
        o => TimeValue(SolutionProvider(ctx).remainingTime(ctx, o, thisSemester = true, nextSemester = false)),
        visibleAtStart = true
      ),
      Column[java.lang.Double](
        "Sem. Frac",
        "Fraction of remaining observable hours divided by observation remaining hours until the end of current semester.",
        o => semesterHrsFraction(ctx, o, thisSem = true, nextSem = false),
        visibleAtStart = true
      ),
      Column[java.lang.Integer](
        "+Sem. Nts",
        "Remaining nights from today until the end of next semester.",
        o => SolutionProvider(ctx).remainingNights(ctx, o, thisSemester = true, nextSemester = true),
        visibleAtStart = false
      ),
      Column[TimeValue](
        "+Sem. Hrs",
        "Remaining hours from today until the end of next semester.",
        o => TimeValue(SolutionProvider(ctx).remainingTime(ctx, o, thisSemester = true, nextSemester = true)),
        visibleAtStart = false
      ),
      Column[java.lang.Double](
        "+Sem. Frac",
        "Fraction of remaining observable hours divided by observation remaining hours until the end of next semester.",
        o => semesterHrsFraction(ctx, o, thisSem = true, nextSem = true),
        visibleAtStart = false
      ),

      // ==== Constraints
      Column[String](
        "Timing Windows (UTC)",
        "Timing window restrictions",
        timingWindowsAsString,
        visibleAtStart = true
      ),
      Column[String](
        "Elevation Constraints",
        "Elevation restrictions",
        elevationConstraintsAsString,
        visibleAtStart = true
      ),

      // ==== instrument specific columns, they will be empty except for the instrument the field is specific to
      // GMOS N/S - Nod & Shuffle
      Column[String](
        "GMOS N&S",
        "GMOS nod and shuffle",
        o => asString(o.getGmosNodShuffle.getOrElse(false)),
        visibleAtStart = false
      ),
      // GMOS N/S - CCD Manufacturer
      Column[String](
        "GMOS CCD",
        "GMOS CCD manufacturer",
        { o => val v = o.getGmosCcdManufacturer; if (v.isEmpty) "" else asString(v.getValue) },
        visibleAtStart = false
      ),
      // F2 and GMOS - Pre Imaging
      Column[String](
        "PreImg",
        "",
        o => asString(o.getPreImaging.getOrElse(false)),
        visibleAtStart = true
      ),
      // GNIRS - Cross Dispersed
      Column[String](
        "GNIRS Cross Dispersed",
        "GNIRS cross dispersed mode",
        { o => val v = o.getGnirsCrossDispersed; if (v.isEmpty) "" else asString(v.getValue) },
        visibleAtStart = false
      ),
      // GPI - Observing Mode
      Column[String](
        "GPI Observing Mode",
        "GPI Observing Mode",
        { o => val v = o.getGpiObservingMode; if (v.isEmpty) "" else asString(v.getValue) },
        visibleAtStart = false
      ),
      installationStateColumn
    )

    val all = header ++ data

  }


  /**
   * Determines whether all the components and custom mask (if any) of the given
   * observation are available according to the ICTD.
   */
  def installationState(c: QvContext, o : Obs): InstallationState = {
    import InstallationState._

    c.dataSource.ictd.map { i =>

      // Name of the custom mask, if it is defined but *not* installed.
      val missingMask: Option[String] =
        Option(o.getCustomMask).map(_.trim).filterNot(_.isEmpty).filter { m =>
          CustomMaskKey.parse(m).forall(!i.maskAvailability.get(_).contains(Installed))
        }

      // Display values of all the missing features, if any.
      val missingFeatures: List[String] =
        o.getOptions.asScala.toList.filter { e =>
          i.featureAvailability.get(e).exists(_ =/= Installed)
        }.map {
          case d: DisplayableSpType => d.displayValue
          case l: LoggableSpType    => l.logValue
          case e                    => e.name
        }

      missingMask.toList ++ missingFeatures match {
        case Nil     => AllInstalled
        case h :: t  => SomeNotInstalled(NonEmptyList(h, t: _*))
      }

    }.getOrElse(Unknown)
  }

  private def semesterHrsFraction(ctx: QvContext, o: Obs, thisSem: Boolean, nextSem: Boolean): Double = {
    val hrs = SolutionProvider(ctx).remainingTime(ctx, o, thisSem, nextSem)
    if (o.getRemainingTime > 0) hrs.toDouble / o.getRemainingTime else 10000
  }

  private def asString(o: Any): String = o match {
    case null                 => ""
    case v: Band              => v match {
      case Band.Band1     => "1"
      case Band.Band2     => "2"
      case Band.Band3     => "3"
      case Band.Band4     => "4"
      case Band.Undefined => ""
    }
    case v: Boolean           => if (v) "yes" else ""
    case v: DisplayableSpType => v.displayValue
    case v: LoggableSpType    => v.logValue
    case v: Affiliate         => v.displayValue
    case v                    => v.toString
  }

  /** Convert timing windows into human readable string. */
  private def timingWindowsAsString(o: Obs): String = {
    def formatTime(t: Long): String = TimeUtils.print(t, TimeZone.getTimeZone("UTC"), "yyyy-MM-dd HH:mm")
    def twToString(tw: TimingWindow): String = {
      formatTime(tw.getStart) + {
        if (tw.getDuration == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) " and remains open forever"
        else s" + ${TimeUtils.msToHHMM(tw.getDuration)}" + {
          tw.getRepeat match {
            case TimingWindow.REPEAT_NEVER   => ""
            case TimingWindow.REPEAT_FOREVER => s" every ${TimeUtils.msToHHMM(tw.getPeriod)} forever"
            case _                           => s" every ${TimeUtils.msToHHMM(tw.getPeriod)} x ${tw.getRepeat}"
          }
        }
      }}

    if (o.getTimingWindows.isEmpty) ""
    else o.getTimingWindows.asScala.map(twToString).mkString(", ")
  }

  /** Convert elevation constraints into human readable string. */
  private def elevationConstraintsAsString(o: Obs): String = {
    o.getElevationConstraintType match {
      case ElevationConstraintType.AIRMASS    => o.getElevationConstraintMin + " \u2264 airmass \u2264 " + o.getElevationConstraintMax
      case ElevationConstraintType.HOUR_ANGLE =>
        val min = TimeUtils.MS_PER_HOUR * o.getElevationConstraintMin
        val max = TimeUtils.MS_PER_HOUR * o.getElevationConstraintMax
        TimeUtils.msToHHMMSS(min.toLong) + " \u2264 ha \u2264 " + TimeUtils.msToHHMMSS(max.toLong)
      case _                                  => ""
    }
  }

  // ==== SPECIAL PURPOSE COMPARATORS
  object ObsIdComparator extends Comparator[String] {
    private val ObsIdPattern = """(.*)-(.*)-(\d+) \[(\d+)\]""".r

    def compare(s1: String, s2: String): Int = {
      s1 match {
        case ObsIdPattern(s1s, s1t, s1pId, s1oId) =>
          s2 match {
            case ObsIdPattern(s2s, s2t, s2pId, s2oId) =>
              if (s1s != s2s) s1s compare s2s
              else if (s1t != s2t) s1t compare s2t
              else if (s1pId != s2pId) Integer.parseInt(s1pId) compare Integer.parseInt(s2pId)
              else Integer.parseInt(s1oId) compare Integer.parseInt(s2oId)
            case _                                    =>
              s1 compare s2
          }
        case _                                    =>
          s1 compare s2
      }
    }
  }

  // ==== Wrapper case classes for some values which allow us to define custom renderers and sorting. ===
  case class RaValue(ra: Double) extends Comparable[RaValue] {
    def compareTo(other: RaValue): Int = math.signum(ra - other.ra).toInt
    val prettyString: String = {
      val hms = Angle.fromDegrees(ra).toHMS
      f"${hms.hours}:${hms.minutes}%02d:${hms.seconds}%06.3f"
    }
  }
  case class DecValue(dec: Double) extends Comparable[DecValue] {
    def compareTo(other: DecValue): Int = math.signum(dec - other.dec).toInt
    val prettyString: String = {
      val dms = new DMS(dec)
      val sig = if (dms.getSign < 0) "-" else ""
      f"$sig${dms.getDegrees}:${dms.getMin}%02d:${dms.getSec}%05.2f"
    }
  }
  case class TimeValue(t: Long) extends Comparable[TimeValue] {
    def compareTo(other: TimeValue): Int = math.signum(t - other.t).toInt
    val prettyString: String = TimeUtils.msToHHMMSS(t)
  }

}

/**
 * The table model for browsing selected observations.
 */
class ObservationTableModel(ctx: QvContext) extends AbstractTableModel {

  // we get a function to calculate the values, to avoid having this done at every single access we cache
  // values once they are calculated. This is ok since we recreate the model every time something changes,
  // so we don't need to worry about keeping track of changes in those cached values
  val cachedValues = scala.collection.mutable.Map[(Int, Int), AnyRef]()

  // need a sequence for indexed access in getValueAt()
  private var _observations: Seq[Obs] = Seq()

  val columns: Columns = new Columns(ctx)

  // Find the installation state column index.
  val installationStateColumnIndex: Int =
    columns.all.zipWithIndex.findRight { case (c, i)  =>
      c == columns.installationStateColumn
    }.map(_._2).getOrElse(sys.error(s"missing installationStateColumn"))

  def observations_=(obs: Set[Obs]): Unit = {
    cachedValues.clear()
    _observations = obs.toSeq
    fireTableDataChanged()     // let listeners know, that we just changed all the data
  }
  def observations = _observations


  val rowSorter: TableRowSorter[ObservationTableModel] = {
    val sorter = new TableRowSorter(this)
    columns.all.zipWithIndex.foreach({ case (c, ix) =>
      c.comparator.foreach(sorter.setComparator(ix, _))
    })
    sorter
  }


  // =======



  def getRowCount: Int = observations.size

  def getColumnCount: Int = columns.all.size

  override def getColumnName(col: Int): String = columns.all(col).name

  override def getColumnClass(col: Int): Class[_] = columns.all(col).myClass

  override def isCellEditable(row: Int, col: Int): Boolean = false

  def getValueAt(row: Int, col: Int): AnyRef =
    cachedValues.getOrElseUpdate((row, col),
      (for {
        c <- columns.all.lift(col)
        o <- observations.lift(row)
      } yield c.value(o).asInstanceOf[AnyRef]).getOrElse(sys.error(s"no value at $row, $col"))
    )

  /**
   * Determines whether all the components and custom mask (if any) of the
   * observation at the given row are available according to the ICTD.
   */
  def installationState(row: Int): InstallationState =
    // Use the cached value?  This is a bit gross but avoids recalculating the
    // value repeatedly as the original version would require:
    //   observations.lift(row).map(ObservationTableModel.installationState(c, _)).getOrElse(InstallationState.Unknown)
    getValueAt(row, installationStateColumnIndex) match {
      case is: InstallationState => is
      case x                     => sys.error(s"expected an InstallationState not $x")
    }


}
