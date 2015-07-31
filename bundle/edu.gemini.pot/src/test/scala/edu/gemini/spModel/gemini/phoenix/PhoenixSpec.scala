package edu.gemini.spModel.gemini.phoenix

import edu.gemini.model.p1.immutable.{PhoenixFocalPlaneUnit, PhoenixFilter}
import edu.gemini.spModel.core.Angle
import org.specs2.mutable.Specification
import edu.gemini.pot.ModelConverters._

class PhoenixSpec extends Specification {
  "Phoenix" should {
    "Support all phase1 filters" in {
      PhoenixFilter.values.forall(f => Option(PhoenixParams.Filter.valueOf(f.name)).isDefined) should beTrue
    }
    "Support all phase1 fpus" in {
      PhoenixFocalPlaneUnit.values.forall(f => Option(PhoenixParams.Mask.valueOf(f.name)).isDefined) should beTrue
    }
    "Use the same vignetting parameters as Visitor, REL-2436" in {
      val phoenix = new InstPhoenix()
      phoenix.pwfs1VignettingClearance().toNewModel should beGreaterThan(Angle.zero)
      phoenix.pwfs2VignettingClearance().toNewModel should beGreaterThan(Angle.zero)
    }
  }
}
