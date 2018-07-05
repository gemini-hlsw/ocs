package edu.gemini.spModel.gemini.ghost

import java.util.{Map => JMap}

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.config.AbstractObsComponentCB
import edu.gemini.spModel.data.config._
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.target.SPSkyObject
import edu.gemini.spModel.target.obsComp.TargetObsComp

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

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

      // Put a coordinate parameter. Coordinates are not available here so we
      // must operate on an SPSkyObject.
      def coordParam(so: SPSkyObject, name: Option[String],
                     raDeg: String, decDeg: String,
                     raHMS: String, decDMS: String) : Unit = so.getCoordinates(None).foreach { c =>
        name.foreach(n => config.putParameter(systemName, StringParameter.getInstance(n, so.getName)))
        config.putParameter(systemName, StringParameter.getInstance(raDeg,  c.ra.formatDegrees))
        config.putParameter(systemName, StringParameter.getInstance(decDeg, c.dec.formatDegrees))
        config.putParameter(systemName, StringParameter.getInstance(raHMS,  c.ra.formatHMS))
        config.putParameter(systemName, StringParameter.getInstance(decDMS, c.dec.formatDMS))
      }

      /** Add the target information for the observation for GHOST to the
        * sequence.
        *
        * Something tells me that there's a nicer way to get the TargetEnvironment
        * for the observation than this, but I'm not sure what it is. This is the
        * way it's done to produce an ObsContext.
        */
      getObsComponent.getContextObservation.getObsComponents.asScala.
        find(_.getType.broadType === TargetObsComp.SP_TYPE.broadType).
        map(_.getDataObject.asInstanceOf[TargetObsComp]).
        foreach{_.getAsterism match {
          case gsr: GhostAsterism.StandardResolution =>
            gsr.overriddenBase.foreach(b => coordParam(b, None,
              Ghost.BaseRADegrees, Ghost.BaseDecDegrees,
              Ghost.BaseRAHMS, Ghost.BaseDecDMS))
            gsr.srifu1.fold(c => coordParam(c, Some(Ghost.SRIFU1Name),
              Ghost.SRIFU1RADeg, Ghost.SRIFU1DecDeg,
              Ghost.SRIFU1RAHMS, Ghost.SRIFU1DecDMS),
              t => coordParam(t.spTarget, Some(Ghost.SRIFU1Name),
                Ghost.SRIFU1RADeg, Ghost.SRIFU1DecDeg,
                Ghost.SRIFU1RAHMS, Ghost.SRIFU1DecDMS))
            gsr.srifu2.foreach(_.fold(c => coordParam(c, Some(Ghost.SRIFU2Name),
              Ghost.SRIFU2RADeg, Ghost.SRIFU2DecDeg,
              Ghost.SRIFU2RAHMS, Ghost.SRIFU2DecDMS),
              t => coordParam(t.spTarget, Some(Ghost.SRIFU2Name),
                Ghost.SRIFU2RADeg, Ghost.SRIFU2DecDeg,
                Ghost.SRIFU2RAHMS, Ghost.SRIFU2DecDMS)))

          case ghr: GhostAsterism.HighResolution =>
            ghr.overriddenBase.foreach(b => coordParam(b, None,
              Ghost.BaseRADegrees, Ghost.BaseDecDegrees,
              Ghost.BaseRAHMS, Ghost.BaseDecDMS))
            coordParam(ghr.hrifu1.spTarget, Some(Ghost.HRIFU1Name),
              Ghost.HRIFU1RADeg, Ghost.HRIFU1DecDeg,
              Ghost.HRIFU1RAHMS, Ghost.HRIFU1DecDMS)
            ghr.hrifu2.foreach(c => coordParam(c, Some(Ghost.HRIFU2Name),
              Ghost.HRIFU2RADeg, Ghost.HRIFU2DecDeg,
              Ghost.HRIFU2RAHMS, Ghost.HRIFU2DecDMS))

          case _ =>
            // The asterism may not have been configured by this point.
        }}
    }
  }
}
