package edu.gemini.spModel.target.env

import java.io.{ObjectInputStream, ObjectOutputStream}

import scalaz._
import Scalaz._


private[env] object ScalazSer {
  sealed trait DisjunctionTag
  case object LeftTag  extends DisjunctionTag
  case object RightTag extends DisjunctionTag

  def readDisjunction[A,B](i: ObjectInputStream)(readA: => A, readB: => B): A \/ B = {
    val tag = safeRead(i) { case s: DisjunctionTag => s }

    tag match {
      case LeftTag  => readA.left
      case RightTag => readB.right
    }
  }

  def writeDisjunction[A,B](o: ObjectOutputStream, d: A \/ B)(writeA: A => Unit, writeB: B => Unit): Unit =
    d match {
      case -\/(a) =>
        o.writeObject(LeftTag)
        writeA(a)
      case \/-(b) =>
        o.writeObject(RightTag)
        writeB(b)
    }

  def readList[A](in: ObjectInputStream)(readA: => A): List[A] = {
    val size = safeRead(in) { case i: Integer => i }
    val as   = (0 to size).map { _ => readA }
    as.toList
  }

  def writeList[A](out: ObjectOutputStream, as: List[A])(writeA: A => Unit): Unit = {
    out.writeInt(new java.lang.Integer(as.size))
    as.foreach { writeA }
  }

  def readZipper[A](in: ObjectInputStream)(readA: => A): Zipper[A] = {
    val l = readList(in)(readA)
    val f = readA
    val r = readList(in)(readA)
    Zipper(l.toStream, f, r.toStream)
  }

  def writeZipper[A](out: ObjectOutputStream, z: Zipper[A])(writeA: A => Unit): Unit = {
    writeList(out, z.lefts.toList)(writeA)
    writeA(z.focus)
    writeList(out, z.rights.toList)(writeA)
  }
}
