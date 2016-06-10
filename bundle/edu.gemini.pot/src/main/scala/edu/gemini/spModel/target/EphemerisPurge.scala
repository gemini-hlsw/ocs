package edu.gemini.spModel.target

import edu.gemini.pot.sp.{ISPObsComponent, ISPObservation}
import edu.gemini.shared.util.immutable.ScalaConverters.ImOptionOps
import edu.gemini.spModel.core.{Ephemeris, Coordinates}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._

import scalaz._, Scalaz._
import scalaz.effect.IO

/** A utility for purging ephemeris data from an observation.  "Purge" means
  * truncate the ephemeris down to a single element valid at the scheduling
  * block start time.
  */
object EphemerisPurge {

  /** Returns an `Option[ IO[Unit] ]` with any actions required to purge the
    * observation of ephemeris data.  The `Option` is defined if the observation
    * will be modified when executed.
    */
  def purge(o: ISPObservation): Option[IO[Unit]] = {
    def go(oc: ISPObsComponent, toc: TargetObsComp): Option[IO[Unit]] = {

      // Get the scheduling block start time, if defined.
      val when = o.getDataObject match {
        case spObs: SPObservation => spObs.getSchedulingBlockStart.asScalaOpt.map(_.toLong)
        case _                    => None
      }

      // Get a list[(SPTarget, NonSiderealTarget)] for all non-sidereal targets
      // in the environment (if any).
      val nsts = toc.getTargetEnvironment.getTargets.asScala.flatMap { sp =>
        sp.getNonSiderealTarget.strengthL(sp)
      }

      // Map each (SPTarget, NonSiderealTarget) pair to (SPTarget, Option[NonSiderealTarget])
      // where the option is defined only if an update is needed and if so,
      // the NonSiderealTarget is the updated target.  Then collect only those
      // with updates.
      val updates = nsts.map { _.map { nst =>
        val site  = nst.ephemeris.site
        val tc    = when.flatMap { t => nst.ephemeris.iLookup(t).strengthL(t) }
        val table = tc.fold[Long ==>> Coordinates](==>>.empty) { case (t,c) => ==>>.singleton(t, c) }
        val eph   = Ephemeris(site, table)
        (eph =/= nst.ephemeris) ? some(nst.copy(ephemeris = eph)) | none
      }}.collect { case (sp, Some(updatedNst)) => (sp, updatedNst) }

      // Only apply new data object if there are actual updates.  Note,
      // the target environment is immutable but holds references to
      // mutable SPTargets.  We will have updated those SPTargets.  We
      // have to store that back to the ISPObsComponent because we are
      // working with a clone of its data object.
      updates.nonEmpty option IO {
        updates.foreach { case (sp, nst) => sp.setTarget(nst) }
        oc.setDataObject(toc)
      }
    }

    Option(SPTreeUtil.findTargetEnvNode(o)).flatMap { oc =>
      oc.getDataObject match {
        case toc: TargetObsComp => go(oc, toc)
        case _                  => none
      }
    }
  }
}
