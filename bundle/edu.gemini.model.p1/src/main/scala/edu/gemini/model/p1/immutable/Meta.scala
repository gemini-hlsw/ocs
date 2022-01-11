package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.io.File

import scalaz._
import Scalaz._
import scala.collection.JavaConverters._

object Meta {

  // Lenses
  val attachments: Lens[Meta, List[Attachment]] = Lens.lensu((a, b) => a.copy(attachments = b), _.attachments)
  val band3OptionChosen: Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(band3OptionChosen = b), _.band3OptionChosen)
  val overrideAffiliate: Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(overrideAffiliate = b), _.overrideAffiliate)
  val firstAttachment: Lens[Meta, Option[Attachment]] = Lens.lensu((a, b) => b.map(r => a.copy(attachments = r :: a.attachments.filterNot(_.index == 1))).getOrElse(a), _.attachments.find(_.index == 1))
  val secondAttachment: Lens[Meta, Option[Attachment]] = Lens.lensu((a, b) => b.map(r => a.copy(attachments = r :: a.attachments.filterNot(_.index == 2))).getOrElse(a), _.attachments.find(_.index == 2))

  val empty = Meta(Nil, band3OptionChosen = false, overrideAffiliate = false)
  def apply(m: M.Meta): Meta = Option(m).map(new Meta(_)).getOrElse(empty)
}

case class Meta(attachments: List[Attachment], band3OptionChosen: Boolean, overrideAffiliate: Boolean) {

  private def this(m: M.Meta) = this(m.getAttachment.asScala.map(Attachment.apply).toList,
    Option(m.isBand3OptionChosen).exists(_.booleanValue),
    Option(m.isOverrideAffiliate).exists(_.booleanValue))

  def mutable = {
    val m = Factory.createMeta
    attachments.map(a => m.getAttachment().add(a.mutable))
    m.setBand3OptionChosen(band3OptionChosen)
    m.setOverrideAffiliate(overrideAffiliate)
    m
  }
}

object Attachment {

  // Lenses
  val name: Lens[Attachment, Option[File]] = Lens.lensu((a, b) => a.copy(name = b), _.name)
  val index: Lens[Attachment, Int] = Lens.lensu((a, b) => a.copy(index = b), _.index)

  val empty = Attachment(None, index = 0)
  def apply(m: M.Attachment):Attachment = Option(m).map(new Attachment(_)).getOrElse(empty)
}

case class Attachment(name: Option[File], index: Int) {

  private def this(m: M.Attachment) = this(Option(m.getName).map(new File(_)), m.getIndex.toInt)

  def mutable = {
    val m = Factory.createAttachment
    m.setName(name.map(_.getPath).orNull)
    m.setIndex(index)
    m
  }
}

