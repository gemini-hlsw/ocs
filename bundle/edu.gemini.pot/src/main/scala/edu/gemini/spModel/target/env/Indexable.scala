package edu.gemini.spModel.target.env

import scala.annotation.tailrec
import scala.language.higherKinds
import scalaz._, Scalaz._

/** A typeclass for working with containers that can be viewed as an ordered
  * sequence of elements.
  */
trait Indexable[F[_]] {
  /** Returns an updated copy of the given container with the element at
    * position `i` potentially modified according to the funtion `g`.
    *
    * @param f container
    * @param i index of the element to retrieve or modify
    * @param g modification function; if a `Some` is returned, the element at
    *          the corresponding index is replaced with the value.  if `None`,
    *          the element is deleted
    * @tparam A type of the elements in the container
    * @return If the index `i` is in range, `Some` wrapping a tuple of the value
    *         of the element at index `i` before the modification function is
    *         applied, and the container with the updated element after the
    *         function has been applied.  If the index is out of range or if
    *         the container cannot be constructed from the value after applying
    *         `g`, `None`.
    */
  def modifyAtIf[A](f: F[A], i: Int)(g: A => Option[A]): Option[(A, F[A])]
}

object Indexable {

  /** Commonly used operations when working with an `Indexable` container. */
  implicit class IndexableSyntax[F[_], A](v: F[A]) {
    def deleteAt(i: Int)(implicit I: Indexable[F]): Option[F[A]] =
      I.modifyAtIf(v, i)(_ => None).map(_._2)

    def getAt(i: Int)(implicit I: Indexable[F]): Option[A] =
      I.modifyAtIf(v, i)(a => Some(a)).map(_._1)

    def filterAt(i: Int)(p: A => Boolean)(implicit I: Indexable[F]): Option[F[A]] =
      I.modifyAtIf(v, i) { a => p(a) option a }.map(_._2)

    def modifyAt(i: Int)(g: A => A)(implicit I: Indexable[F]): Option[F[A]] =
      I.modifyAtIf(v, i)(a => Some(g(a))).map(_._2)

    def modifyAtIf(i: Int)(g: A => Option[A])(implicit I: Indexable[F]): Option[(A, F[A])] =
      I.modifyAtIf(v, i)(g)

    def setAt(i: Int, a: A)(implicit I: Indexable[F]): Option[F[A]] =
      I.modifyAtIf(v, i)(_ => Some(a)).map(_._2)
  }

  /** A trait that abstracts the parts of `List` and `Stream` needed to make
    * a straightforward `Indexable` implementation.  Other options would be to
    * implement the methods in terms of the `List`/`Stream` `patch` method or
    * `splitAt` method.  The problem with `patch` is that there's no indication
    * when the index is out of bounds.  The problem with `splitAt` is that you
    * have to do an append to splice together two lists/streams.
    */
  private sealed trait Patchable[F[_]] {
    def zero[A]: F[A]
    def cons[A](a: A, r: F[A]): F[A]
    def uncons[A](r: F[A]): Option[(A, F[A])]
  }

  private def fromPatchable[F[_]](p: Patchable[F]): Indexable[F] =
    new Indexable[F] {
      // Replaces the element at index i if the function g applied to the
      // element returns a Some.  Otherwise, deletes the element.
      override def modifyAtIf[A](f: F[A], i: Int)(g: A => Option[A]): Option[(A, F[A])] = {
        @tailrec
        def pop(reversePrefix: F[A], suffix: F[A]): F[A] =
          p.uncons(reversePrefix) match {
            case None         => suffix
            case Some((h, t)) => pop(t, p.cons(h, suffix))
          }

        @tailrec
        def go(rem: F[A], stack: F[A], i0: Int): Option[(A, F[A])] =
          (i0, p.uncons(rem)) match {
            case (0, Some((h, t)))          =>
              g(h) match {
                case None     => Some((h, pop(stack, t)))
                case Some(h0) => Some((h, pop(stack, p.cons(h0, t))))
              }
            case (n, Some((h, t))) if n > 0 =>
              go(t, p.cons(h, stack), n - 1)

            case _                          =>
              None
          }

        go(f, p.zero, i)
      }
    }


  implicit val IndexableList: Indexable[List] = fromPatchable(new Patchable[List] {
    def zero[A]: List[A]                   = List.empty[A]
    def cons[A](a: A, l: List[A]): List[A] = a :: l

    def uncons[A](l: List[A]): Option[(A, List[A])] =
      l.headOption.map { a => (a, l.tail) }
  })

  implicit val IndexableStream: Indexable[Stream] = fromPatchable(new Patchable[Stream] {
    def zero[A]: Stream[A]                     = Stream.empty[A]
    def cons[A](a: A, s: Stream[A]): Stream[A] = a #:: s

    def uncons[A](s: Stream[A]): Option[(A, Stream[A])] =
      s.headOption.map { a => (a, s.tail) }
  })

  implicit val IndexableOneAndList: Indexable[OneAndList] = new Indexable[OneAndList] {
    def fromList[A](l: List[A]): Option[OneAndList[A]] =
      l match {
        case h :: t => Some(OneAnd[List, A](h, t))
        case _      => None
      }

    override def modifyAtIf[A](o: OneAndList[A], i: Int)(g: A => Option[A]): Option[(A, OneAndList[A])] =
      o.toList.modifyAtIf(i)(g).flatMap { case (a, lst) => fromList(lst).strengthL(a) }
  }

  implicit val IndexableZipper: Indexable[Zipper] = new Indexable[Zipper] {
    def left[A](z: Zipper[A], i: Int): Int  = z.index - i - 1  // lefts are stored in reverse order
    def right[A](z: Zipper[A], i: Int): Int = i - z.index - 1  // rights are stored in normal order

    override def modifyAtIf[A](z: Zipper[A], i: Int)(g: A => Option[A]): Option[(A, Zipper[A])] =
      if (i === z.index)
        g(z.focus).fold(z.delete.map(z0 => (z.focus, z0))) { a0 =>
          Some((z.focus, z.modify(_ => a0)))
        }
      else if (i < z.index)
        z.lefts.modifyAtIf(z.index - i - 1)(g).map(_.map(Zipper(_, z.focus, z.rights)))
      else
        z.rights.modifyAtIf(i - z.index - 1)(g).map(_.map(Zipper(z.lefts, z.focus, _)))
  }
}