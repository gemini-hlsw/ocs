package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.spModel.core.{ Wavelength, Site }
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{ GmosSouthType, GmosNorthType, GmosCommonType }
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.trecs.TReCSParams

trait InstrumentDetailsCodec {
  import coproduct._
  import keyed._
  import wavelength._

  private implicit val AltairParametersCodec: CodecJson[AltairParameters] =
    casecodec4(AltairParameters.apply, AltairParameters.unapply)(
      "guideStarSeparation",
      "guideStarMagnitude",
      "fieldLens",
      "wfsMode"
    )

  private implicit val GemsParametersCodec: CodecJson[GemsParameters] =
    casecodec2(GemsParameters.apply, GemsParameters.unapply)(
      "avgStrehl",
      "strehlBand"
    )

  private implicit val AcquisitionCamParametersCodec: CodecJson[AcquisitionCamParameters] =
    casecodec2(AcquisitionCamParameters.apply, AcquisitionCamParameters.unapply)(
      "colorFilter",
      "ndFilter"
    )

  private implicit val Flamingos2ParametersCodec: CodecJson[Flamingos2Parameters] =
    casecodec5(Flamingos2Parameters.apply, Flamingos2Parameters.unapply)(
      "filter",
      "grism",
      "mask",
      "customSlitWidth",
      "readMode"
    )

  implicit val CommonFilterCodec: CodecJson[GmosCommonType.Filter]  =
    CoproductCodec[GmosCommonType.Filter]
      .withCase("FilterNorth", enumCodec[GmosNorthType.FilterNorth]) { case f: GmosNorthType.FilterNorth => f }
      .withCase("FilterSouth", enumCodec[GmosSouthType.FilterSouth]) { case f: GmosSouthType.FilterSouth => f }
      .asCodecJson

  implicit val CommonDisperserCodec: CodecJson[GmosCommonType.Disperser] =
    CoproductCodec[GmosCommonType.Disperser]
      .withCase("DisperserNorth", enumCodec[GmosNorthType.DisperserNorth]) { case f: GmosNorthType.DisperserNorth => f }
      .withCase("DisperserSouth", enumCodec[GmosSouthType.DisperserSouth]) { case f: GmosSouthType.DisperserSouth => f }
      .asCodecJson

  implicit val CommonFPUnitCodec: CodecJson[GmosCommonType.FPUnit] =
    CoproductCodec[GmosCommonType.FPUnit]
      .withCase("FPUnitNorth", enumCodec[GmosNorthType.FPUnitNorth]) { case f: GmosNorthType.FPUnitNorth => f }
      .withCase("FPUnitSouth", enumCodec[GmosSouthType.FPUnitSouth]) { case f: GmosSouthType.FPUnitSouth => f }
      .asCodecJson

  private implicit val GmosParametersCodec: CodecJson[GmosParameters] =
    casecodec12(GmosParameters.apply, GmosParameters.unapply)(
      "filter",
      "grating",
      "centralWavelength",
      "fpMask",
      "ampGain",
      "ampReadMode",
      "customSlitWidth",
      "spatialBinning",
      "spectralBinning",
      "ccdType",
      "builtinROI",
      "site"
    )

  private implicit val GnirsParametersCodec: CodecJson[GnirsParameters] =
    casecodec10(GnirsParameters.apply, GnirsParameters.unapply)(
      "pixelScale",
      "filter",
      "grating",
      "readMode",
      "crossDispersed",
      "centralWavelength",
      "slitWidth",
      "camera",
      "wellDepth",
      "altair"
    )


  private implicit val GsaoiParametersCodec: CodecJson[GsaoiParameters] =
    casecodec4(GsaoiParameters.apply, GsaoiParameters.unapply)(
      "filter",
      "readMode",
      "largeSkyOffset",
      "gems"
    )

  private implicit val MichelleParametersCodec: CodecJson[MichelleParameters] =
    casecodec5(MichelleParameters.apply, MichelleParameters.unapply)(
      "filter",
      "grating",
      "centralWavelength",
      "mask",
      "polarimetry"
    )


  private implicit val NifsParametersCodec: CodecJson[NifsParameters] =
    casecodec5(NifsParameters.apply, NifsParameters.unapply)(
      "filter",
      "grating",
      "readMode",
      "centralWavelength",
      "altair"
    )

  private implicit val NiriParametersCodec: CodecJson[NiriParameters] =
    casecodec8(NiriParameters.apply, NiriParameters.unapply)(
      "filter",
      "grism",
      "camera",
      "readMode",
      "wellDepth",
      "mask",
      "builtinROI",
      "altair"
    )

  private implicit val TRecsParametersCodec: CodecJson[TRecsParameters] =
    casecodec5(TRecsParameters.apply, TRecsParameters.unapply)(
      "filter",
      "instrumentWindow",
      "grating",
      "centralWavelength",
      "mask"
    )

  implicit val InstrumentDetailsDecodeJson: CodecJson[InstrumentDetails] =
    CoproductCodec[InstrumentDetails]
      .withCase("AcquisitionCamParameters", AcquisitionCamParametersCodec) { case a: AcquisitionCamParameters => a }
      .withCase("Flamingos2Parameters",     Flamingos2ParametersCodec)     { case a: Flamingos2Parameters     => a }
      .withCase("GmosParameters",           GmosParametersCodec)           { case a: GmosParameters           => a }
      .withCase("GnirsParameters",          GnirsParametersCodec)          { case a: GnirsParameters          => a }
      .withCase("GsaoiParameters",          GsaoiParametersCodec)          { case a: GsaoiParameters          => a }
      .withCase("MichelleParameters",       MichelleParametersCodec)       { case a: MichelleParameters       => a }
      .withCase("NifsParameters",           NifsParametersCodec)           { case a: NifsParameters           => a }
      .withCase("NiriParameters",           NiriParametersCodec)           { case a: NiriParameters           => a }
      .withCase("TRecsParameters",          TRecsParametersCodec)          { case a: TRecsParameters          => a }
      .asCodecJson

}

object instrumentdetails extends InstrumentDetailsCodec