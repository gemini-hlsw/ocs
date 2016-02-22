package edu.gemini.spModel.target.env

import scala.language.higherKinds
import scalaz._, Scalaz._

import OptsList._

/**
 * A non-empty list of elements of type `A`, where optionally one element is
 * said to have the focus.  In other words, it supports an optional selection.
 *
 * Maintains a `\/` where the left value (a non-empty list) represents no-focus
 * and the right value (a zipper) contains a focus.
 */
case class OptsList[A](toDisjunction: OneAndList[A] \/ Zipper[A]) {

  def clearFocus: OptsList[A] =
    OptsList(toDisjunction.flatMap { z =>
      val s = z.start
      OneAnd(s.focus, s.toList.tail).left[Zipper[A]]
    })

  def contains(a: A): Boolean =
    toList.contains(a)

  def focus: Option[A] =
    toDisjunction.toOption.map(_.focus)

  def focusIndex: Option[Int] =
    toDisjunction.toOption.map(_.lefts.length)

  /** Moves or sets the focus to `a` if `a` is a member of the options list and
    * returns the updated `OptsList` in a `Some`. Otherwise, returns `None`.
    */
  def focusOn(a: A)(implicit ev: Equal[A]): Option[OptsList[A]] =
    toList.span(_ =/= a) match {
      case (_, Nil)                 => none
      case (lefts, focus :: rights) => some(focused(lefts, focus, rights))
    }

  /** Sets the focus element to the element at the given index if in range,
    * returning a new `OptsList` wrapped in a `Some`.  Otherwise, returns `None`.
    */
  def focusOnIndex(i: Int): Option[OptsList[A]] =
    if (i < 0) none
    else toList.splitAt(i) match {
      case (_, Nil)                 => none
      case (lefts, focus :: rights) => some(focused(lefts, focus, rights))
    }

  def hasFocus: Boolean =
    toDisjunction.isRight

  def length: Int =
    toDisjunction.fold(_.tail.length + 1, _.length)

  def map[B](f: A => B): OptsList[B] =
    OptsList(toDisjunction.bimap(_.map(f), _.map(f)))

  def toNel: NonEmptyList[A] =
    toDisjunction match {
      case -\/(OneAnd(a, as)) =>
        NonEmptyList.nel(a, as)
      case \/-(z) =>
        val s = z.start
        NonEmptyList.nel(s.focus, s.rights.toList)
    }

  def toList: List[A] =
    toDisjunction match {
      case -\/(l) => l.toList
      case \/-(z) => z.toList
    }

  def traverse[G[_]: Applicative, B](f: A => G[B]): G[OptsList[B]] =
    toDisjunction match {
      case -\/(l) => l.traverse(f).map(l => OptsList(l.left))
      case \/-(r) => r.traverse(f).map(r => OptsList(r.right))
    }

  /** Deletes all occurrences of `a` in the options list. */
  def delete(a: A)(implicit ev: Equal[A]): Option[OptsList[A]] =
    toDisjunction match {
      case -\/(l) => l.toList.filter(_ =/= a).toNel.map(nel => OptsList.unfocused(nel))
      case \/-(z) =>
        val l  = z.lefts.filter(_ =/= a)
        val r  = z.rights.filter(_ =/= a)
        val z0 = Zipper(l, z.focus, r)
        (if (z0.focus === a) z0.delete else Some(z0)).map(zip => OptsList(zip.right))
    }

  /** Pairs each element with a boolean indicating whether that element has focus. */
  def withFocus: OptsList[(A, Boolean)] =
    toDisjunction match {
      case -\/(o) => OptsList(o.strengthR(false).left)
      case \/-(z) => OptsList(z.withFocus.right)
    }
}

object OptsList {
  implicit val TraverseOptsList: Traverse[OptsList] =
    new Traverse[OptsList] {
      def traverseImpl[G[_]: Applicative, A, B](fa: OptsList[A])(f: A => G[B]): G[OptsList[B]] =
        fa.toDisjunction match {
          case -\/(l) => l.traverse(f).map(l => OptsList(l.left))
          case \/-(r) => r.traverse(f).map(r => OptsList(r.right))
        }

      override def map[A, B](fa: OptsList[A])(f: A => B): OptsList[B] =
        fa.map(f)
    }

  /** Creates a singleton `OptsList` focused on the single element. */
  def focused[A](a: A): OptsList[A] =
    focused(Nil, a, Nil)

  /** Creates a focused `OptsList` from two lists and a focus element.  Note
    * that unlike the corresponding `Zipper` constructor, the left values are
    * specified in order.
    */
  def focused[A](lefts: List[A], focus: A, rights: List[A]): OptsList[A] =
    OptsList(Zipper(lefts.reverse.toStream, focus, rights.toStream).right)

  /** Creates a singleton `OptsList` without a foucs. */
  def unfocused[A](a: A): OptsList[A] =
    unfocused(a, Nil)

  def unfocused[A](a: A, as: List[A]): OptsList[A] =
    OptsList(OneAnd(a, as).left)

  def unfocused[A](nel: NonEmptyList[A]): OptsList[A] =
    unfocused(nel.head, nel.tail)

  /** Creates an `OptsList` provided that the given `List` has at least one
    * element.
    */
  def unfocused[A](l: List[A]): Option[OptsList[A]] =
    l.toNel.map(nel => unfocused(nel))

  implicit def EqualOptsList[A : Equal]: Equal[OptsList[A]] = Equal.equal {
    case (OptsList(-\/(nel1)),OptsList(-\/(nel2))) => nel1 === nel2
    case (OptsList(\/-(zip1)),OptsList(\/-(zip2))) => zip1 === zip2
    case _                                         => false
  }

  import Indexable._

  implicit val IndexableOptsList: Indexable[OptsList] = new Indexable[OptsList] {
    override def modifyAtIf[A](o: OptsList[A], i: Int)(g: A => Option[A]): Option[(A, OptsList[A])] =
      o.toDisjunction.bitraverse(_.modifyAtIf(i)(g), _.modifyAtIf(i)(g)).map {
        case -\/((a, l)) => (a, OptsList(-\/(l)))
        case \/-((a, z)) => (a, OptsList(\/-(z)))
      }
  }
}
