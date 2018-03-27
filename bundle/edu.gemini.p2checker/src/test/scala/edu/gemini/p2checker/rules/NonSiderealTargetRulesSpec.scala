package edu.gemini.p2checker.rules

import java.util.UUID

import edu.gemini.p2checker.target.NonSiderealTargetRules
import edu.gemini.pot.sp.{Instrument, ISPObservation, SPComponentType}
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obs.{SPObservation, SchedulingBlock}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import scalaz._, Scalaz._

class NonSiderealTargetRulesSpec extends RuleSpec {

  val ruleSet = new NonSiderealTargetRules()
  import NonSiderealTargetRules._

  ERR_NO_SCHEDULING_BLOCK should {

    "error if no scheduling block defined with nonsidereal targets" in
      expectAllOf(ERR_NO_SCHEDULING_BLOCK) {
        obs(NonSiderealTarget.empty, None)
      }

    "no error if no scheduling block defined without nonsidereal targets" in
      expectNoneOf(ERR_NO_SCHEDULING_BLOCK) {
        obs(SiderealTarget.empty, None)
      }

    "no error if scheduling block defined" in
      expectNoneOf(ERR_NO_SCHEDULING_BLOCK) {
        obs(SiderealTarget.empty, Some(SchedulingBlock(0L)))
      }

  }

  ERR_NO_EPHEMERIS_FOR_BLOCK should {

    "error if no ephemeris for scheduling block" in
      expectAllOf(ERR_NO_EPHEMERIS_FOR_BLOCK) {
        obs(NonSiderealTarget.empty, Some(SchedulingBlock(0L)))
      }

    "on error if ephemeris for scheduling block" in
      expectNoneOf(ERR_NO_EPHEMERIS_FOR_BLOCK) {
        obs(NonSiderealTarget.empty.copy(ephemeris = Ephemeris(Site.GS, ==>>.singleton(0L, Coordinates.zero))), Some(SchedulingBlock(0L)))
      }

  }

  ERR_NO_HORIZONS_DESIGNATION should {

    "error if no horizons designation" in {
      expectAllOf(ERR_NO_HORIZONS_DESIGNATION) {
        obs(NonSiderealTarget.empty, None)
      }
    }

    "no error if horizons designation present" in {
      expectNoneOf(ERR_NO_HORIZONS_DESIGNATION) {
        obs(NonSiderealTarget.empty.copy(horizonsDesignation = Some(HorizonsDesignation.Comet("foo"))), None)
      }
    }

  }

  ERR_EPHEMERIS_WRONG_SITE should {

    "no error for sidereal target" in {
      expectNoneOf(ERR_EPHEMERIS_WRONG_SITE) {
        obs(SiderealTarget.empty, None)
      }
    }

    "no error for TOO target" in {
      expectNoneOf(ERR_EPHEMERIS_WRONG_SITE) {
        obs(TooTarget.empty, None)
      }
    }

    "no error for non-sidereal target with wrong site and empty ephemeris" in {
      expectNoneOf(ERR_EPHEMERIS_WRONG_SITE) {
        val t = NonSiderealTarget.ephemeris.set(NonSiderealTarget.empty, Ephemeris(Site.GS, ==>>.empty))
        obs(t, None, InstGmosNorth.SP_TYPE)
      }
    }

    "no error for non-sidereal target with wrong site and trivial ephemeris" in {
      expectNoneOf(ERR_EPHEMERIS_WRONG_SITE) {
        val t = NonSiderealTarget.ephemeris.set(NonSiderealTarget.empty, Ephemeris.singleton(Site.GS, 0L, Coordinates.zero))
        obs(t, None, InstGmosNorth.SP_TYPE)
      }
    }

    "no error for non-sidereal target with correct site and nontrivial ephemeris" in {
      expectNoneOf(ERR_EPHEMERIS_WRONG_SITE) {
        val t = NonSiderealTarget.ephemeris.set(NonSiderealTarget.empty, Ephemeris(Site.GS, ==>>((0L, Coordinates.zero), (1L, Coordinates.zero)) ))
        obs(t, None, InstGmosSouth.SP_TYPE)
      }
    }

    "error for non-sidereal target with wrong site and nontrivial ephemeris" in {
      expectAllOf(ERR_EPHEMERIS_WRONG_SITE) {
        val t = NonSiderealTarget.ephemeris.set(NonSiderealTarget.empty, Ephemeris(Site.GS, ==>>((0L, Coordinates.zero), (1L, Coordinates.zero)) ))
        obs(t, None, InstGmosNorth.SP_TYPE)
      }
    }

  }

  def obs(target: Target, sb: Option[SchedulingBlock], inst: SPComponentType = InstGmosSouth.SP_TYPE): ISPObservation = {
    val f = POTUtil.createFactory(UUID.randomUUID())
    val p = f.createProgram(null, SPProgramID.toProgramID("no-site"))
    val o = f.createObservation(p, Instrument.none, null) <| { on =>
      p.addObservation(on)
      on.getDataObject.asInstanceOf[SPObservation] <| { o =>
        o.setSchedulingBlock(sb.asGeminiOpt)
        on.setDataObject(o)
      }
    }
    val e = o.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV).get
    val t = e.getDataObject.asInstanceOf[TargetObsComp] <| { toc =>
      toc.getTargetEnvironment <| { te =>
        val te2 = te.setBasePosition(new SPTarget(target))
        toc.setTargetEnvironment(te2)
      }
      e.setDataObject(toc)
    }
    f.createObsComponent(p, inst, null) <| { i => o.addObsComponent(i) }
    o
  }

}
