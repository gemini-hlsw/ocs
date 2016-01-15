package edu.gemini.spModel.target.env

import scalaz._, Scalaz._

import OptsList._

case class OptsList[A](toDisjunction: NonEmptyList[A] \/ Zipper[A]) {

  def clearFocus: OptsList[A] =
    OptsList(toDisjunction.flatMap { z =>
      val s = z.start
      NonEmptyList.nel(s.focus, s.toList.tail).left
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
  def focusOn(a: A): Option[OptsList[A]] =
    toList.span(_ != a) match {
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
    toDisjunction.fold(_.length, _.length)

  def map[B](f: A => B): OptsList[B] =
    OptsList(toDisjunction.bimap(_.map(f), _.map(f)))

  def toNel: NonEmptyList[A] =
    toDisjunction match {
      case -\/(l) =>
        l
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
  def delete(a: A): Option[OptsList[A]] =
    toDisjunction match {
      case -\/(l) => l.toList.filter(_ != a).toNel.map(nel => OptsList(nel.left))
      case \/-(z) =>
        val l  = z.lefts.filter(_ != a)
        val r  = z.rights.filter(_ != a)
        val z0 = Zipper(l, z.focus, r)
        (if (z0.focus == a) z0.delete else Some(z0)).map(zip => OptsList(zip.right))
    }

  /** Pairs each element with a boolean indicating whether that element has focus. */
  def withFocus: OptsList[(A, Boolean)] =
    toDisjunction match {
      case -\/(l) => OptsList(l.zip(NonEmptyList.nel(false, List.fill(l.length-1)(false))).left)
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
    }

  def focused[A](lefts: List[A], focus: A, rights: List[A]): OptsList[A] =
    OptsList(Zipper(lefts.reverse.toStream, focus, rights.toStream).right)

  def unfocused[A](nel: NonEmptyList[A]): OptsList[A] =
    OptsList(nel.left)

  def unfocused[A](l: List[A]): Option[OptsList[A]] =
    l.toNel.map(nel => unfocused(nel))

  implicit def EqualOptsList[A : Equal]: Equal[OptsList[A]] = Equal.equal {
    case (OptsList(-\/(nel1)),OptsList(-\/(nel2))) => nel1 === nel2
    case (OptsList(\/-(zip1)),OptsList(\/-(zip2))) => zip1 === zip2
    case _                                         => false
  }
}
