package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.io.File

import scalaz._
import Scalaz._

object Meta {

  // Lenses
  val attachment:Lens[Meta,Option[File]] = Lens.lensu((a, b) => a.copy(attachment = b), _.attachment)
  val band3OptionChosen:Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(band3OptionChosen = b), _.band3OptionChosen)
  val overrideAffiliate:Lens[Meta,Boolean] = Lens.lensu((a, b) => a.copy(overrideAffiliate = b), _.overrideAffiliate)

  val empty = Meta(None, band3OptionChosen = false, overrideAffiliate = false)
  def apply(m:M.Meta):Meta = Option(m).map(new Meta(_)).getOrElse(empty)
}

case class Meta(attachment:Option[File], band3OptionChosen:Boolean, overrideAffiliate:Boolean) {

  private def this(m:M.Meta) = this(Option(m.getAttachment).map(new File(_)),
    Option(m.isBand3OptionChosen).exists(_.booleanValue),
    Option(m.isOverrideAffiliate).exists(_.booleanValue))

  def mutable = {
    val m = Factory.createMeta
    m.setAttachment(attachment.map(_.getPath).orNull)
    m.setBand3OptionChosen(band3OptionChosen)
    m.setOverrideAffiliate(overrideAffiliate)
    m
  }
}

