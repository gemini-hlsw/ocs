package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.sp.{ ISPFactory, ISPProgram }
import edu.gemini.pot.sp.Instrument.GmosSouth

import edu.gemini.shared.util.immutable.ImOption

import edu.gemini.spModel.core.{ ProgramId, SPProgramID }
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import edu.gemini.spModel.obs.{ ObsPhase2Status, SPObservation }
import edu.gemini.spModel.obsrecord.ObsExecStatus
import edu.gemini.spModel.util.SPTreeUtil

import ObsPhase2Status.PHASE_2_COMPLETE
import ObsExecStatus.{ PENDING, ONGOING }
import TimingWindow.{ REPEAT_FOREVER, REPEAT_NEVER, WINDOW_REMAINS_OPEN_FOREVER }

import scala.collection.JavaConverters._

object Setup {

  final case class Obs(
    phase2Status: ObsPhase2Status,
    execStatus:   ObsExecStatus
  ) {

    def valid: Boolean =
      phase2Status == PHASE_2_COMPLETE && (execStatus == PENDING || execStatus == ONGOING)
  }

  final case class Prog(
    pid:          SPProgramID,
    active:       Active,
    completed:    Boolean,
    shouldNotify: Boolean,
    obs:          List[Obs]
  ) {

    def valid: Boolean =
      ProgramId.parse(pid.toString).ptype.exists(_.isScience) &&
        active == Active.YES                                  &&
        !completed                                            &&
        shouldNotify

    /** Creates an ISPProgram matching the specification. */
    def create(f: ISPFactory, tw: List[TimingWindow]): ISPProgram = {
      val pn = f.createProgram(null, pid)

      val pd = new SPProgram
      pd.setActive(active)
      pd.setCompleted(completed)
      pd.setTimingWindowNotification(shouldNotify)
      pn.setDataObject(pd)

      val ons = obs.map { o =>
        val on = f.createObservation(pn, ImOption.apply(GmosSouth), null)

        val od = new SPObservation
        od.setPhase2Status(o.phase2Status)
        od.setExecStatusOverride(ImOption.apply(o.execStatus))
        on.setDataObject(od)

        val cn = SPTreeUtil.findObsCondNode(on)
        val cd = new SPSiteQuality
        cd.setTimingWindows(tw.asJava)
        cn.setDataObject(cd)

        on
      }

      pn.setObservations(ons.asJava)
      pn
    }
  }

}
