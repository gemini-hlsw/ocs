package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.target.SPTarget

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz._, Scalaz._

class GuideGroupSpec extends Specification with ScalaCheck with Arbitraries {
  import GuideGroupSpec.AllProbes

  "GuideGroup name" should {
    "always be defined for manual groups, undefined for automatic" in
      forAll { (g: GuideGroup) =>
        g.grp match {
          case a: AutomaticGroup => g.getName == ImOption.empty
          case _                 => g.getName != ImOption.empty
        }
      }

    "be modifiable for manual groups but updates ignored for automatic groups" in
      forAll { (g: GuideGroup, n: String) =>
        val nopt = ImOption.apply(n)
        g.grp match {
          case a: AutomaticGroup => g.setName(nopt) == g
          case _                 => g.setName(nopt).getName == nopt &&
                                    g.setName(n).getName == nopt
        }
      }
  }

  "GuideGroup contains" should {
    "be false for any guide probe for the initial automatic group" in
      forAll { (gp: GuideProbe) =>
        !GuideGroup(AutomaticGroup.Initial).contains(gp)
      }

    "be true iff there are targets associated with the guide probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.contains(gp) == g.get(gp).asScalaOpt.exists(_.getTargets.nonEmpty())
        }
      }
  }

  "GuideGroup get" should {
    "return none or else a non empty list" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall(_.getTargets.nonEmpty)
        }
      }

    "return none or else GuideProbeTargets with a matching probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall(_.getGuider == gp)
        }
      }

    "return a non empty GuideProbeTargets iff the group contains the probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.isDefined == g.contains(gp)
        }
      }

    "return a GuideProbeTargets with primary star that matches the options list focus" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall { gpt =>
            gpt.getPrimary.asScalaOpt == (g.grp match {
              case ManualGroup(_, m)        => m.get(gp).flatMap { _.focus }
              case AutomaticGroup.Active(m) => m.get(gp)
              case AutomaticGroup.Initial   => None
            })
          }
        }
      }
  }

  "GuideGroup put" should {
    "remove all guide stars if GuideProbeTargets is empty" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val emptyGpt = GuideProbeTargets.create(gp)
          val g2       = g.put(emptyGpt)
          !g2.contains(gp) && g2.get(gp).isEmpty
        }
      }

    "remove the primary guide star if there is no primary in GuideProbeTargets" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val noPrimaryGpt = GuideProbeTargets.create(gp, new SPTarget())
          val g2           = g.put(noPrimaryGpt)
          g2.get(gp).asScalaOpt.forall { _.getPrimary.isEmpty }
        }
      }

    "remove the guider from automatic groups if there is no primary in GuideProbeTargets" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val noPrimaryGpt = GuideProbeTargets.create(gp, new SPTarget())
          val g2           = g.put(noPrimaryGpt)
          val gpt2         = g2.get(gp).asScalaOpt
          g2.grp match {
            case _: AutomaticGroup => gpt2.isEmpty
            case _: ManualGroup    => gpt2.exists(_.getPrimary.isEmpty)
          }
        }
      }

    "set guide stars associated with a probe but ignore non-primary guide stars for automatic groups" in
      forAll { (g: GuideGroup) =>
        val primaryTarget    = new SPTarget() <| (_.setName("primary"))
        val notPrimaryTarget = new SPTarget() <| (_.setName("not primary"))
        AllProbes.forall { gp =>
          val gpt  = GuideProbeTargets.create(gp, ImOption.apply(primaryTarget), primaryTarget, notPrimaryTarget)
          val g2   = g.put(gpt)
          val lst2 = g2.get(gp).asScalaOpt.toList.flatMap(_.getTargets.asScalaList)
          g2.grp match {
            case _: AutomaticGroup => lst2 == List(primaryTarget)
            case _: ManualGroup    => lst2 == List(primaryTarget, notPrimaryTarget)
          }
        }
      }
  }
}

object GuideGroupSpec {
  val AllProbes: List[GuideProbe] = GuideProbeMap.instance.values.asScala.toList

}
