package edu.gemini.p2checker.rules

import java.util.UUID

import edu.gemini.p2checker.api.ObservationElements
import edu.gemini.p2checker.rules.general.GeneralRule
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz.syntax.id._

class WfsRuleSpec extends Specification {

  // Expect exactly the specified error strings (in any order) given an observation with
  // default base named "Foo", and guider = mod(base).
  def expect(errs: String*)(mod: SiderealTarget => Target) = {

    // Ok just a bunch of setup. Program with one observation, GMOS, default target env.
    val f = POTUtil.createFactory(UUID.randomUUID())
    val p = f.createProgram(null, SPProgramID.toProgramID("GS-2014B-Q-1"))
    val o = f.createObservation(p, null) <| p.addObservation
    val i = f.createObsComponent(p, SPComponentType.INSTRUMENT_GMOS, null) <| o.addObsComponent
    val e = o.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV).get
    val t = e.getDataObject.asInstanceOf[TargetObsComp]

    // Rename base to "Foo" and add a guidestar that's mod(base.clone())
    t.setTargetEnvironment {
      val te  = t.getTargetEnvironment <| (_.getAsterism.allSpTargets.head.setName("Foo"))
      val g   = te.getAsterism.allSpTargets.head.clone() <| (g => g.setTarget(mod(g.getTarget.asInstanceOf[SiderealTarget])))
      val p   = GmosOiwfsGuideProbe.instance
      val gpt = GuideProbeTargets.create(p, g)
      val gg  = te.getPrimaryGuideGroup.setAll(List(gpt).asImList)
      te.setPrimaryGuideGroup(gg)
    }

    // Update the data object
    e.setDataObject(t)

    // Run the rule and look for expected messages.
    val oe = new ObservationElements(o)
    val ps = new GeneralRule().check(oe).getProblems.asScala.map(_.getDescription)
    ps must containTheSameElementsAs(errs)

  }

  val rule = new GeneralRule()


  "WFS_RULE (same tag, coords, name)" should {

    "TCN  Do nothing for identical targets" in {
      expect() {
        identity
      }
    }

    "TC-  Identify targets that should have the same name" in {
      expect("Objects with the same coordinates must have the same name.") {
        Target.name.set(_, "Bar")
      }
    }

    "T-N  Identify targets that should have the same coordinates" in {
      expect("Objects with the same name must have the same coordinates.") {
        (SiderealTarget.coordinates >=> Coordinates.ra).set(_, RightAscension.fromAngle(Angle.fromDegrees(1.23)))
      }
    }

    "T--  Do nothing for targets that differ in coords and name" in {
      expect() { t =>
        val t1 = (SiderealTarget.coordinates >=> Coordinates.ra).set(t, RightAscension.fromAngle(Angle.fromDegrees(1.23)))
        val t2 = SiderealTarget.name.set(t1, "Bar")
        t2
      }
    }

    "-CN  Identify targets that should have the same type" in {
      expect("Objects with the same name must have the same type and coordinates.") { t =>
        NonSiderealTarget.empty.copy(name = t.name)
      }
    }

    "-C-  Do nothing for targets that differ in type and name" in {
      expect() { t =>
        NonSiderealTarget.empty.copy(name = "Bar")
      }
    }

    "---  Do nothing for targets with nothing in common" in {
      expect() { t =>
        TooTarget.empty
      }
    }


  }

}

