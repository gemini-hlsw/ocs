package edu.gemini.spModel.target.env

import scala.annotation.tailrec
import scala.language.higherKinds
import scalaz._, Scalaz._


/** A typeclass for working with containers that can be viewed as an ordered
  * sequence of elements.
  */
trait Indexable[F[_]] {
  def deleteAt[A](f: F[A], i: Int): Option[F[A]]
  def elementAt[A](f: F[A], i: Int): Option[A]
  def modifyAt[A](f: F[A], i: Int)(g: A => A): Option[F[A]]
}

object Indexable {

  implicit class IndexableSyntax[F[_], A](v: F[A]) {
    def deleteAt(i: Int)(implicit I: Indexable[F]): Option[F[A]] =
      I.deleteAt(v, i)

    def elementAt(i: Int)(implicit I: Indexable[F]): Option[A] =
      I.elementAt(v, i)

    def modifyAt(i: Int)(g: A => A)(implicit I: Indexable[F]): Option[F[A]] =
      I.modifyAt(v, i)(g)

    def setAt(i: Int, a: A)(implicit I: Indexable[F]): Option[F[A]] =
      modifyAt(i)(_ => a)
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
      @tailrec
      override def elementAt[A](f: F[A], i: Int): Option[A] =
        (i, p.uncons(f)) match {
          case (0, Some((h, _)))           => Some(h)
          case (n, Some((_, t))) if n >= 0 => elementAt(t, n - 1)
          case _                           => None
        }

      private def modIf[A](f: F[A], i: Int)(g: A => Option[A]): Option[F[A]] = {
        @tailrec
        def pop(reversePrefix: F[A], suffix: F[A]): F[A] =
          p.uncons(reversePrefix) match {
            case None         => suffix
            case Some((h, t)) => pop(t, p.cons(h, suffix))
          }

        @tailrec
        def go(rem: F[A], stack: F[A], i0: Int): Option[F[A]] =
          (i0, p.uncons(rem)) match {
            case (0, Some((h, t)))          =>
              g(h) match {
                case None     => Some(pop(stack, t))
                case Some(h0) => Some(pop(stack, p.cons(h0, t)))
              }
            case (n, Some((h, t))) if n > 0 =>
              go(t, p.cons(h, stack), n - 1)

            case _                          =>
              None
          }

        go(f, p.zero, i)
      }

      override def deleteAt[A](f: F[A], i: Int): Option[F[A]] =
        modIf(f, i)(_ => None)

      override def modifyAt[A](f: F[A], i: Int)(g: (A) => A): Option[F[A]] =
        modIf[A](f, i)(a => Some(g(a)))
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

    def viaList[A](o: OneAndList[A], f: List[A] => Option[List[A]]): Option[OneAndList[A]] =
      f(o.toList).flatMap(fromList)

    override def elementAt[A](o: OneAndList[A], i: Int): Option[A] =
      o.toList.elementAt(i)

    override def modifyAt[A](o: OneAndList[A], i: Int)(g: (A) => A): Option[OneAndList[A]] =
      viaList(o, _.modifyAt(i)(g))

    override def deleteAt[A](o: OneAndList[A], i: Int): Option[OneAndList[A]] =
      viaList(o, _.deleteAt(i))
  }

  implicit val IndexableZipper: Indexable[Zipper] = new Indexable[Zipper] {
    def left[A](z: Zipper[A], i: Int): Int  = z.index - i - 1  // lefts are stored in reverse order
    def right[A](z: Zipper[A], i: Int): Int = i - z.index - 1  // rights are stored in normal order

    override def elementAt[A](z: Zipper[A], i: Int): Option[A] =
      if (i === z.index)    Some(z.focus)
      else if (i < z.index) z.lefts.elementAt(left(z, i))
      else                  z.rights.elementAt(right(z, i))

    override def modifyAt[A](z: Zipper[A], i: Int)(g: (A) => A): Option[Zipper[A]] =
      if (i === z.index)    Some(z.modify(g))
      else if (i < z.index) z.lefts.modifyAt(left(z, i))(g).map(lefts    => Zipper(lefts, z.focus, z.rights))
      else                  z.rights.modifyAt(right(z, i))(g).map(rights => Zipper(z.lefts, z.focus, rights))

    override def deleteAt[A](z: Zipper[A], i: Int): Option[Zipper[A]] =
      if (i === z.index)    z.delete
      else if (i < z.index) z.lefts.deleteAt(left(z, i)).map(lefts    => Zipper(lefts, z.focus, z.rights))
      else                  z.rights.deleteAt(right(z, i)).map(rights => Zipper(z.lefts, z.focus, rights))
  }
}