package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe

import java.io.IOException

import scalaz._
import Scalaz._

final case class GuideEnv(auto: AutomaticGroup, manual: List[ManualGroup] \/ Zipper[ManualGroup]) {

  def groups: List[GuideGrp] =
    auto :: manual.fold(identity, _.toList)

  def primaryGroup: GuideGrp =
    manual.fold(_ => auto, _.focus)

  def referencedGuiders: Set[GuideProbe] =
    groups.foldMap(_.referencedGuiders)

  def primaryReferencedGuiders: Set[GuideProbe] =
    primaryGroup.referencedGuiders

  def writeObject(out: java.io.ObjectOutputStream): Unit = {
    auto.writeObject(out)
    def writeManualList(lst: List[ManualGroup]): Unit = {
      out.writeInt(new java.lang.Integer(lst.size))
      lst.foreach { _.writeObject(out) }
    }

    manual match {
      case -\/(lst) =>
        out.writeObject(GuideEnv.LeftTag)
        writeManualList(lst)
      case \/-(zip) =>
        out.writeObject(GuideEnv.RightTag)
        writeManualList(zip.lefts.toList)
        zip.focus.writeObject(out)
        writeManualList(zip.rights.toList)
    }
  }
}

/** A guide environment is a bags group (possibly empty or "initial") followed
  * by zero or more manual groups. One is always selected. If the second
  * element in the pair is a list, it means the bags group is selected.
  * Otherwise the selection is indicated by the zipper.
  */
object GuideEnv {
  val initial: GuideEnv = GuideEnv(AutomaticGroup.Initial, Nil.left)

  private sealed trait SerializationTag
  private case object LeftTag  extends SerializationTag
  private case object RightTag extends SerializationTag

  def readObject(in: java.io.ObjectInputStream): GuideEnv = {
    val auto = GuideGrp.readObject(in) match {
      case ag: AutomaticGroup => ag
      case _                  => throw new IOException("Expecting automatic group")
    }

    val tag = safeRead(in) { case s: SerializationTag => s }

    def readManualGroup: ManualGroup =
      GuideGrp.readObject(in) match {
        case mg: ManualGroup => mg
        case _               => throw new IOException("Expecting manual group")
      }

    def readManualList: List[ManualGroup] = {
      val size = safeRead(in) { case i: Integer => i }
      val mgs  = (0 to size).map { _ => readManualGroup }
      mgs.toList
    }

    tag match {
      case LeftTag  =>
        GuideEnv(auto, readManualList.left)

      case RightTag =>
        val l = readManualList
        val f = readManualGroup
        val r = readManualList
        GuideEnv(auto, Zipper(l.toStream, f, r.toStream).right)
    }
  }
}