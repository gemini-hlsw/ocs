package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.gpi._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.gpi.Gpi
import org.specs2.mutable.Specification
import edu.gemini.model.p1.immutable.GpiBlueprint
import edu.gemini.model.p1.mutable.{GpiObservingMode, GpiDisperser}

object REL_2465_Test extends TemplateSpec("GPI_BP.xml") with Specification {
  import GpiDisperser._, GpiFilterGroup._

  def gpiTest(disp: GpiDisperser, mode: GpiObservingMode, incl: Set[Int]): Unit =
    expand(proposal(GpiBlueprint(mode, disp), List(1,2,3), MagnitudeBand.R)) { (p, sp) =>
      s"GPI Blueprint Expansion $disp $mode " >> {

        "There should be exactly one template group." in {
          groups(sp).size must_== 1
        }

        // Check that the group has the expected inclusions and nothing else
        val excl = (1 to 8).toSet -- incl
        checkLibs("Group", groups(sp).head, incl, excl)

      }
    }

  // Add tests for every disperser/mode combination
  for {
    disp <- GpiDisperser.values
    mode <- GpiObservingMode.values
  } {

    // Expected inclusion is based on filter group
    val incl: Set[Int] =
      (disp, GpiFilterGroup.lookup(Gpi.ObservingMode.valueOf(mode.name)).get) match {
        case (PRISM, Yjh)      => Set(8, 2)
        case (PRISM, K1k2)     => Set(1, 3, 4)
        case (WOLLASTON, Yjh)  => Set(5, 6)
        case (WOLLASTON, K1k2) => Set(5, 7)
      }

    gpiTest(disp, mode, incl)

  }

}
