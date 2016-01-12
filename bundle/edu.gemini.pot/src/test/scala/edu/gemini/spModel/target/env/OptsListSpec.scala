package edu.gemini.spModel.target.env

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._, Scalaz._

class OptsListSpec extends Specification with ScalaCheck with Arbitraries {

  "OptsList" should {
    "have no focus after clearFocus" in
      forAll { (opts: OptsList[Int]) =>
        !opts.clearFocus.hasFocus
      }

    "contain an element iff the nel or zipper contains the element" in
      forAll { (opts: OptsList[Int], i: Int) =>
        opts.contains(i) == opts.toDisjunction.fold(_.toList.contains(i), _.toStream.contains(i))
      }

    "have a focus equal to the zipper focus (if any)" in
      forAll { (opts: OptsList[Int]) =>
        opts.focus == opts.toDisjunction.toOption.map(_.focus)
      }

    "have a focus element which is the same as the element at the focus index" in
      forAll { (opts: OptsList[Int]) =>
        opts.focus == opts.focusIndex.map { opts.toList }
      }

    
  }
}
