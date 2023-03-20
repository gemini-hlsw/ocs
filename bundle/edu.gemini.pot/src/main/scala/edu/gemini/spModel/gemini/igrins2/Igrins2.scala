package edu.gemini.spModel.gemini.igrins2

import edu.gemini.pot.sp.{ISPNodeInitializer, ISPObsComponent, SPComponentType}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}

import java.util.{Collections, Map => JMap, Set => JSet}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer
import edu.gemini.spModel.obscomp.{InstConstants, SPInstObsComp}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames

import java.beans.PropertyDescriptor
import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._

/*
 ** The Igrins2 instrument SP model.
 * Note that we do not override clone since private variables are immutable.
 */
final class Igrins2 extends SPInstObsComp(Igrins2.SP_TYPE) with PropertyProvider with Igrins2Mixin {
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
    val paramSet = super.getParamSet(factory)
    Pio.addParam(factory, paramSet, InstConstants.POS_ANGLE_PROP, getPosAngleDegreesStr)
    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    Option(Pio.getValue(paramSet, InstConstants.POS_ANGLE_PROP)).map(_.toDouble).foreach(setPosAngleDegrees)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(Igrins2.POS_ANGLE_PROP, getPosAngle))
    sc
  }
}

object Igrins2 {
  val SP_TYPE: SPComponentType = SPComponentType.INSTRUMENT_IGNRIS2

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor = {
    PropertySupport.init(propName, classOf[Igrins2], query, iter)
  }
  // The name of the Ghost instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "IGRINS2"

  val POS_ANGLE_PROP: PropertyDescriptor = initProp(InstConstants.POS_ANGLE_PROP, query = query_no, iter = iter_no)

  // Unfortunately we need a Java "Supplier" and "Function" which makes it
  // awkward to create the NodeInitializer via ComponentNodeInitializer.
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
    POS_ANGLE_PROP
  ).map(p => (p.getName, p))

  private[igrins2] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }
}