package edu.gemini.spModel.gemini.ghost

import java.time.Instant
import java.util.{Map => JMap}
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.config.AbstractObsComponentCB
import edu.gemini.spModel.data.config._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.target.{SPCoordinates, SPSkyObject, SPTarget}

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz._

/**
  * Configuration builder for GHOST.
  */
final class GhostCB(obsComp: ISPObsComponent) extends AbstractObsComponentCB(obsComp) {

  @transient private var sysConfig: Option[ISysConfig] = None

  override def clone(): AnyRef = {
    val result = super.clone().asInstanceOf[GhostCB]
    result.sysConfig = None
    result
  }

  private def getGhostComponent: Ghost =
    getDataObject.asInstanceOf[Ghost]

  override def thisReset(options: JMap[String, Object]): Unit = {
    sysConfig = Some(getGhostComponent.getSysConfig)
  }

  override protected def thisHasConfiguration(): Boolean =
    sysConfig.exists(_.getParameterCount > 0)

  override protected def thisApplyNext(config: IConfig, prevFull: IConfig): Unit = {

    GhostExposureTimeProvider.addToConfig(config, SeqConfigNames.OBSERVE_CONFIG_NAME, getGhostComponent)

    sysConfig.foreach { sc =>
      val systemName: String = sc.getSystemName
      sc.getParameters.asScala.foreach { p =>
        config.putParameter(systemName, DefaultParameter.getInstance(p.getName, p.getValue))
      }
      config.putParameter(systemName,
        StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, Ghost.INSTRUMENT_NAME_PROP)
      )

      /**
       * Put the information for a coordinate parameter, i.e. either SPSkyCoordinates or SPTarget.
       * We put in:
       * 1. RA in degrees (as a Double) and HMS;
       * 2. Dec in degrees (as a Double) and DMS.
       */
      // Put a coordinate parameter. Coordinates are not available here so we
      // must operate on an SPSkyObject.
      def coordParam(so: SPSkyObject, name: Option[String],
                     raDeg: String, decDeg: String,
                     raHMS: String, decDMS: String) : Unit = {
        val coords = so match {
          case c: SPCoordinates => c.getCoordinates.some
          case t: SPTarget => t.getProperMotion.map(_.calculateAt(t.getTarget, Instant.now())).orElse(t.getCoordinates(None))
        }
        coords.foreach { c =>
          name.foreach(n => config.putParameter(systemName, StringParameter.getInstance(n, so.getName)))
          config.putParameter(systemName, DefaultParameter.getInstance(raDeg, c.ra.toDegrees))
          config.putParameter(systemName, DefaultParameter.getInstance(decDeg, c.dec.toDegrees))
          config.putParameter(systemName, DefaultParameter.getInstance(raHMS, c.ra.formatHMS))
          config.putParameter(systemName, DefaultParameter.getInstance(decDMS, c.dec.formatDMS))
        }
      }

      /**
       * Add the guiding information for an SRIFU or HRIFU pointing at a target.
       */
      def guiding(name: String, t: GhostTarget): Unit =
        config.putParameter(systemName, DefaultParameter.getInstance(name, t.guideFiberState))

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
        foreach{t =>
          t.getTargetEnvironment.getUserTargets.asScala.zipWithIndex.foreach { case (ut, i) =>
            val (a, b, c, d, e)= Ghost.userTargetParams(i + 1)
            coordParam(ut.target, Some(a), b, c, d, e)
            config.putParameter(systemName, DefaultParameter.getInstance(s"userTarget${i+1}Type", ut.`type`))
          }
          config.putParameter(systemName, DefaultParameter.getInstance(Ghost.RESOLUTION_MODE, t.getAsterism.resolutionMode))
          t.getAsterism match {

            /** STANDARD RESOLUTION
             * 1. If the base is overridden, add it to the parameters.
             * 2. If SRIFU1 is pointing to a sky position, write sky position coordinates.
             *    Otherwise, write target coordinates and indicate if guiding is turned on for SRIFU1.
             * 3. If SRIFU2 is set, it can be a sky position or target: repeat the previous step. for SRIFU2 data.
             */
            case gsr: GhostAsterism.StandardResolution =>
              gsr.overriddenBase.foreach(b => coordParam(b, None,
                Ghost.BASE_RA_DEGREES, Ghost.BASE_DEC_DEGREES,
                Ghost.BASE_RA_HMS, Ghost.BASE_DEC_DMS))

              gsr.srifu1.fold(c => coordParam(c, Some(Ghost.SRIFU1_NAME),
                Ghost.SRIFU1_RA_DEG, Ghost.SRIFU1_DEC_DEG,
                Ghost.SRIFU1_RA_HMS, Ghost.SRIFU1_DEC_DMS),
                t => {
                  coordParam(t.spTarget, Some(Ghost.SRIFU1_NAME),
                    Ghost.SRIFU1_RA_DEG, Ghost.SRIFU1_DEC_DEG,
                    Ghost.SRIFU1_RA_HMS, Ghost.SRIFU1_DEC_DMS)
                  guiding(Ghost.SRIFU1_GUIDING, t)
                })

              gsr.srifu2.foreach(_.fold(c => coordParam(c, Some(Ghost.SRIFU2_NAME),
                Ghost.SRIFU2_RA_DEG, Ghost.SRIFU2_DEC_DEG,
                Ghost.SRIFU2_RA_HMS, Ghost.SRIFU2_DEC_DMS),
                t => {
                  coordParam(t.spTarget, Some(Ghost.SRIFU2_NAME),
                    Ghost.SRIFU2_RA_DEG, Ghost.SRIFU2_DEC_DEG,
                    Ghost.SRIFU2_RA_HMS, Ghost.SRIFU2_DEC_DMS)

                  guiding(Ghost.SRIFU2_GUIDING, t)
                }))

              /** HIGH RESOLUTION
               * 1. If the base is overridden, add it to the parameters.
               * 2. HRIFU1 is always pointing to a target: include the info.
               * 3. Indicate if guiding is turned on for HRIFU1,
               * 4. If we have a sky position for HRIFU2, add it to the parameters.
               */
            case ghr: GhostAsterism.HighResolution =>
              ghr.overriddenBase.foreach(b => coordParam(b, None,
                Ghost.BASE_RA_DEGREES, Ghost.BASE_DEC_DEGREES,
                Ghost.BASE_RA_HMS, Ghost.BASE_DEC_DMS))

              // Always target.
              coordParam(ghr.hrifu.spTarget, Some(Ghost.HRIFU1_NAME),
                Ghost.HRIFU1_RA_DEG, Ghost.HRIFU1_DEC_DEG,
                Ghost.HRIFU1_RA_HMS, Ghost.HRIFU1_DEC_DMS)
              guiding(Ghost.HRIFU1_GUIDING, ghr.hrifu)

              // Always sky.
              coordParam(
                ghr.hrsky,  // switch to ghr.srifu2 if we're supposed to send IFU2 coords
                Some(Ghost.HRIFU2_NAME),
                Ghost.HRIFU2_RA_DEG,
                Ghost.HRIFU2_DEC_DEG,
                Ghost.HRIFU2_RA_HMS,
                Ghost.HRIFU2_DEC_DMS
              )

            case _ =>
              // The asterism may not have been configured by this point.
        }}
    }
  }
}