package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.graces.Graces
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint._
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint.ReadMode._
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint.FiberMode._
import edu.gemini.spModel.target.SPTarget

import org.specs2.mutable.Specification

import scalaz.syntax.id._

class REL_1842_Test extends Specification {


  def incSpec(r: ReadMode, f: FiberMode, rMag: Option[Double], inc: Set[Int], excl: Set[Int]) = {
    val t = rMag.map(m => new SPTarget <| (_.getTarget.putMagnitude(new Magnitude(Band.R, m))))
    val g = Graces(new SpGracesBlueprint(r, f), t)
    val n = s"Graces($r, $f, R-MAG ${rMag.fold("--")(_.toString)})"
    s"Initialized $n" should {
      if (excl.nonEmpty) {
        s"exclude obs ${excl.mkString("{", ",", "}")} from target group" in {
          g.targetGroup.filter(excl) must beEmpty
        }
      }
      s"include obs ${inc.mkString("{",",","}")} in target group" in {
        g.targetGroup.filter(inc).toSet must_== inc
      }
    }
  }

  // Tests for R-band selection of acquisitions
  incSpec(FAST, ONE_FIBER, None,     Set(1,2,3,4), Set())
  incSpec(FAST, ONE_FIBER, Some(3),  Set(1),       Set(2,3,4))
  incSpec(FAST, ONE_FIBER, Some(7),  Set(2),       Set(1,3,4))
  incSpec(FAST, ONE_FIBER, Some(15), Set(3),       Set(1,2,4))
  incSpec(FAST, ONE_FIBER, Some(22), Set(4),       Set(1,2,3))

  // Tests for config selection of science obs
  incSpec(FAST,   ONE_FIBER, None, Set(5),  Set(6, 7, 8, 9, 10))
  incSpec(NORMAL, ONE_FIBER, None, Set(6),  Set(5, 7, 8, 9, 10))
  incSpec(SLOW,   ONE_FIBER, None, Set(7),  Set(5, 6, 8, 9, 10))
  incSpec(FAST,   TWO_FIBER, None, Set(8),  Set(5, 6, 7, 9, 10))
  incSpec(NORMAL, TWO_FIBER, None, Set(9),  Set(5, 6, 7, 8, 10))
  incSpec(SLOW,   TWO_FIBER, None, Set(10), Set(5, 6, 7, 8, 9))

}
