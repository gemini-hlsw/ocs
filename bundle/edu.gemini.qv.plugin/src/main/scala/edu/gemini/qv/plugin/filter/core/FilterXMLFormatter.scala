package edu.gemini.qv.plugin.filter.core

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qv.plugin.QvStore
import edu.gemini.qv.plugin.QvStore.{BarChart, Histogram, Table}
import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.filter.core.Filter._

import scala.xml.Node

object FilterXMLFormatter {

  /** Store everything. */
  def formatAll: Node = formatSome(QvStore.filters, QvStore.axes, QvStore.histograms, QvStore.tables, QvStore.visCharts)

  /** Store some data.
    * Only store customer made elements, don't store pre-defined/default ones.
    * This makes sure a fail proof minimal set of axes, charts and tables is always available.
    * @return
    */
  // TODO: The dynamic on-the-fly axes should become part of the default axes, so we don't need to treat them separately.
  def formatSome(filters: Seq[FilterSet] = Seq(), axes: Seq[Axis] = Seq(), histograms: Seq[Histogram] = Seq(), tables: Seq[Table] = Seq(), barCharts: Seq[BarChart] = Seq()): Node =
    <qvTool>
      <filters>{filters.filterNot(QvStore.DefaultFilters.contains(_)).map(format)}</filters>
      <axes>{axes.filterNot((QvStore.DefaultAxes ++ Axis.Dynamics).contains(_)).map(format)}</axes>
      <histograms>{histograms.filterNot(QvStore.DefaultHistograms.contains(_)).map(_.toXml)}</histograms>
      <tables>{tables.filterNot(QvStore.DefaultTables.contains(_)).map(_.toXml)}</tables>
      <barcharts>{barCharts.filterNot(QvStore.DefaultBarCharts.contains(_)).map(_.toXml)}</barcharts>
    </qvTool>

  def format(filterSet: FilterSet): Node = {
    <filter>
      <name>{filterSet.label}</name>
      <filterset>
        { filterSet.filters.map(format) }
      </filterset>
    </filter>
  }

  def format(axis: Axis): Node = {
    <axis>
      <name>{axis.label}</name>
      <filtergroups>
        { axis.groups.map(format) }
      </filtergroups>
    </axis>
  }

  def format(input: Filter): Node = {
    // Functions to create inner nodes for standardization and easy maintainability.
    def makeSetNode[A](valueSet: Set[A]) = <set>{valueSet.map(x => <elem>{x.toString}</elem>)}</set>
    def makeRangeNode[A](low: A, high: A) = <range><min>{low.toString}</min><max>{high.toString}</max></range>
    def makeEnumSetNode[A](valueSet: Set[A]) = <set>{valueSet.map(x => <elem>{x.asInstanceOf[Enum[_]].name}</elem>)}</set>

    input match {

      case RA(min, max)  => <rafilter>{makeRangeNode(min, max)}</rafilter>
      case Dec(min, max) => <decfilter>{makeRangeNode(min, max)}</decfilter>

      case HasNonSidereal(value) => <isnonsidereal><boolvalue>{toBoolean(value)}</boolvalue></isnonsidereal>
      case IsActive(value) => <isactive><boolvalue>{toBoolean(value)}</boolvalue></isactive>
      case IsCompleted(value) => <iscompleted><boolvalue>{toBoolean(value)}</boolvalue></iscompleted>
      case IsRollover(value) => <rolloverfilter><boolvalue>{toBoolean(value)}</boolvalue></rolloverfilter>
      case HasTimingConstraints(value) => <timingConstraints><boolvalue>{toBoolean(value)}</boolvalue></timingConstraints>
      case HasElevationConstraints(value) => <elevationConstraints><boolvalue>{toBoolean(value)}</boolvalue></elevationConstraints>
      case HasPreImaging(value) => <preImaging><boolvalue>{toBoolean(value)}</boolvalue></preImaging>
      case HasDummyTarget(value) => <dummyTarget><boolvalue>{toBoolean(value)}</boolvalue></dummyTarget>

      case ProgId(value) => <progidfilter><id>{value}</id></progidfilter>
      case ProgPi(value) => <progpifilter><id>{value}</id></progpifilter>
      case ProgContact(value) => <progcontactfilter><id>{value}</id></progcontactfilter>
      case ObsId(value) => <obsidfilter><id>{value}</id></obsidfilter>

      case RemainingNights(_, min, max, enabled, cur, next) => <remnightsfilter>{makeRangeNode(min, max)}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remnightsfilter>
      case RemainingHours(_, min, max, enabled, cur, next) => <remhoursfilter>{makeRangeNode(min, max)}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remhoursfilter>
      case RemainingHoursFraction(_, min, max, enabled, cur, next) => <remhoursfracfilter>{makeRangeNode(min, max)}<enabled>{enabled}</enabled><cur>{cur}</cur><next>{next}</next></remhoursfracfilter>
      case SetTime(_, min, max) => <settimefilter>{makeRangeNode(min, max)}</settimefilter>

      case EnumFilter("Semesters", _, _, selection, _)           => <semesterfilter>{makeSetNode(selection)}</semesterfilter>
      case EnumFilter("Partner", _, _, selection, _)           => <partnerfilter>{makeSetNode(selection.map(toPartner))}</partnerfilter>
      case EnumFilter("AO", _, _, selection, _)           => <aofilter>{makeSetNode(selection)}</aofilter>
      case EnumFilter("Band", _, _, selection, _)           => <bandfilter>{makeEnumSetNode(selection)}</bandfilter>
      case EnumFilter("Instruments", _, _, selection, _)    => <instrumentfilter>{makeEnumSetNode(selection)}</instrumentfilter>
      case EnumFilter("Priority", _, _, selection, _)       => <priorityfilter>{makeEnumSetNode(selection)}</priorityfilter>
      case EnumFilter("Image Quality", _, _, selection, _)  => <iqfilter>{makeEnumSetNode(selection)}</iqfilter>
      case EnumFilter("Cloud Cover", _, _, selection, _)    => <ccfilter>{makeEnumSetNode(selection)}</ccfilter>
      case EnumFilter("Water Vapor", _, _, selection, _)    => <wvfilter>{makeEnumSetNode(selection)}</wvfilter>
      case EnumFilter("Sky Background", _, _, selection, _) => <sbfilter>{makeEnumSetNode(selection)}</sbfilter>
      case EnumFilter("TOO Type", _, _, selection, _)       => <toofilter>{makeEnumSetNode(selection)}</toofilter>
      case EnumFilter("Program Types", _, _, selection, _)       => <typefilter>{makeSetNode(selection)}</typefilter>
      case EnumFilter("Observation Status", _, _, selection, _)       => <obsStatus>{makeEnumSetNode(selection)}</obsStatus>
      case EnumFilter("Observation Class", _, _, selection, _)       => <obsClass>{makeEnumSetNode(selection)}</obsClass>

      // GMOSN
      // *** NOTE: Bizarrely, if we use InstGmosNorth.SP_TYPE instead of the exact same value SPComponentType.Instrument_GMOS,
      // we get "warning: unreachable code". This can be remedied instead by changing this to:
      // case ConfigurationFilter(_, tp, "Dispersers", _, selection, _) if tp == InstGmosNorth.SP_TYPE
      // as well, but this solution is not particularly elegant either.
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOS, "Dispersers", _, selection, _)  => <gmosndisp>{makeEnumSetNode(selection)}</gmosndisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOS, "Filters", _, selection, _)     => <gmosnfilt>{makeEnumSetNode(selection)}</gmosnfilt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOS, "Focal Planes", _, selection, _)     => <gmosnfocplane>{makeEnumSetNode(selection)}</gmosnfocplane>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOS, "CCD", _, selection, _)     => <gmosnccd>{makeEnumSetNode(selection)}</gmosnccd>

      // GMOSS
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOSSOUTH, "Dispersers", _, selection, _) => <gmossdisp>{makeEnumSetNode(selection)}</gmossdisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOSSOUTH, "Filters", _, selection, _)    => <gmossfilt>{makeEnumSetNode(selection)}</gmossfilt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOSSOUTH, "Focal Planes", _, selection, _)    => <gmossfocplane>{makeEnumSetNode(selection)}</gmossfocplane>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GMOSSOUTH, "CCD", _, selection, _)    => <gmossccd>{makeEnumSetNode(selection)}</gmossccd>

      // GNIRS
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GNIRS, "Dispersers", _, selection, _)       => <gnirsdisp>{makeEnumSetNode(selection)}</gnirsdisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GNIRS, "Filters", _, selection, _)       => <gnirsfilt>{makeEnumSetNode(selection)}</gnirsfilt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GNIRS, "Cross Dispersers", _, selection, _) => <gnirscrossdisp>{makeEnumSetNode(selection)}</gnirscrossdisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GNIRS, "Cameras", _, selection, _)          => <gnirscam>{makeEnumSetNode(selection)}</gnirscam>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GNIRS, "Focal Planes", _, selection, _)     => <gnirsfocplane>{makeEnumSetNode(selection)}</gnirsfocplane>

      // GSAOI
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_GSAOI, "Filters", _, selection, _)   => <gsaoifilt>{makeEnumSetNode(selection)}</gsaoifilt>

      // F2
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_FLAMINGOS2, "Dispersers", _, selection, _)   => <f2disp>{makeEnumSetNode(selection)}</f2disp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_FLAMINGOS2, "Filters", _, selection, _)   => <f2filt>{makeEnumSetNode(selection)}</f2filt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_FLAMINGOS2, "Focal Planes", _, selection, _) => <f2focplane>{makeEnumSetNode(selection)}</f2focplane>

      // NIFS
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIFS, "Dispersers", _, selection, _) => <nifsdisp>{makeEnumSetNode(selection)}</nifsdisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIFS, "Filters", _, selection, _)    => <nifsfilt>{makeEnumSetNode(selection)}</nifsfilt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIFS, "Masks", _, selection, _)      => <nifsmask>{makeEnumSetNode(selection)}</nifsmask>

      // NICI
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NICI, "Focal Planes", _, selection, _) => <nicifocplane>{makeEnumSetNode(selection)}</nicifocplane>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NICI, "Dichroic Wheel", _, selection, _)    => <niciwheel>{makeEnumSetNode(selection)}</niciwheel>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NICI, "Filter Red Channel", _, selection, _)    => <nicired>{makeEnumSetNode(selection)}</nicired>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NICI, "Filter Blue Channel", _, selection, _)      => <niciblue>{makeEnumSetNode(selection)}</niciblue>

      // NIRI
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIRI, "Dispersers", _, selection, _) => <niridisp>{makeEnumSetNode(selection)}</niridisp>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIRI, "Filters", _, selection, _)    => <nirifilt>{makeEnumSetNode(selection)}</nirifilt>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIRI, "Cameras", _, selection, _)    => <niricam>{makeEnumSetNode(selection)}</niricam>
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_NIRI, "Masks", _, selection, _)      => <nirimask>{makeEnumSetNode(selection)}</nirimask>

      // TEXES
      case ConfigurationFilter(_, SPComponentType.INSTRUMENT_TEXES, "Dispersers", _, selection, _) => <texesdisp>{makeEnumSetNode(selection)}</texesdisp>

      case EmptyFilter(name) => <filterall><name>{name}</name></filterall>
      case FilterOr(a,b)  => <or><first>{FilterXMLFormatter.format(a)}</first><second>{FilterXMLFormatter.format(b)}</second></or>
      case FilterAnd(a,b) => <and><first>{FilterXMLFormatter.format(a)}</first><second>{FilterXMLFormatter.format(b)}</second></and>

      // fail
      case f => throw new IllegalArgumentException("did not recognize filter " + f)
    }
  }

  def toBoolean(value: Option[Boolean]): String =
    value match {
      case None => "none"
      case Some(true) => "true"
      case Some(false) => "false"
    }

  def toPartner(partner: Any): String =
    partner match {
      case Partner(None) => "none"
      case Partner(Some(affiliate)) => affiliate.toString
      case _ => throw new IllegalArgumentException("unexpected partner")
    }
}