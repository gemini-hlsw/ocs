package edu.gemini.spModel.gemini.igrins2

import edu.gemini.pot.sp.{ISPNodeInitializer, ISPObsComponent, ISPObservation, SPComponentType}
import edu.gemini.shared.util.immutable
import edu.gemini.shared.util.immutable.{DefaultImList, ImList}
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Site, Wavelength}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}

import java.util.{Collections, Map => JMap, Set => JSet}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer
import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider, PosAngleConstraint, PosAngleConstraintAware}
import squants.time.Time
import squants.time.TimeConversions.TimeConversions

import java.beans.PropertyDescriptor
import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import scala.math.sqrt

/*
 ** The Igrins2 instrument SP model.
 * Note that we do not override clone since private variables are immutable.
 */
final class Igrins2 extends ParallacticAngleSupportInst(Igrins2.SP_TYPE) with PropertyProvider with PosAngleConstraintAware with Igrins2Mixin with IssPortProvider {
  _exposureTime = Igrins2.DefaultExposureTime.toSeconds
  private var _port = IssPort.UP_LOOKING
  private var _posAngleConstraint = PosAngleConstraint.PARALLACTIC_ANGLE
  private var _slitViewingCamera: SlitViewingCamera = SlitViewingCamera.DEFAULT

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
    Pio.addParam(factory, paramSet, Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName(), getPosAngleConstraint().name());
    Pio.addParam(factory, paramSet, Igrins2.PORT_PROP.getName, getIssPort.name)
    Pio.addParam(factory, paramSet, Igrins2.SLIT_VIEWING_PROP.getName, getSlitViewingCamera.name)

    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    // The parent takes care of exposure and posangle
    super.setParamSet(paramSet)
    Option(Pio.getValue(paramSet, Igrins2.POS_ANGLE_CONSTRAINT_PROP))
      .flatMap(v => Option(PosAngleConstraint.valueOf(v)))
      .foreach(_setPosAngleConstraint)
    Option(Pio.getValue(paramSet, Igrins2.PORT_PROP.getName)).foreach(_setPort)
    Option(Pio.getValue(paramSet, Igrins2.SLIT_VIEWING_PROP))
      .flatMap(v => Option(SlitViewingCamera.valueOf(v)))
      .foreach(_setSlitViewingCamera)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(InstConstants.POS_ANGLE_PROP, getPosAngleDegrees))
    sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.POS_ANGLE_CONSTRAINT_PROP, getPosAngleConstraint))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.PORT_PROP, getIssPort))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.SLIT_VIEWING_PROP, getSlitViewingCamera))
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
   * Get the GMOS Port
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

  def getSlitViewingCamera: SlitViewingCamera = _slitViewingCamera

  private def _setSlitViewingCamera(value: SlitViewingCamera): Unit =
    _slitViewingCamera = value

  def setSlitViewingCamera(newValue: SlitViewingCamera): Unit = {
    val oldValue = getSlitViewingCamera
    if (oldValue != newValue) {
      _slitViewingCamera = newValue
      firePropertyChange(Igrins2.SLIT_VIEWING_PROP.getName, oldValue, newValue)
    }
    _slitViewingCamera = newValue
  }
}

object Igrins2 {
  val SP_TYPE: SPComponentType = SPComponentType.INSTRUMENT_IGNRIS2
  val DefaultExposureTime: Time = 30.seconds // sec (by default settings)

  val WavelengthCoverageLowerBound: Wavelength = Wavelength.fromMicrons(1.49)
  val WavelengthCoverageUpperBound: Wavelength = Wavelength.fromMicrons(2.46)

  val AllowedFowlerSamples: List[Int] = List(1, 2, 4, 8, 16)

  def fowlerSamples(expTime: Time): Int = {
    val nFowler = ((expTime.toSeconds - 0.168)/1.45479).toInt
    AllowedFowlerSamples.minBy(cur => math.abs (cur - nFowler))
  }

  def readNoise(expTime: Time): List[(MagnitudeBand, Double)] = {
    val fowlerSamples0 = fowlerSamples(expTime)
    def rn(rnAt16: Double) = rnAt16 * sqrt(16) / sqrt(fowlerSamples0)
    List((MagnitudeBand.H, rn(3.8)), (MagnitudeBand.K, rn(5.0)))
  }

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor =
    PropertySupport.init(propName, classOf[Igrins2], query, iter)

  // The name of the Igrins2 instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "IGRINS2"
  val SLIT_VIEWING_CAMERA: String = "slitViewingCamera"

  val POS_ANGLE_CONSTRAINT_PROP: PropertyDescriptor = initProp("posAngleConstraint", query = query_no, iter = iter_no)
  val EXPOSURE_TIME_PROP: PropertyDescriptor = initProp(InstConstants.EXPOSURE_TIME_PROP, query = query_no, iter = iter_yes)
  var PORT_PROP: PropertyDescriptor = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no)
  var SLIT_VIEWING_PROP: PropertyDescriptor = initProp(SLIT_VIEWING_CAMERA, query_yes, iter_no)

  private val Igrins2Supplier: java.util.function.Supplier[Igrins2] =
    new java.util.function.Supplier[Igrins2] {
      def get(): Igrins2 = new Igrins2()
    }

  private val Igrins2CbFactory: java.util.function.Function[ISPObsComponent, Igrins2CB] =
    new java.util.function.Function[ISPObsComponent, Igrins2CB] {
      def apply(oc: ISPObsComponent): Igrins2CB = new Igrins2CB(oc)
    }

  val NI: ISPNodeInitializer[ISPObsComponent, Igrins2] =
    new ComponentNodeInitializer(SPComponentType.INSTRUMENT_IGNRIS2, Igrins2Supplier, Igrins2CbFactory)

  private val Properties: List[(String, PropertyDescriptor)] = List[PropertyDescriptor](
    POS_ANGLE_CONSTRAINT_PROP,
    EXPOSURE_TIME_PROP,
    PORT_PROP,
    SLIT_VIEWING_PROP
  ).map(p => (p.getName, p))

  private[igrins2] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }
}
