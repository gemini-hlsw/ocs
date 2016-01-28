package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.env.Indexable._

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._, Scalaz._

class OptsListSpec extends Specification with ScalaCheck with Arbitraries {

  "OptsList" should {
    "have no focus after clearFocus" in
      forAll { (opts: OptsList[Int]) =>
        val clearOpts = opts.clearFocus
        !clearOpts.hasFocus && clearOpts.focus.isEmpty && clearOpts.focusIndex.isEmpty
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

    "for an element in the list, have a focus element set by focusOn" in
      forAll { (opts: OptsList[Int]) =>
        opts.toList.forall(i => opts.focusOn(i).exists(_.focus.exists(_ == i)))
      }

    "for attempts to focusOn an element not in the list, return None" in
      forAll { (opts: OptsList[Int], i: Int) =>
        !opts.contains(i) ==> opts.focusOn(i).isEmpty
      }

    "for an index in the list, have the element at that index focused on by focusOnIndex" in
      forAll { (opts: OptsList[Int]) =>
        Range(0, opts.length).forall(i => opts.focusOnIndex(i).exists(_.focus.exists(_ == opts.toList(i))))
      }

    "for attempts to focusOnIndex on an invalid index, return None" in
      forAll { (opts: OptsList[Int], i: Int) =>
        (i < 0 || i >= opts.length) ==> opts.focusOnIndex(i).isEmpty
      }

    "not contain an element deleted from the list" in
      forAll { (opts: OptsList[Int]) =>
        opts.toList.forall(i => opts.delete(i).forall(o => !o.contains(i)))
      }

    "have the same elements if it is converted to a non-empty list" in
      forAll { (opts: OptsList[Int]) =>
        opts.toList == opts.toNel.toList
      }

    "have a focus equal to its mapped value when a map operation is performed" in
      forAll { (opts: OptsList[Int], scale: Int) =>
        opts.map(_ * scale).focus == opts.focus.map(_ * scale)
      }

    "still have a focus if nonempty after the original focus was deleted" in
     forAll { (opts: OptsList[Int]) =>
       opts.focus.flatMap(opts.delete).forall(_.hasFocus)
     }

    "have length reduced by the number of times an element appears when that element is deleted" in
    forAll { (opts: OptsList[Int]) =>
      opts.toList.distinct.forall(elem =>
        opts.delete(elem).fold(0)(_.length) == opts.length - opts.toList.count(_ == elem))
    }
  }

  "OptsList deleteAt" should {
    "return None if the index is out of range" in
      forAll { (opts: OptsList[Int]) =>
        List(Int.MinValue, -2, -1, opts.length, opts.length + 1, Int.MaxValue).forall { i =>
          opts.deleteAt(i).isEmpty
        }
      }

    "return None if the last element is deleted" in
      forAll { (opts: OptsList[Int]) =>
        opts.deleteAt(0).fold(opts.length == 1) { _ => opts.length > 1 }
      }

    "delete the indicated element if in range" in
      forAll { (opts: OptsList[Int], i: Int) =>
        val index = (i % opts.length).abs
        opts.deleteAt(index).fold(opts.length === 1) { opts2 =>
          val expected = opts.toList.zipWithIndex.filterNot(_._2 === index).unzip._1
          val actual   = opts2.toList
          expected === actual
        }
      }

    "maintain the same focus, unless deleting the focus" in
      forAll { (opts: OptsList[Int], i: Int) =>
        val index = (i % opts.length).abs
        opts.deleteAt(index).fold(opts.length === 1) { opts2 =>
          opts.focusIndex.forall { fi =>
            (index === fi) || opts.focus === opts2.focus
          }
        }
      }

    "move the focus to right when deleting the focus, unless at the end" in
      forAll { (opts: OptsList[Int]) =>
        opts.focusIndex.forall { fi =>
          opts.deleteAt(fi).forall { opts2 =>
            val last = opts.length - 1
            val expected = if (fi === last) last - 1 else fi
            opts2.focusIndex.exists((_ === expected))
          }
        }
      }
  }
}
