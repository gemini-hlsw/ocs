package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.target.env.TargetCollection.TargetCollectionSyntax

import edu.gemini.shared.util.immutable.{DefaultImList, ImList, ImOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget
import org.apache.commons.io.output.ByteArrayOutputStream

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop._

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}

import scala.collection.JavaConverters._
import scalaz._, Scalaz._


class GuideEnvironmentSpec extends Specification with ScalaCheck with Arbitraries with Almosts {

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
        val s0 = toScala(g.getPrimary.getValue.getPrimaryReferencedGuiders)
        val s1 = toScala(g.getPrimaryReferencedGuiders)
        s0 === s1
      }
  }

  /*
  val GenTargets: Gen[(GuideProbe, List[SPTarget])] =
    for {
      gp <- arbitrary[GuideProbe]
      ts <- boundedListOf[SPTarget](3)
    } yield (gp, ts)
    */

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

  /*
  "GuideEnvironment removeGroup" should {
    "do nothing if the group is automatic" in {
      forAll { (g: GuideEnvironment) =>
        val auto = g.getOptions.head
        auto.grp match {
          case _: AutomaticGroup => g.removeGroup(auto) === g
          case _                 => false
        }
      }
    }

    "remove the manual options altogether if the last one is removed" in
      forAll { (g0: GuideEnvironment) =>
        g0.getOptions.asScalaList match {
          case (_ :: m :: Nil) =>
            val g1 = g0.removeGroup(m)
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
            val m  = ms((i % ms.size).abs)  // pick a manual group
            val g1 = g0.removeGroup(m)
            g1.getOptions.asScalaList.tail === ms.filter(_ =/= m)
          case _              =>
            true
        }
      }

    "keep the same focus unless removing the focused group" in
      forAll { (g0: GuideEnvironment, i: Int) =>
        g0.getOptions.asScalaList match {
          case (_ :: h :: t) =>  // auto :: first_manual :: other_manuals
            val ms = h :: t
            val m  = ms((i % ms.size).abs)  // pick a manual group
            val f0 = g0.getPrimary.getValue // TODO: remove the option wrapper
            val g1 = g0.removeGroup(m)
            val f1 = g1.getPrimary.getValue

            // removing the focused group or else the focus is the same after
            // the removal
            (f0 === m) || (f0 === f1)
          case _              =>
            true
        }
      }

    "when removing the primary, move the primary to the right unless at the end in which case to the left" in
      forAll { (g0: GuideEnvironment, i: Int) =>
        g0.getOptions.asScalaList match {
          case (_ :: h :: t) =>  // auto :: first_manual :: other_manuals
            val ms = h :: t
            val m  = ms((i % ms.size).abs)  // pick a manual group
            val f0 = g0.getPrimary.getValue // TODO: remove the option wrapper
            val g1 = g0.removeGroup(m)
            val f1 = g1.getPrimary.getValue

            (f0 =/= m) || {
               // focus should have changed
              (f0 =/= f1) && {
                val i0 = g0.getPrimaryIndex.intValue
                val i1 = g1.getPrimaryIndex.intValue
                (i0 === i1) || // but moved to the right if possible (i.e., same index)
                  ((i0 === ms.size) && (i1 === (i0 - 1))) // or one to the left if at the end
              }
            }
          case _              =>
            true
        }
      }
  }
  */


  "GuideEnvironment" should {
    "be PIO Externalizable" in
      forAll { (g: GuideEnvironment) =>
        g ~= GuideEnvironment.fromParamSet(g.getParamSet(new PioXmlFactory()))
      }


    "be Serializable" in
      forAll { (g: GuideEnvironment) =>
        val bao = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(bao)
        oos.writeObject(g)
        oos.close()

        val ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray))
        ois.readObject() match {
          case g2: GuideEnvironment => g ~= g2
          case _                    => false
        }
      }
  }


}
