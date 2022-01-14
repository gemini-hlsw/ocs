package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.io.File

import scalaz._
import Scalaz._
import scala.collection.JavaConverters._

object Meta {

  // Lenses
  val band3OptionChosen: Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(band3OptionChosen = b), _.band3OptionChosen)
  val overrideAffiliate: Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(overrideAffiliate = b), _.overrideAffiliate)
  val firstAttachment: Lens[Meta, Option[File]] =
    Lens.lensu((a, b) => a.copy(firstAttachment = b), _.firstAttachment)
  val secondAttachment: Lens[Meta, Option[File]] =
    Lens.lensu((a, b) => a.copy(secondAttachment = b), _.secondAttachment)

  def isDARP(p: ProposalClass): Boolean = attachmentsForType(p) == 2

  def attachmentsForType(p: ProposalClass): Int = p match {
    case _: FastTurnaroundProgramClass => 1
    case _                             => 2
  }

  def lensForAttachment(i: Int): Lens[Meta, Option[File]] = i match {
    case 1 => firstAttachment
    case 2 => secondAttachment
    case _ => sys.error("Only up to two attachments allowed")
  }

  val empty = Meta(firstAttachment = None, secondAttachment = None, band3OptionChosen = false, overrideAffiliate = false)
  def apply(m: M.Meta): Meta = Option(m).map(new Meta(_)).getOrElse(empty)
}

case class Meta(firstAttachment: Option[File], secondAttachment: Option[File], band3OptionChosen: Boolean, overrideAffiliate: Boolean) {

  private def this(m: M.Meta) = this(
    Option(m.getFirstAttachment).map(new File(_)),
    Option(m.getSecondAttachment).map(new File(_)),
    Option(m.isBand3OptionChosen).exists(_.booleanValue),
    Option(m.isOverrideAffiliate).exists(_.booleanValue))

  def mutable = {
    val m = Factory.createMeta
    m.setFirstAttachment(firstAttachment.map(_.getName).orNull)
    m.setSecondAttachment(secondAttachment.map(_.getName).orNull)
    m.setBand3OptionChosen(band3OptionChosen)
    m.setOverrideAffiliate(overrideAffiliate)
    m
  }
}

