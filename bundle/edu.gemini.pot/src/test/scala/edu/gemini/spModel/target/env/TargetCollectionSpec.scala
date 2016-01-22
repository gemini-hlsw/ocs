package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.AlmostEqual
import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetCollection.TargetCollectionSyntax

import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import org.scalacheck.Prop
import org.scalacheck.Prop._

import scalaz._, Scalaz._

// Tests the target collection-ability of guide groups and environments
class TargetCollectionSpec extends Specification with ScalaCheck with Arbitraries with Almosts {

  def cloneProp[A : Arbitrary : TargetCollection : AlmostEqual]: Prop =
    forAll { (a0: A) =>
      val a1 = a0.cloneTargets
      (a0 ~= a1) && a0.targetList.zip(a1.targetList).forall { case (t0, t1) => t0 =/= t1 }
    }

  def doesNotContainProp[A : Arbitrary : TargetCollection]: Prop =
    forAll { (a: A, t: SPTarget) => !a.containsTarget(t) }  // Note, SPTarget reference equality

  def doesContainProp[A : Arbitrary : TargetCollection]: Prop =
    forAll { (a: A) => a.targetList.forall(a.containsTarget) }

  def containsProp[A : Arbitrary : TargetCollection]: Prop =
    all(
      "not contain" |: doesNotContainProp[A],
      "contain"     |: doesContainProp[A]
    )

  //noinspection MutatorLikeMethodIsParameterless
  def removeNothingProp[A : Arbitrary : TargetCollection : Equal]: Prop =
    forAll { (a0: A, t: SPTarget) =>
      val a1 = a0.removeTarget(t)
      a0 === a1
    }

  //noinspection MutatorLikeMethodIsParameterless
  def removeSomethingProp[A : Arbitrary : TargetCollection: Equal]: Prop =
    forAll { (a0: A, i: Int) =>
      val ts = a0.targetList
      ts.isEmpty || {
        val rm = ts((i % ts.size).abs)  // pick a target at random
        val a1 = a0.removeTarget(rm)
        ts.filterNot(_ === rm) === a1.targetList
      }
    }

  //noinspection MutatorLikeMethodIsParameterless
  def removeProp[A : Arbitrary : TargetCollection : Equal]: Prop =
    all(
      "remove nothing"   |: removeNothingProp[A],
      "remove something" |: removeSomethingProp[A]
    )

  "cloneTargets" should {
    "create a new GuideGroup with cloned SPTargets but otherwise equivalent in structure" in all(
      "GuideGrp"         |: cloneProp[GuideGrp],
      "GuideGroup"       |: cloneProp[GuideGroup],
      "GuideEnv"         |: cloneProp[GuideEnv],
      "GuideEnvironment" |: cloneProp[GuideEnvironment]
    )
  }

  "containsTargets" should {
    "return true iff the collection contains the given target" in all(
      "GuideGrp"         |: containsProp[GuideGrp],
      "GuideGroup"       |: containsProp[GuideGroup],
      "GuideEnv"         |: containsProp[GuideEnv],
      "GuideEnvironment" |: containsProp[GuideEnvironment]
    )
  }

  "removeTargets" should {
    "remove a given target if present in the collection but do nothing otherwise" in all(
      "GuideGrp"         |: removeProp[GuideGrp],
      "GuideGroup"       |: removeProp[GuideGroup],
      "GuideEnv"         |: removeProp[GuideEnv],
      "GuideEnvironment" |: removeProp[GuideEnvironment]
    )
  }

}
