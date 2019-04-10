package edu.gemini.ags.servlet

import edu.gemini.spModel.data.AbstractDataObject
import edu.gemini.spModel.gemini.altair.AltairParams.Mode

sealed trait AgsAo extends Product with Serializable {

  import AgsAo._

  def aoObsComp: AbstractDataObject =
    this match {
      case Altair(m) =>
        val alt = new edu.gemini.spModel.gemini.altair.InstAltair
        alt.setMode(m)
        alt

      case Gems      =>
        new edu.gemini.spModel.gemini.gems.Gems
    }

}



object AgsAo {

  final case class Altair(mode: Mode) extends AgsAo

  case object Gems extends AgsAo

}
