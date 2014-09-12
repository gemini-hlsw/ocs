package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._

trait Gmos {

  /** Enumerated type for pre-imaging. */
  object PreImaging extends Enumeration {
    val Yes, No = Value
    type PreImaging = Value
    implicit val toBool = (pi:PreImaging) => pi == Yes
  }

  trait PreImagingNode { self: GenericNode =>
    val title = "Pre-Imaging"
    val description = "Is pre-imaging required for this configuration?"
    def choices = PreImaging.values.toList
  }

}
