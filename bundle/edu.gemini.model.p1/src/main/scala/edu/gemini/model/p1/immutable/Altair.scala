package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object Altair {
  def apply(ac:M.AltairChoice): Altair = {
    // This is awkward but if o is null and we try to unbox it implicitly
    // it will throw a NPE.
    def bool(o: java.lang.Boolean): Boolean = Option(o).map(Boolean.unbox).getOrElse(false)

    def lgs: Option[AltairLGS] =
      Option(ac.getLgs).map { lgs =>
        AltairLGS(pwfs1 = bool(lgs.isPwfs1),
                  aowfs = bool(lgs.isAowfs),
                  oiwfs = bool(lgs.isOiwfs))
      }

    def ngs: Option[AltairNGS] =
      Option(ac.getNgs).map { ngs => AltairNGS(bool(ngs.isFieldLens)) }

    (lgs orElse ngs).getOrElse(AltairNone)
  }
}

sealed trait Altair {
  def shortName:String
  def mutable:M.AltairChoice
  def ao: AoPerspective
}

sealed trait AltairAO extends Altair {
  def fieldLens:Boolean
  def pwfs1:Boolean
}

case object AltairNone extends Altair {
  def mutable = {
    val m = Factory.createAltairChoice()
    m.setNone(Factory.createAltairNone)
    m
  }
  def ao: AoPerspective = AoNone
  override val toString = "None"
  val shortName = ""
}

case class AltairLGS(pwfs1: Boolean, aowfs: Boolean = false, oiwfs: Boolean = false) extends AltairAO {
  require((!pwfs1 && !aowfs && !oiwfs) || (pwfs1 ^ aowfs ^ oiwfs))
  require(!(pwfs1 && aowfs && oiwfs))
  val fieldLens = true

  def mutable = {
    val m = Factory.createAltairChoice()
    val n = Factory.createAltairLGS
    n.setPwfs1(pwfs1)
    n.setOiwfs(oiwfs)
    n.setAowfs(aowfs)
    m.setLgs(n)
    m
  }
  def ao: AoPerspective = AoLgs
  override val toString = {
    val suffix = if (pwfs1) {
        " w/ PWFS1"
      } else if (oiwfs) {
        " w/ OIWFS"
      } else {
        ""
      }
    s"Altair Laser Guidestar$suffix"
  }
  val shortName = if (pwfs1) {
      "LGS/PWFS1"
    } else if (aowfs) {
      "LGS/AOWFS"
    } else if (oiwfs) {
      "LGS/OIWFS"
    } else {
      "LGS"
    }
}

case class AltairNGS(fieldLens:Boolean) extends AltairAO {
  val pwfs1 = false

  def mutable = {
    val m = Factory.createAltairChoice()
    val n = Factory.createAltairNGS()
    n.setFieldLens(fieldLens)
    m.setNgs(n)
    m
  }
  def ao: AoPerspective = AoNgs
  override val toString = "Altair Natural Guidestar" + (if (fieldLens) " w/ Field Lens" else "")
  val shortName = if (fieldLens) "NGS/FL" else "NGS"
}