package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.env.ScalazSer._

import scalaz._, Scalaz._

case class OptsList[A](toDisjunction: List[A] \/ Zipper[A]) {

  def focus: Option[A] =
    toDisjunction.toOption.map(_.focus)

  def hasFocus: Boolean =
    toDisjunction.isRight

  def map[B](f: A => B): OptsList[B] =
    OptsList(toDisjunction.bimap(_.map(f), _.map(f)))

  def toList: List[A] =
    toDisjunction match {
      case -\/(l) => l
      case \/-(z) => z.toList
    }

  def traverse[G[_]: Applicative, B](f: A => G[B]): G[OptsList[B]] =
    toDisjunction match {
      case -\/(l) => l.traverse(f).map(l => OptsList(l.left))
      case \/-(r) => r.traverse(f).map(r => OptsList(r.right))
    }

  def writeObject(out: java.io.ObjectOutputStream)(writeA: A => Unit): Unit =
    writeDisjunction(out, toDisjunction)(writeList(out, _)(writeA), writeZipper(out, _)(writeA))
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

  def readObject[A](in: java.io.ObjectInputStream)(readA:  => A): OptsList[A] =
    OptsList(readDisjunction(in)(readList(in)(readA), readZipper(in)(readA)))
}
