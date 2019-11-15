package edu.gemini.spModel.gemini.ghost

import java.beans.PropertyDescriptor
import java.util.logging.Logger
import java.util.{Collections, List => JList, Map => JMap, Set => JSet}

import edu.gemini.pot.sp._
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.{ComponentNodeInitializer, ObservationNI}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obscomp.{InstConfigInfo, InstConstants, SPInstObsComp}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider}

import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


/** The GHOST instrument SP model.
  * Note that we do not override clone since private variables are immutable.
  */
final class Ghost extends SPInstObsComp(GhostMixin.SP_TYPE) with PropertyProvider with GhostMixin with IssPortProvider {
  private var redExposureTime: Double = Ghost.DEF_EXPOSURE_TIME_RED
  private var blueExposureTime: Double = Ghost.DEF_EXPOSURE_TIME_BLUE

  override def getSite: JSet[Site] = {
    Site.SET_GS
  }

  override def getPhaseIResourceName: String = {
    "gemGHOST"
  }

  override def getProperties: JMap[String, PropertyDescriptor] = {
    Ghost.PropertyMap
  }

  /**
   * Paramset methods.
   * Note that we do not call the method on the superclass, SpInstObsComp, because unlike other instruments, GHOST
   * does not have a single exposure time: it instead has two exposure times, red and blue.
   */
  override def getParamSet(factory: PioFactory): ParamSet = {
    val paramSet = factory.createParamSet(getType.readableStr)
    paramSet.setKind(ISPDataObject.PARAM_SET_KIND)

    // Only write the title as a property if it has been changed.
    if (isValidTitle) Pio.addParam(factory, paramSet, ISPDataObject.TITLE_PROP, getTitle)

    Pio.addParam(factory, paramSet, InstConstants.POS_ANGLE_PROP, getPosAngleDegreesStr)
    Pio.addParam(factory, paramSet, Ghost.EXPOSURE_TIME_RED_PROP, getRedExposureTimeAsString)
    Pio.addParam(factory, paramSet, Ghost.EXPOSURE_TIME_BLUE_PROP, getBlueExposureTimeAsString)
    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    Option(Pio.getValue(paramSet, Ghost.PORT_PROP)).map(IssPort.valueOf).foreach(setIssPort)
    Option(Pio.getValue(paramSet, ISPDataObject.TITLE_PROP)).foreach(setTitle)
    Option(Pio.getValue(paramSet, Ghost.EXPOSURE_TIME_RED_PROP)).foreach(setExposureTimeAsString)
    Option(Pio.getValue(paramSet, InstConstants.POS_ANGLE_PROP)).map(_.toDouble).foreach(setPosAngleDegrees)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(Ghost.POS_ANGLE_PROP, getPosAngle))
    sc.putParameter(DefaultParameter.getInstance(Ghost.PORT_PROP, getIssPort))
    sc.putParameter(DefaultParameter.getInstance(Ghost.EXPOSURE_TIME_RED_PROP, getRedExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Ghost.EXPOSURE_TIME_BLUE_PROP, getBlueExposureTime))
    sc
  }

  /**
   * ISS Port
   */
  private var port: IssPort = IssPort.DEFAULT
  override def getIssPort: IssPort = port
  override def setIssPort(newValue: IssPort): Unit = {
    val oldValue = getIssPort
    if (oldValue != newValue) {
      port = newValue
      firePropertyChange(Ghost.PORT_PROP, oldValue, newValue)
    }
  }

  def getRedExposureTime: Double = redExposureTime
  def getRedExposureTimeAsString: String = redExposureTime.toString
  def getBlueExposureTime: Double = blueExposureTime
  def getBlueExposureTimeAsString: String = blueExposureTime.toString

  def setRedExposureTime(newValue: Double): Unit = {
    val oldValue = getRedExposureTime
    if (oldValue != newValue) {
      redExposureTime = newValue
      firePropertyChange(Ghost.EXPOSURE_TIME_RED_PROP, oldValue, newValue)
    }
  }

  def setRedExposureTimeAsString(newValue: String): Unit =
    setRedExposureTime(newValue.toDouble)

  def setBlueExposureTime(newValue: Double): Unit = {
    val oldValue = getBlueExposureTime
    if (oldValue != newValue) {
      blueExposureTime = newValue
      firePropertyChange(Ghost.EXPOSURE_TIME_BLUE_PROP, oldValue, newValue)
    }
  }

  def setBlueExposureTimeAsString(newValue: String): Unit =
    setBlueExposureTime(newValue.toDouble)

  /**
   * Unsupported operations: GHOST has two exposure times, red and blue, and not a single exposure time like other
   * instruments do.
   */
  override def getExposureTime: Double = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def getExposureTimeAsString: String = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def setExposureTime(newValue: Double): Unit = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def setExposureTimeAsString(newValue: String): Unit = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }
}

object Ghost {
  val LOG: Logger = Logger.getLogger(classOf[Ghost].getName)

  // Unfortunately we need a Java "Supplier" and "Function" which makes it
  // awkward to create the NodeInitializer via ComponentNodeInitializer.
  private val GhostSupplier: java.util.function.Supplier[Ghost] =
    new java.util.function.Supplier[Ghost] {
      def get(): Ghost = new Ghost()
    }

  private val GhostCbFactory: java.util.function.Function[ISPObsComponent, GhostCB] =
    new java.util.function.Function[ISPObsComponent, GhostCB] {
      def apply(oc: ISPObsComponent): GhostCB = new GhostCB(oc)
    }

  val NI: ISPNodeInitializer[ISPObsComponent, Ghost] =
    new ComponentNodeInitializer(SPComponentType.INSTRUMENT_GHOST, GhostSupplier, GhostCbFactory)

  val OBSERVATION_NI: ISPNodeInitializer[ISPObservation, SPObservation] = new ObservationNI(Instrument.Ghost.some()) {
    override protected def addTargetEnv(factory: ISPFactory, obsNode: ISPObservation): Unit = {
      Try {
        val p   = obsNode.getProgram
        val oc  = factory.createObsComponent(p, TargetObsComp.SP_TYPE, null)
        val toc = oc.getDataObject.asInstanceOf[TargetObsComp]

        // Create a single target GHOST asterism as the default.
        val a   = GhostAsterism.createEmptySingleTargetAsterism
        val env = TargetEnvironment.create(a)

        toc.setTargetEnvironment(env)
        oc.setDataObject(toc)
        obsNode.addObsComponent(oc)
      } match {
        case Success(_)               =>
          // Do nothing.
        case Failure(ex: SPException) =>
          throw new RuntimeException("Unable to create and initialize GHOST target environment", ex)
        case Failure(_)               =>
          // This should never happen.
          throw new RuntimeException("Unknown failure in creating GHOST target environment")
      }
    }
  }

  // The name of the Ghost instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "GHOST"

  // GHOST-specific exposure times.
  val EXPOSURE_TIME_RED_PROP = "exposureTimeRed"
  val DEF_EXPOSURE_TIME_RED = 10.0
  val EXPOSURE_TIME_RED_KEY = new ItemKey(INSTRUMENT_KEY, EXPOSURE_TIME_RED_PROP)

  val EXPOSURE_TIME_BLUE_PROP = "exposureTimeBlue"
  val DEF_EXPOSURE_TIME_BLUE = 10.0
  val EXPOSURE_TIME_BLUE_KEY = new ItemKey(INSTRUMENT_KEY, EXPOSURE_TIME_BLUE_PROP)

  // The names of the base position / IFUs.
  val BaseRADegrees: String  = "baseRADeg"
  val BaseRAHMS: String      = "baseRAHMS"
  val BaseDecDegrees: String = "baseDecDeg"
  val BaseDecDMS: String     = "baseDecDMS"

  val SRIFU1Name: String     = "srifu1Name"
  val SRIFU1RADeg: String    = "srifu1CoordsRADeg"
  val SRIFU1DecDeg: String   = "srifu1CoordsDecDeg"
  val SRIFU1RAHMS: String    = "srifu1CoordsRAHMS"
  val SRIFU1DecDMS: String   = "srifu1CoordsDecDMS"

  val SRIFU2Name: String     = "srifu2Name"
  val SRIFU2RADeg: String    = "srifu2CoordsRADeg"
  val SRIFU2DecDeg: String   = "srifu2CoordsDecDeg"
  val SRIFU2RAHMS: String    = "srifu2CoordsRAHMS"
  val SRIFU2DecDMS: String   = "srifu2CoordsDecDMS"

  val HRIFU1Name: String     = "hrifu1Name"
  val HRIFU1RADeg: String    = "hrifu1CoordsRADeg"
  val HRIFU1DecDeg: String   = "hrifu1CoordsDecDeg"
  val HRIFU1RAHMS: String    = "hrifu1CoordsRAHMS"
  val HRIFU1DecDMS: String   = "hrifu1CoordsDecDMS"

  val HRIFU2Name: String     = "hrifu2Name"
  val HRIFU2RADeg: String    = "hrifu2CoordsRADeg"
  val HRIFU2DecDeg: String   = "hrifu2CoordsDecDeg"
  val HRIFU2RAHMS: String    = "hrifu2CoordsRAHMS"
  val HRIFU2DecDMS: String   = "hrifu2CoordsDecDMS"

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor = {
    PropertySupport.init(propName, classOf[Ghost], query, iter)
  }

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  val POS_ANGLE_PROP: PropertyDescriptor = initProp(InstConstants.POS_ANGLE_PROP, query = query_no, iter = iter_no)
  val PORT_PROP: PropertyDescriptor = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no)

  // Use Java classes to be compatible with existing instruments.
  private val Properties: List[(String, PropertyDescriptor)] = List(
    POS_ANGLE_PROP.getName -> POS_ANGLE_PROP
  )

  private[ghost] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }

  /** Currently, the instrument has no queryable configuration parameters. */
  val getInstConfigInfo: JList[InstConfigInfo] = List.empty[InstConfigInfo].asJava

  val GhostExposureTimeErrorMessage: String = "Error: tried to access single exposure time for GHOST."
}