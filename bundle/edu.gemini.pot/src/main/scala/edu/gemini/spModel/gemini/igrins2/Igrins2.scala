package edu.gemini.spModel.gemini.igrins2

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Site

import java.util.{Collections, Map => JMap, Set => JSet}
import edu.gemini.spModel.data.property.PropertyProvider
import edu.gemini.spModel.obscomp.SPInstObsComp

import java.beans.PropertyDescriptor
import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._

/*
 ** The Igrins2 instrument SP model.
 * Note that we do not override clone since private variables are immutable.
 */
final class Igrins2 extends SPInstObsComp(Igrins2.SP_TYPE) with PropertyProvider {
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
}

object Igrins2 {
  val SP_TYPE: SPComponentType = SPComponentType.INSTRUMENT_IGNRIS2

  private val Properties: List[(String, PropertyDescriptor)] = List[PropertyDescriptor](
  ).map(p => (p.getName, p))

  private[igrins2] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }
}