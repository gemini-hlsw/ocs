package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._
import edu.gemini.ags.servlet.AgsAo.Altair

import edu.gemini.ags.servlet.AgsInstrument
import edu.gemini.ags.servlet.AgsInstrument._
import edu.gemini.json.keyed._
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.telescope.PosAngleConstraint


trait AgsInstrumentCodec {

  import edu.gemini.json.all._

  private implicit val LyotWheelCodec: CodecJson[LyotWheel]     = enumCodec[LyotWheel]
  private implicit val FPUnitNorthCodec: CodecJson[FPUnitNorth] = enumCodec[FPUnitNorth]
  private implicit val FPUnitSouthCodec: CodecJson[FPUnitSouth] = enumCodec[FPUnitSouth]
  private implicit val InstrumentCodec: CodecJson[Instrument]   = enumCodec[Instrument]
  private implicit val AltairModeCodec: CodecJson[Mode]         = enumCodec[Mode]

  private implicit val AltairCodec: CodecJson[Altair] =
    casecodec1(Altair.apply, Altair.unapply)("mode")

  private implicit val PosAngleConstraintCodec: CodecJson[PosAngleConstraint] =
    enumCodec[PosAngleConstraint]

  private val Flamingos2Codec: CodecJson[Flamingos2] =
    casecodec2(Flamingos2.apply, Flamingos2.unapply)("lyout", "constraint")

  private val GmosNorthCodec: CodecJson[GmosNorth] =
    casecodec3(GmosNorth.apply, GmosNorth.unapply)("fpu", "constraint", "altair")

  private val GmosSouthCodec: CodecJson[GmosSouth] =
    casecodec2(GmosSouth.apply, GmosSouth.unapply)("fpu", "constraint")

  private val GnirsCodec: CodecJson[Gnirs] =
    casecodec2(Gnirs.apply, Gnirs.unapply)("constraint", "altair")

  private val GsaoiCodec: CodecJson[Gsaoi] =
    casecodec1(Gsaoi.apply, Gsaoi.unapply)("constraint")

  private val NifsCodec: CodecJson[Nifs] =
    casecodec1(Nifs.apply, Nifs.unapply)("altair")

  private val NiriCodec: CodecJson[Niri] =
    casecodec1(Niri.apply, Niri.unapply)("altair")

  private val OtherCodec: CodecJson[Other] =
    casecodec1(Other.apply, Other.unapply)("id")

  implicit val AgsInstrumentCodec: CodecJson[AgsInstrument] =
    CoproductCodec[AgsInstrument]
      .withCase("flamingos2", Flamingos2Codec) { case i: Flamingos2 => i }
      .withCase("gmosNorth",  GmosNorthCodec)  { case i: GmosNorth  => i }
      .withCase("gmosSouth",  GmosSouthCodec)  { case i: GmosSouth  => i }
      .withCase("gnirs",      GnirsCodec)      { case i: Gnirs      => i }
      .withCase("nifs",       NifsCodec)       { case i: Nifs       => i }
      .withCase("niri",       NiriCodec)       { case i: Niri       => i }
      .withCase("gsaoi",      GsaoiCodec)      { case i: Gsaoi      => i }
      .withCase("other",      OtherCodec)      { case i: Other      => i }
      .asCodecJson

}

object agsinstrument extends AgsInstrumentCodec
