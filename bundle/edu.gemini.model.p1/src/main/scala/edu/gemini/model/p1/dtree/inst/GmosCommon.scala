package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._

object GmosCommon {
  sealed trait NodAndShuffle { def toBoolean: Boolean }
  case object NsYes extends NodAndShuffle { val toBoolean = true }
  case object NsNo extends NodAndShuffle { val toBoolean = false }

  sealed trait PreImaging { def toBoolean: Boolean }
  case object PreImagingYes extends PreImaging {
    val toBoolean = true
    override def toString = "Yes, requires pre-imaging."
  }
  case object PreImagingNo  extends PreImaging {
    val toBoolean = false
    override def toString = "No, does not require pre-imaging."
  }

  trait PreImagingNode { self: GenericNode =>
    val title       = "Pre-Imaging"
    val description = "Is pre-imaging required for this configuration?"
    def choices     = List(PreImagingYes, PreImagingNo)
  }

}