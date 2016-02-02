package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.core.Helpers
import edu.gemini.spModel.target.env.TargetCollection.TargetCollectionSyntax

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget
import org.apache.commons.io.output.ByteArrayOutputStream

import org.scalacheck.Prop._

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}

import scala.collection.JavaConverters._
import scalaz._, Scalaz._


class GuideEnvironmentSpec extends Specification with ScalaCheck with Arbitraries with Almosts with Helpers {

  "GuideEnvironment guider references" should {
    def toScala(s: java.util.Set[GuideProbe]): Set[GuideProbe] =
      s.asScala.toSet

    "include all guide probes associated with a guide star in getReferencedGuiders" in
      forAll { (g: GuideEnvironment) =>
        val gs = (Set.empty[GuideProbe]/:g.getOptions.asScalaList) { (s, grp) =>
          s ++ toScala(grp.getReferencedGuiders)
        }
        gs === toScala(g.getReferencedGuiders)
      }

    "include only guide probes associated with the primary group that have a selected guide star in getPrimaryReferencedGuiders" in
      forAll { (g: GuideEnvironment) =>
        val s0 = toScala(g.getPrimary.getPrimaryReferencedGuiders)
        val s1 = toScala(g.getPrimaryReferencedGuiders)
        s0 === s1
      }
  }

  "GuideEnvironment getTargets" should {
    "return all targets in order according to their associated probe" in
      forAll { (g: GuideEnvironment) =>
        val maps = g.guideEnv.groups.map(_.targets)
        val ts   = (maps :\ ==>>.empty[GuideProbe, NonEmptyList[SPTarget]]) { (cur, acc) =>
          cur.unionWith(acc)(_ append _)
        }
        g.getTargets.asScalaList == ts.values.map(_.toList).flatten
      }
  }

  "GuideEnvironment setOptions" should {
    "keep the initial automatic group, if any; otherwise use a new AutomaticGroup.Initial" in
      forAll { (gs: ImList[GuideGroup]) =>
        val env  = GuideEnvironment.Initial.setOptions(gs)
        val grps = env.getOptions

        val expected = gs.headOption.asScalaOpt.filter(_.grp.isAutomatic).getOrElse(GuideGroup(AutomaticGroup.Initial))
        val actual   = env.getOptions.head

        expected === actual
      }

    "convert all groups except the first one to equivalent manual groups" in
      forAll { (gs: ImList[GuideGroup]) =>
        val env = GuideEnvironment.Initial.setOptions(gs)

        val expected = (gs.asScalaList.map(_.grp) match {
          case Nil                      => Nil
          case (a: AutomaticGroup) :: t => t
          case ms                       => ms
        }).map {
          case a: AutomaticGroup => ManualGroup("", a.targetMap.map(t => OptsList.focused(t)))
          case g                 => g
        }

        val actual = env.getOptions.asScalaList.tail.map(_.grp)

        expected === actual
      }

    "select the element at the same index as the existing primary if there are enough options in the new list" in
      forAll { (env: GuideEnvironment, newOptions: ImList[GuideGroup], i: Int) =>
        val oldIndex = env.getPrimaryIndex.intValue
        val newSize  = newOptions.size.toInt + (newOptions.headOption.asScalaOpt.filter(_.grp.isAutomatic).isDefined ? 0 | 1)
        val newEnv   = env.setOptions(newOptions)
        val newIndex = newEnv.getPrimaryIndex.intValue

        if (oldIndex < newSize) newIndex === oldIndex else newIndex === (newSize - 1)
      }
  }

  "GuideEnvironment removeGroup" should {
    "do nothing if the group is automatic" in {
      forAll { (g: GuideEnvironment) =>
        val auto = g.getOptions.head
        auto.grp match {
          case _: AutomaticGroup => g.removeGroup(0) === g
          case _                 => false
        }
      }
    }

    "remove the manual options altogether if the last one is removed" in
      forAll { (g0: GuideEnvironment) =>
        g0.getOptions.asScalaList match {
          case (_ :: m :: Nil) =>
            val g1 = g0.removeGroup(1)
            g1.guideEnv.manual.isEmpty && g1.getPrimaryIndex == 0
          case _                       =>
            true
        }
      }

    "remove the manual group if it exists in the environment" in
      forAll { (g0: GuideEnvironment, i: Int) =>
        g0.getOptions.asScalaList match {
          case (_ :: h :: t) =>  // auto :: first_manual :: other_manuals
            val ms = h :: t
            val ix = (i % ms.size).abs + 1 // pick a manual group
            val g1 = g0.removeGroup(ix)
            g1.getOptions.asScalaList.tail === ms.patch(ix - 1, Nil, 1)
          case _              =>
            true
        }
      }

    "keep the same focus unless removing the focused group" in
      forAll { (g0: GuideEnvironment, i: Int) =>
        g0.getOptions.asScalaList match {
          case (_ :: h :: t) =>  // auto :: first_manual :: other_manuals
            val ms = h :: t
            val ix = (i % ms.size).abs + 1 // pick a manual group
            val g1 = g0.removeGroup(ix)

            // removing the focused group or else the focus is the same after
            // the removal
            (g0.getPrimaryIndex.intValue() === ix) || (g0.getPrimary === g1.getPrimary)
          case _              =>
            true
        }
      }

    "when removing the primary, move the primary to the right unless at the end in which case to the left" in
      forAll { (g0: GuideEnvironment, i: Int) =>
        g0.getOptions.asScalaList match {
          case (_ :: h :: t) =>  // auto :: first_manual :: other_manuals
            val ms = h :: t
            val ix = (i % ms.size).abs  + 1 // pick a manual group
            val g1 = g0.removeGroup(ix)
            val i0 = g0.getPrimaryIndex.intValue
            val i1 = g1.getPrimaryIndex.intValue

            (i0 =/= ix) || {
               // focus should have changed
              (System.identityHashCode(g0.getPrimary) =/= System.identityHashCode(g1.getPrimary)) && {
                (i0 === i1) || // but moved to the right if possible (i.e., same index)
                  ((i0 === ms.size) && (i1 === (i0 - 1))) // or one to the left if at the end
              }
            }
          case _              =>
            true
        }
      }
  }

  "GuideEnvironment getGroup" should {
    "return a group iff the index is in range" in
      forAll { (g: GuideEnvironment, i: Int) =>
        g.getGroup(i).isDefined === ((i >= 0) && (i < g.getOptions.size()))
      }

    "return the group corresponding to the index" in
      forAll { (g: GuideEnvironment, i: Int) =>
        val gs = g.getOptions
        val ix = (i % gs.size()).abs
        g.getOptions.get(ix) === g.getGroup(ix).getValue
      }
  }

  "GuideEnvironment setGroup" should {
    "do nothing if the group index is out of range" in
      forAll { (g: GuideEnvironment) =>
        val grp = GuideGroup.create("")
        val sz  = g.getOptions.size
        val ixs = List(Int.MinValue, -1, sz, sz + 1, Int.MaxValue)
        ixs.forall { i => g.setGroup(i, grp) === g }
      }

    "do nothing if trying to replace the automatic group with an manual group" in
      forAll { (g: GuideEnvironment, m: ManualGroup) =>
        g.setGroup(0, GuideGroup(m)) === g
      }

    "do nothing if trying to replace a manual group with an automatic group" in
      forAll { (g: GuideEnvironment, a: AutomaticGroup) =>
        g.setGroup(1, GuideGroup(a)) === g  // either out of range or a manual group at 1
      }

    "replace the automatic group at index 0 if given an automatic group" in
      forAll { (g: GuideEnvironment, a: AutomaticGroup) =>
        g.setGroup(0, GuideGroup(a)).getGroup(0).getValue.grp === a
      }

    "replace a manual group at the corresponding index if given a manual group" in
      forAll { (g: GuideEnvironment, m: ManualGroup, i: Int) =>
        val ix = (i % g.getOptions.size).abs
        (ix === 0) || {
          g.setGroup(ix, GuideGroup(m)).getGroup(ix).getValue.grp === m
        }
      }

    "not change any group at any other indices" in
      forAll { (g: GuideEnvironment, a: AutomaticGroup, m: ManualGroup, i: Int) =>
        val ix  = (i % g.getOptions.size).abs
        val grp = (ix === 0) ? GuideGroup(a) | GuideGroup(m)
        val g2  = g.setGroup(ix, grp)

        import Indexable._

        g.getOptions.asScalaList.deleteAt(ix) === g2.getOptions.asScalaList.deleteAt(ix)
      }
  }

  "GuideEnvironment setOptions" should {
    "add an automatic group if the first element isn't automatic" in
      forAll { (g: GuideEnvironment, ms: List[ManualGroup]) =>
        val manualGroups = ms.map(GuideGroup)
        val g2 = g.setOptions(manualGroups.asImList)
        g2.getOptions.asScalaList === (GuideGroup(AutomaticGroup.Initial) :: manualGroups)
      }

    "convert any additional automatic groups to manual" in
      forAll { (g: GuideEnvironment, grps: List[GuideGroup]) =>
        val init = GuideGroup(AutomaticGroup.Initial)
        val g2   = g.setOptions((init :: grps).asImList)
        g2.getOptions.asScalaList === (init :: grps.map(g => GuideGroup.Grp.set(g, g.grp.toManualGroup)))
      }

    "keep the primary index the same, if the new options have enough elements to include it" in
      forAll { (g: GuideEnvironment, grps: List[GuideGroup]) =>
        val init = GuideGroup(AutomaticGroup.Initial)
        val g2   = g.setOptions((init :: grps).asImList)

        val oldPrimary = g.getPrimaryIndex.intValue()
        val newPrimary = g2.getPrimaryIndex.intValue()
        val lastIndex  = grps.size // since the initial auto group is not counted
        (oldPrimary === newPrimary) ||
          ((oldPrimary > lastIndex) && (newPrimary === lastIndex))
      }
  }

  "GuideEnvironment setPrimary" should {
    "update the automatic group if given an automatic group" in
      forAll { (g: GuideEnvironment, a: AutomaticGroup) =>
        val g2 = g.setPrimary(GuideGroup(a))
        (g2.getPrimaryIndex.intValue === 0) && (g2.getPrimary.grp === a)
      }

    "add a manual group if given a manual group when there is no primary manual group" in
      forAll { (g: GuideEnvironment, m: ManualGroup) =>
        val mg = GuideGroup(m)
        val p1 = g.getPrimaryIndex.intValue    // initial guide environment primary
        val m1 = g.getOptions.tail             // manuals for the initial guide env

        val g2 = g.setPrimary(mg)
        val p2 = g2.getPrimaryIndex.intValue   // primary in the updated guide env
        val m2 = g2.getOptions.tail            // manuals for the updated guide env

        if (p1 === 0) {
          // Adds a new manual group
          (m1.append(mg).asScalaList === m2.asScalaList) && (p2 === m2.size())
        } else {
          // Replaces the existing primary manual group
          (m1.updated(p1 - 1, mg).asScalaList === m2.asScalaList) && (p2 === p1)
        }
      }
  }

  "GuideEnvironment setPrimaryIndex" should {
    "select the corresponding group if in range, do nothing otherwise" in
      forAll { (g: GuideEnvironment, i: Int) =>
        val sz = g.getOptions.size
        val ix = (i % sz).abs
        val inRange = (i >= 0) && (i < sz)

        (g.setPrimaryIndex(ix).getPrimaryIndex.intValue === ix) &&
          (inRange || g.setPrimaryIndex(i).getPrimaryIndex.intValue === g.getPrimaryIndex.intValue)
      }
  }

  "GuideEnvironment" should {
    "be PIO Externalizable" in
      forAll { (g: GuideEnvironment) =>
        g ~= GuideEnvironment.fromParamSet(g.getParamSet(new PioXmlFactory()))
      }


    "be Serializable" in
      forAll { (g: GuideEnvironment) =>
        canSerializeP(g)(_ ~= _)
      }
  }
}
