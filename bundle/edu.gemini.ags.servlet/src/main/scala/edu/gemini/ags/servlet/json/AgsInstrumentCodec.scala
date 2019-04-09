package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.ags.servlet.AgsInstrument
import edu.gemini.ags.servlet.AgsInstrument._
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth

trait AgsInstrumentCodec {

  import edu.gemini.json.all._

  private implicit val LyotWheelCodec: CodecJson[LyotWheel]     = enumCodec[LyotWheel]
  private implicit val FPUnitNorthCodec: CodecJson[FPUnitNorth] = enumCodec[FPUnitNorth]
  private implicit val FPUnitSouthCodec: CodecJson[FPUnitSouth] = enumCodec[FPUnitSouth]
  private implicit val InstrumentCodec: CodecJson[Instrument]   = enumCodec[Instrument]

  private val Flamingos2Codec: CodecJson[Flamingos2] =
    casecodec1(Flamingos2.apply, Flamingos2.unapply)("lyout")

  private val GmosNorthCodec: CodecJson[GmosNorth] =
    casecodec1(GmosNorth.apply, GmosNorth.unapply)("fpu")

  private val GmosSouthCodec: CodecJson[GmosSouth] =
    casecodec1(GmosSouth.apply, GmosSouth.unapply)("fpu")

  private val OtherCodec: CodecJson[Other] =
    casecodec1(Other.apply, Other.unapply)("id")

  implicit val InstrumentRequestCodec: CodecJson[AgsInstrument] =
    CoproductCodec[AgsInstrument]
      .withCase("flamingos2", Flamingos2Codec) { case f2: Flamingos2 => f2 }
      .withCase("gmosNorth",  GmosNorthCodec)  { case gn: GmosNorth  => gn }
      .withCase("gmosSouth",  GmosSouthCodec)  { case gs: GmosSouth  => gs }
      .withCase("other",      OtherCodec)      { case o:  Other      => o  }
      .asCodecJson

}

object agsinstrument extends AgsInstrumentCodec
