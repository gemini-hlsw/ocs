package edu.gemini.qv.plugin.filter.core

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.Band
import edu.gemini.qv.plugin.QvStore
import edu.gemini.qv.plugin.QvStore.{BarChart, Histogram, Table}
import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.filter.core.Filter._
import edu.gemini.spModel.core.{Affiliate, ProgramType, Semester}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.SPObservation.Priority
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.too.TooType

import scala.util.{Failure, Try}
import scala.xml.{Node, NodeSeq}

object FilterXMLParser {

  def parseFilters(filterNodes: NodeSeq): Try[Seq[FilterSet]] =
    Try(filterNodes.map(x => FilterSet(x \ "name" text, parseFilterSet(x))))

  def parseAxes(axisNodes: NodeSeq): Try[Seq[Axis]] =
    Try(axisNodes.map(x => Axis(x \ "name" text, parseFilters(x))))

  def parseHistograms(chartNodes: NodeSeq, axes: Map[String, Axis]): Try[Seq[Histogram]] =
    Try(chartNodes.map(x => QvStore.histogramFromXml(x, axes)))

  def parseVisCharts(chartNodes: NodeSeq, axes: Map[String, Axis]): Try[Seq[BarChart]] =
    Try(chartNodes.map(x => QvStore.barChartFromXml(x, axes)))

  def parseTables(tableNodes: NodeSeq, axes: Map[String, Axis]): Try[Seq[Table]] =
    Try(tableNodes.map(x => QvStore.tableFromXml(x, axes)))


  private def parseFilters(axisNode: Node): Seq[Filter] = {
    val filters = for {
      filter <- (axisNode \ "filtergroups").head.child.map(parseFilter)
      if filter.isSuccess
    } yield filter.get
    filters
  }

  private def parseFilterSet(filterNode: Node): Set[Filter] = {
    val filters = for {
      filter <- (filterNode \ "filterset").head.child.map(parseFilter)
      if filter.isSuccess
    } yield filter.get
    filters.toSet
  }

  private def parseFilter(input: Node): Try[Filter] = {
    // Generalized functions to deal with sets, ranges, etc, to improve readability and enforce standardization.

    // parseSetNode should take a <set>...</set> node, and the type must be specified at compile time.
    def parseEnumSetNode[T <: Enum[T]](cls: Class[T], setNode: Node): Try[Set[T]] =
      Try((setNode \ "elem").map(x => Enum.valueOf(cls, x.text)).toSet)

    // Partners types are not enums..
    def parseSetPartner(setNode: Node): Try[Set[Partner]] =
      Try((setNode \ "elem").map(x => x.text match {
        case "none" => Partner(None)
        case _ => Partner(Some(Affiliate.valueOf(x.text)))
      }).toSet)
    // Program types are not enums..
    def parseSetType(setNode: Node): Try[Set[ProgramType]] =
      Try((setNode \ "elem").map(x => ProgramType.read(x.text).get).toSet)
    // Semesters are not enums..
    def parseSetSemester(setNode: Node): Try[Set[Semester]] =
      Try((setNode \ "elem").map(x => Semester.parse(x.text)).toSet)
    // AO usage are not enums..
    def parseSetAO(setNode: Node): Try[Set[AoUsage]] =
      Try((setNode \ "elem").map(x => x.text match {
        case "None" => AoUsage.None
        case "Lgs" => AoUsage.Lgs
        case "Ngs" => AoUsage.Ngs
      }).toSet)


    // parseRangeNode should take a <range><min>...</min><max>...</max></range>, and the type must be the type of the range,
    // with conv being a function to convert a String to the range type.
    def parseRangeNode[A](rangeNode: Node)(implicit conv: String => A): Try[(A, A)] = Try {
      rangeNode match { case <range><min>{low}</min><max>{high}</max></range> => (conv(low.text), conv(high.text)) }
    }
    implicit def stringToDouble(value: String): Double = augmentString(value).toDouble

    // Alternatively, for RangedFilters, can do:
    // parseRangeNode[Double](range).map(x => (Filter.RAs.apply _).tupled(x))
    input match {
      case <rafilter>{range}</rafilter>  => for { (low, high) <- parseRangeNode[Double](range) } yield Filter.RA(low, high)
      case <decfilter>{range}</decfilter> => for { (low, high) <- parseRangeNode[Double](range) } yield Filter.Dec(low, high)

      case <isnonsidereal><boolvalue>{value}</boolvalue></isnonsidereal> => Try(Filter.HasNonSidereal(fromBoolean(value.text)))
      case <isactive><boolvalue>{value}</boolvalue></isactive> => Try(Filter.IsActive(fromBoolean(value.text)))
      case <iscompleted><boolvalue>{value}</boolvalue></iscompleted> => Try(Filter.IsCompleted(fromBoolean(value.text)))
      case <rolloverfilter><boolvalue>{value}</boolvalue></rolloverfilter> => Try(Filter.IsRollover(fromBoolean(value.text)))
      case <timingConstraints><boolvalue>{value}</boolvalue></timingConstraints> => Try(Filter.HasTimingConstraints(fromBoolean(value.text)))
      case <elevationConstraints><boolvalue>{value}</boolvalue></elevationConstraints> => Try(Filter.HasElevationConstraints(fromBoolean(value.text)))
      case <preImaging><boolvalue>{value}</boolvalue></preImaging> => Try(Filter.HasPreImaging(fromBoolean(value.text)))
      case <dummyTarget><boolvalue>{value}</boolvalue></dummyTarget> => Try(Filter.HasDummyTarget(fromBoolean(value.text)))

      case <progidfilter><id>{value}</id></progidfilter> => Try(Filter.ProgId(value.text))
      case <progpifilter><id>{value}</id></progpifilter> => Try(Filter.ProgPi(value.text))
      case <progcontactfilter><id>{value}</id></progcontactfilter> => Try(Filter.ProgContact(value.text))
      case <obsidfilter><id>{value}</id></obsidfilter> => Try(Filter.ObsId(value.text))

      // TODO: using a broken null dummy context is super ugly, there must be a better solution for this
      // (the dummy context will be replaced with the appropriate one when the filter pages are built for the UI)
      case <remnightsfilter>{range}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remnightsfilter> =>
        for { (low, high) <- parseRangeNode[Double](range) } yield RemainingNights(null, low, high, fromBoolean(enabled.text).get, fromBoolean(cur.text).get, fromBoolean(next.text).get)
      case <remhoursfilter>{range}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remhoursfilter> =>
        for { (low, high) <- parseRangeNode[Double](range) } yield RemainingHours(null, low, high, fromBoolean(enabled.text).get, fromBoolean(cur.text).get, fromBoolean(next.text).get)
      case <remhoursfracfilter>{range}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remhoursfracfilter> =>
        for { (low, high) <- parseRangeNode[Double](range) } yield RemainingHoursFraction(null, low, high, fromBoolean(enabled.text).get, fromBoolean(cur.text).get, fromBoolean(next.text).get)
      case <settimefilter>{range}</settimefilter> =>
        for { (low, high) <- parseRangeNode[Double](range) } yield SetTime(null, low, high)

      case <semesterfilter>{setNode}</semesterfilter> => parseSetSemester(setNode).map(Filter.Semester(_))
      case <partnerfilter>{setNode}</partnerfilter> => parseSetPartner(setNode).map(Filter.Partners(_))
      case <aofilter>{setNode}</aofilter> => parseSetAO(setNode).map(Filter.Ao(_))
      case <bandfilter>{setNode}</bandfilter> => parseEnumSetNode(classOf[Band], setNode).map(Filter.Bands(_))
      case <instrumentfilter>{setNode}</instrumentfilter> => parseEnumSetNode(classOf[SPComponentType], setNode).map(Filter.Instruments(_))
      case <priorityfilter>{setNode}</priorityfilter> => parseEnumSetNode(classOf[Priority], setNode).map(Filter.Priorities(_))
      case <iqfilter>{setNode}</iqfilter> => parseEnumSetNode(classOf[ImageQuality], setNode).map(Filter.IQs(_))
      case <ccfilter>{setNode}</ccfilter> => parseEnumSetNode(classOf[CloudCover], setNode).map(Filter.CCs(_))
      case <wvfilter>{setNode}</wvfilter> => parseEnumSetNode(classOf[WaterVapor], setNode).map(Filter.WVs(_))
      case <sbfilter>{setNode}</sbfilter> => parseEnumSetNode(classOf[SkyBackground], setNode).map(Filter.SBs(_))
      case <toofilter>{setNode}</toofilter> => parseEnumSetNode(classOf[TooType], setNode).map(Filter.TOOs(_))
      case <obsStatus>{setNode}</obsStatus> => parseEnumSetNode(classOf[ObservationStatus], setNode).map(Filter.Statuses(_))
      case <obsClass>{setNode}</obsClass> => parseEnumSetNode(classOf[ObsClass], setNode).map(Filter.Classes(_))
      case <typefilter>{setNode}</typefilter> => parseSetType(setNode).map(Filter.Types(_))

      // GMOSN
      case <gmosndisp>{setNode}</gmosndisp> => parseEnumSetNode(classOf[DisperserNorth], setNode).map(Filter.GmosN.Dispersers(_))
      case <gmosnfilt>{setNode}</gmosnfilt> => parseEnumSetNode(classOf[FilterNorth], setNode).map(Filter.GmosN.Filters(_))
      case <gmosnfocplane>{setNode}</gmosnfocplane> => parseEnumSetNode(classOf[FPUnitNorth], setNode).map(Filter.GmosN.FocalPlanes(_))
      case <gmosnccd>{setNode}</gmosnccd> => parseEnumSetNode(classOf[DetectorManufacturer], setNode).map(Filter.GmosN.CcdManufacturers(_))

      // GMOSS
      case <gmossdisp>{setNode}</gmossdisp> => parseEnumSetNode(classOf[DisperserSouth], setNode).map(Filter.GmosS.Dispersers(_))
      case <gmossfilt>{setNode}</gmossfilt> => parseEnumSetNode(classOf[FilterSouth], setNode).map(Filter.GmosS.Filters(_))
      case <gmossfocplane>{setNode}</gmossfocplane> => parseEnumSetNode(classOf[FPUnitSouth], setNode).map(Filter.GmosS.FocalPlanes(_))
      case <gmossccd>{setNode}</gmossccd> => parseEnumSetNode(classOf[DetectorManufacturer], setNode).map(Filter.GmosS.CcdManufacturers(_))

      // GNIRS
      case <gnirsdisp>{setNode}</gnirsdisp> => parseEnumSetNode(classOf[GNIRSParams.Disperser], setNode).map(Filter.GNIRS.Dispersers(_))
      case <gnirsfilt>{setNode}</gnirsfilt> => parseEnumSetNode(classOf[GNIRSParams.Filter], setNode).map(Filter.GNIRS.Filters(_))
      case <gnirscrossdisp>{setNode}</gnirscrossdisp> => parseEnumSetNode(classOf[GNIRSParams.CrossDispersed], setNode).map(Filter.GNIRS.CrossDispersers(_))
      case <gnirscam>{setNode}</gnirscam> => parseEnumSetNode(classOf[GNIRSParams.Camera], setNode).map(Filter.GNIRS.Cameras(_))
      case <gnirsfocplane>{setNode}</gnirsfocplane> => parseEnumSetNode(classOf[GNIRSParams.SlitWidth], setNode).map(Filter.GNIRS.FocalPlanes(_))

      // GSAOI
      case <gsaoifilt>{setNode}</gsaoifilt> => parseEnumSetNode(classOf[Gsaoi.Filter], setNode).map(Filter.GSAOI.Filters(_))

      // F2
      case <f2disp>{setNode}</f2disp> => parseEnumSetNode(classOf[Flamingos2.Disperser], setNode).map(Filter.F2.Dispersers(_))
      case <f2focplane>{setNode}</f2focplane> => parseEnumSetNode(classOf[Flamingos2.FPUnit], setNode).map(Filter.F2.FocalPlanes(_))
      case <f2filt>{setNode}</f2filt> => parseEnumSetNode(classOf[Flamingos2.Filter], setNode).map(Filter.F2.Filters(_))

      // NIFS
      case <nifsdisp>{setNode}</nifsdisp> => parseEnumSetNode(classOf[NIFSParams.Disperser], setNode).map(Filter.NIFS.Dispersers(_))
      case <nifsfilt>{setNode}</nifsfilt> => parseEnumSetNode(classOf[NIFSParams.Filter], setNode).map(Filter.NIFS.Filters(_))
      case <nifsmask>{setNode}</nifsmask> => parseEnumSetNode(classOf[NIFSParams.Mask], setNode).map(Filter.NIFS.Masks(_))

      // NIRI
      case <niridisp>{setNode}</niridisp> => parseEnumSetNode(classOf[Niri.Disperser], setNode).map(Filter.NIRI.Dispersers(_))
      case <nirifilt>{setNode}</nirifilt> => parseEnumSetNode(classOf[Niri.Filter], setNode).map(Filter.NIRI.Filters(_))
      case <niricam>{setNode}</niricam> => parseEnumSetNode(classOf[Niri.Camera], setNode).map(Filter.NIRI.Cameras(_))
      case <nirimask>{setNode}</nirimask> => parseEnumSetNode(classOf[Niri.Mask], setNode).map(Filter.NIRI.Masks(_))

      case <filterall><name>{name}</name></filterall> => Try(EmptyFilter(name.text))
      case <or><first>{a}</first><second>{b}</second></or> => for {
        first  <- FilterXMLParser.parseFilter(a)
        second <- FilterXMLParser.parseFilter(b)
      } yield FilterOr(first, second)
      case <and><first>{a}</first><second>{b}</second></and> => for {
        first  <- FilterXMLParser.parseFilter(a)
        second <- FilterXMLParser.parseFilter(b)
      } yield FilterAnd(first, second)

      // FAILURE
      case _ => Failure(new MatchError(input))
    }
  }

  def fromBoolean(value: String): Option[Boolean] =
    if (value.equals("none")) None else Some(value.toBoolean)
}