package edu.gemini.spModel.gemini.ghost

import java.util.{Map => JMap}

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.config.AbstractObsComponentCB
import edu.gemini.spModel.data.config._
import edu.gemini.spModel.obscomp.InstConstants

import scala.collection.JavaConverters._

/** Configuration builder for GHOST.
  */
final class GhostCB(obsComp: ISPObsComponent) extends AbstractObsComponentCB(obsComp) {
  @transient private var sysConfig: Option[ISysConfig] = None

  override def clone(): AnyRef = {
    val result = super.clone().asInstanceOf[GhostCB]
    result.sysConfig = None
    result
  }

  override def thisReset(options: JMap[String, Object]): Unit = {
    val dataObj: Ghost = getDataObject.asInstanceOf[Ghost]
    if (dataObj == null)
      throw new IllegalArgumentException("The data object for GHOST cannot be null")
    sysConfig = Some(dataObj.getSysConfig)
  }

  override protected def thisHasConfiguration(): Boolean = {
    sysConfig.exists(_.getParameterCount > 0)
  }

  override protected def thisApplyNext(config: IConfig, prevFull: IConfig): Unit = {
    sysConfig.foreach { sc =>
      val systemName: String = sc.getSystemName
      sc.getParameters.asScala.foreach { p =>
        config.putParameter(systemName, DefaultParameter.getInstance(p.getName, p.getValue))
      }
      config.putParameter(systemName,
        StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, Ghost.INSTRUMENT_NAME_PROP)
      )
    }
  }
}
