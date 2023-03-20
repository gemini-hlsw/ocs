package edu.gemini.spModel.gemini.igrins2

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.config.AbstractObsComponentCB
import edu.gemini.spModel.data.config._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.target.{SPCoordinates, SPSkyObject, SPTarget}
import scalaz.Scalaz._

import java.time.Instant
import java.util.{Map => JMap}
import scala.collection.JavaConverters._

/**
  * Configuration builder for Igrins2
  */
final class Igrins2CB(obsComp: ISPObsComponent) extends AbstractObsComponentCB(obsComp) {

  @transient private var sysConfig: Option[ISysConfig] = None

  override def clone(): AnyRef = {
    val result = super.clone().asInstanceOf[Igrins2CB]
    result.sysConfig = None
    result
  }

  private def getIgrins2Component: Igrins2 =
    getDataObject.asInstanceOf[Igrins2]

  override def thisReset(options: JMap[String, Object]): Unit = {
    sysConfig = Some(getIgrins2Component.getSysConfig)
  }

  override protected def thisHasConfiguration(): Boolean =
    sysConfig.exists(_.getParameterCount > 0)

  override protected def thisApplyNext(config: IConfig, prevFull: IConfig): Unit = {
    sysConfig.foreach { sc =>
      val systemName: String = sc.getSystemName
      sc.getParameters.asScala.foreach { p =>
        config.putParameter(systemName, DefaultParameter.getInstance(p.getName, p.getValue))
      }
      config.putParameter(systemName,
        StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, Igrins2.INSTRUMENT_NAME_PROP)
      )

    }
  }
}

