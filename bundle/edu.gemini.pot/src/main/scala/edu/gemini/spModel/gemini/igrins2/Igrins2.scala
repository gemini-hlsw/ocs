package edu.gemini.spModel.gemini.igrins2

import edu.gemini.pot.sp.{ISPNodeInitializer, ISPObsComponent, ISPObservation, SPComponentType}
import edu.gemini.shared.util.immutable
import edu.gemini.shared.util.immutable.{DefaultImList, ImList}
import edu.gemini.skycalc.{Angle => SkyAngle}
import edu.gemini.spModel.config.ConfigPostProcessor
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Site, Wavelength}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}

import java.util.{Collections, List => JList, Map => JMap, Set => JSet}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyIgrins2
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer
import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst
import edu.gemini.spModel.inst.{ScienceAreaGeometry, VignettableScienceAreaInstrument}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obs.plannedtime.{CommonStepCalculator, ExposureCalculator, PlannedTime}
import edu.gemini.spModel.obs.plannedtime.PlannedTime.{CategorizedTime, Category}
import edu.gemini.spModel.obscomp.{InstConfigInfo, InstConstants, ItcOverheadProvider}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider, PosAngleConstraint, PosAngleConstraintAware}
import squants.time.Time
import squants.time.TimeConversions.TimeConversions

import java.beans.PropertyDescriptor
import java.time.Duration
import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._

/*
 ** The Igrins2 instrument SP model.
 * Note that we do not override clone since private variables are immutable.
 */
final class Igrins2 extends ParallacticAngleSupportInst(Igrins2.SP_TYPE)
  with PropertyProvider
  with PosAngleConstraintAware
  with Igrins2Mixin
  with IssPortProvider
  with PlannedTime.StepCalculator
  with ConfigPostProcessor
  with ItcOverheadProvider
  with CalibrationKeyProvider
  with VignettableScienceAreaInstrument {
  _exposureTime = Igrins2.DefaultExposureTime.toSeconds
  private var _port = IssPort.UP_LOOKING
  private var _posAngleConstraint = PosAngleConstraint.PARALLACTIC_ANGLE

  /**
   * Get the site in which the instrument resides.
   *
   * @return the site of the instrument.
   */
  override def getSite: JSet[Site] = Site.SET_GN

  /**
   * Gets the name of the Phase I resource representing this instrument.
   * This can be used in the Phase I to Phase II conversion process to
   * associate the instrument component with the phase I resource name.
   */
  override def getPhaseIResourceName: String = "gemIGRINS2"

  override def getProperties: JMap[String, PropertyDescriptor] =
    Igrins2.PropertyMap

  override def getParamSet(factory: PioFactory): ParamSet = {
    // The parent takes care of exposure and posangle
    val paramSet = super.getParamSet(factory)
    Pio.addParam(factory, paramSet, Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName, getPosAngleConstraint().name())
    Pio.addParam(factory, paramSet, Igrins2.PORT_PROP.getName, getIssPort.name)

    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    // The parent takes care of exposure and posangle
    super.setParamSet(paramSet)
    Option(Pio.getValue(paramSet, Igrins2.POS_ANGLE_CONSTRAINT_PROP))
      .flatMap(v => Option(PosAngleConstraint.valueOf(v)))
      .foreach(_setPosAngleConstraint)
    Option(Pio.getValue(paramSet, Igrins2.PORT_PROP.getName)).foreach(_setPort)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(InstConstants.POS_ANGLE_PROP, getPosAngleDegrees))
    sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.POS_ANGLE_CONSTRAINT_PROP, getPosAngleConstraint))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.PORT_PROP, getIssPort))
    sc
  }

  override def getPosAngleConstraint: PosAngleConstraint =
    if (_posAngleConstraint == null) PosAngleConstraint.PARALLACTIC_ANGLE else _posAngleConstraint

  override def setPosAngleConstraint(newValue: PosAngleConstraint): Unit = {
    val oldValue = getPosAngleConstraint
    if (!(oldValue == newValue)) {
      _posAngleConstraint = newValue
      firePropertyChange(Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName, oldValue, newValue)
    }
  }

  private def _setPosAngleConstraint(newValue: PosAngleConstraint): Unit =
    _posAngleConstraint = newValue

  /**
   * Get the Port
   */
  override def getIssPort: IssPort = {
    if (_port == null) _port = IssPort.DEFAULT
    _port
  }

  /**
   * Set the Port.
   */
  override def setIssPort(newValue: IssPort): Unit = {
    val oldValue = getIssPort
    if (oldValue != newValue) {
      _port = newValue
      firePropertyChange(Igrins2.PORT_PROP.getName, oldValue, newValue)
    }
  }

  /**
   * Set the Port with a String.
   */
  private def _setPort(name: String): Unit = {
    val oldValue = getIssPort
    setIssPort(IssPort.getPort(name, oldValue))
  }

  /**
   * The list of valid position angle constraints for this instrument.
   *
   * @return a list of the supported position angle constraints.
   */
  override def getSupportedPosAngleConstraints: ImList[PosAngleConstraint] = {
    DefaultImList.create(
      PosAngleConstraint.FIXED,
      PosAngleConstraint.PARALLACTIC_ANGLE,
      PosAngleConstraint.PARALLACTIC_OVERRIDE)
  }

  /**
   * Return true if this instrument and its configuration allows calculation of best position angle,
   * and false otherwise.
   */
  override def allowUnboundedPositionAngle(): Boolean = false

  /**
   * Perform the parallactic angle computation for the observation.
   */
  override def calculateParallacticAngle(obs: ISPObservation): immutable.Option[Angle] =
    super.calculateParallacticAngle(obs)

  override def getVignettableScienceArea: ScienceAreaGeometry =
    Igrins2ScienceAreaGeometry

  override def pwfs1VignettingClearance(ctx: ObsContext): SkyAngle =
    edu.gemini.skycalc.Angle.arcmins(5.0)

  override def pwfs2VignettingClearance(ctx: ObsContext): SkyAngle =
    edu.gemini.skycalc.Angle.arcmins(4.75)

  override def getSetupTime(obs: ISPObservation): Duration =
    Duration.ofMinutes(7) // 420 s as indicated in REL-4531

  override def getSetupTime(config: Config): Duration =
    Duration.ofSeconds(Igrins2.SetupTime.toSeconds.toLong)

  override def getReacquisitionTime(config: Config): Duration =
    Duration.ofMinutes(5)

  override def calc(cur: Config, prev: immutable.Option[Config]): PlannedTime.CategorizedTimeGroup = {
    val times = new java.util.ArrayList[CategorizedTime]()

    val rawExposureTime = ExposureCalculator.instance.exposureTimeSec(cur)

    times.add(CategorizedTime.fromSeconds(
      Category.EXPOSURE,
      rawExposureTime
    ))

    val readoutTime = Igrins2.readoutTime(rawExposureTime.seconds)

    times.add(CategorizedTime.fromSeconds(
      Category.READOUT,
      readoutTime.toSeconds
    ))

    times.add(getDhsWriteTime)

    CommonStepCalculator.instance.calc(cur, prev).addAll(times)
  }

  override def postProcessSequence(in: ConfigSequence): ConfigSequence = {
    val configs = in.getAllSteps
    for (c <- configs) {
      Option(c.getItemValue(InstConstants.EXPOSURE_TIME_KEY)).foreach {
        case expTime: java.lang.Double =>
          c.putItem(Igrins2.FOWLER_SAMPLES_KEY, Igrins2.fowlerSamples(expTime.doubleValue().seconds))
        case _ =>
      }
    }
    new ConfigSequence(configs)
  }

  override def extractKey(instrumentConfig: ISysConfig): CalibrationKey =
    new CalibrationKeyImpl(ConfigKeyIgrins2.INSTANCE)
}

object Igrins2 {
  val SP_TYPE: SPComponentType = SPComponentType.INSTRUMENT_IGRINS2
  val SetupTime: Time = 7.minutes // REL-4531
  val DefaultExposureTime: Time = 30.seconds // sec (by default settings)

  val MinExposureTime: Time = 3.08.seconds  // REL-4667
  val MaxExposureTime: Time = 600.seconds

  val WavelengthCoverageLowerBound: Wavelength = Wavelength.fromMicrons(1.49)
  val WavelengthCoverageUpperBound: Wavelength = Wavelength.fromMicrons(2.46)

  val AllowedFowlerSamples: List[Int] = List(1, 2, 4, 8, 16)

  // REL-4531 and updated in REL-4667
  val FowlerSamplesReadoutTime: Map[Int, Time] =
    Map(1 -> 8.5.seconds, 2 -> 11.3.seconds, 4 -> 16.0.seconds, 8 -> 24.0.seconds, 16 -> 39.5.seconds)

  assert(AllowedFowlerSamples.forall(FowlerSamplesReadoutTime.contains))

  def fowlerSamples(expTime: Time): Int = {
    val nFowler = ((expTime.toSeconds + 1.45479 - 0.168)/1.45479).toInt  // REL-4667
    AllowedFowlerSamples.reverse.find(cur => nFowler >= cur).getOrElse(AllowedFowlerSamples.head)
  }

  def fowlerSamples(expTime: Double) : Int = {
    fowlerSamples(expTime.seconds)
  }

  def readoutTime(expTime: Time): Time = {
    val nFowler = fowlerSamples(expTime)
    FowlerSamplesReadoutTime.getOrElse(nFowler, sys.error(s"Unsupported fowler samples value $nFowler"))
  }

  // REL-4644
  // Fowler samples: {1, 2, 4, 8, 16}
  // H detector read noise: {17.4, 12.5, 9.2, 6.8, 5.1}
  // K detector read noise: {23.7, 16.9, 12.2, 8.9, 6.7}
  def ReadNoise(band: MagnitudeBand, fowlerSamples: Int): Double = {
    (band, fowlerSamples) match {
      case (MagnitudeBand.H, 1) => 17.4
      case (MagnitudeBand.H, 2) => 12.5
      case (MagnitudeBand.H, 4) => 9.2
      case (MagnitudeBand.H, 8) => 6.8
      case (MagnitudeBand.H, 16) => 5.1
      case (MagnitudeBand.K, 1) => 23.7
      case (MagnitudeBand.K, 2) => 16.9
      case (MagnitudeBand.K, 4) => 12.2
      case (MagnitudeBand.K, 8) => 8.9
      case (MagnitudeBand.K, 16) => 6.7
    }
  }

  def readNoise(expTime: Time): List[(MagnitudeBand, Double)] = {
    val fowlerSamples0 = fowlerSamples(expTime)
    List(
      (MagnitudeBand.H, ReadNoise(MagnitudeBand.H, fowlerSamples0)),
      (MagnitudeBand.K, ReadNoise(MagnitudeBand.K, fowlerSamples0)))
  }

  def readNoise(expTime: Double, band: MagnitudeBand) : Double = {
    band match {
      case MagnitudeBand.H => ReadNoise(MagnitudeBand.H, fowlerSamples(expTime))
      case MagnitudeBand.K => ReadNoise(MagnitudeBand.K, fowlerSamples(expTime))
    }
  }

  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor =
    PropertySupport.init(propName, classOf[Igrins2], query, iter)

  // The name of the Igrins2 instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "IGRINS2"

  val POS_ANGLE_CONSTRAINT_PROP: PropertyDescriptor = initProp("posAngleConstraint", query = query_no, iter = iter_no)
  val EXPOSURE_TIME_PROP: PropertyDescriptor = initProp(InstConstants.EXPOSURE_TIME_PROP, query = query_no, iter = iter_yes)
  val PORT_PROP: PropertyDescriptor = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no)

  val FOWLER_SAMPLES_KEY =  new ItemKey("instrument:fowlerSamples")

  private val Igrins2Supplier: java.util.function.Supplier[Igrins2] =
    new java.util.function.Supplier[Igrins2] {
      def get(): Igrins2 = new Igrins2()
    }

  private val Igrins2CbFactory: java.util.function.Function[ISPObsComponent, Igrins2CB] =
    new java.util.function.Function[ISPObsComponent, Igrins2CB] {
      def apply(oc: ISPObsComponent): Igrins2CB = new Igrins2CB(oc)
    }

  val NI: ISPNodeInitializer[ISPObsComponent, Igrins2] =
    new ComponentNodeInitializer(SPComponentType.INSTRUMENT_IGRINS2, Igrins2Supplier, Igrins2CbFactory)

  private val Properties: List[(String, PropertyDescriptor)] = List[PropertyDescriptor](
    POS_ANGLE_CONSTRAINT_PROP,
    EXPOSURE_TIME_PROP,
    PORT_PROP
  ).map(p => (p.getName, p))

  val getInstConfigInfo: JList[InstConfigInfo] =
    List[InstConfigInfo]().asJava

  private[igrins2] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }
}
