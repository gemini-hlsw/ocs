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
  import edu.gemini.json.coproduct._
  import edu.gemini.json.keyed._
  import wavelength._

  // All our enum types must be declared explicitly
  // TODO-GHOSTITC: Add GHOST params here
  private implicit val AcqCamParamsColorFilterCodec = enumCodec[AcqCamParams.ColorFilter]
  private implicit val AcqCamParamsNDFilterCodec = enumCodec[AcqCamParams.NDFilter]
  private implicit val Flamingos2CustomSlitWidthCodec = enumCodec[Flamingos2.CustomSlitWidth]
  private implicit val Flamingos2DisperserCodec = enumCodec[Flamingos2.Disperser]
  private implicit val Flamingos2FilterCodec = enumCodec[Flamingos2.Filter]
  private implicit val Flamingos2FPUnitCodec = enumCodec[Flamingos2.FPUnit]
  private implicit val Flamingos2ReadModeCodec = enumCodec[Flamingos2.ReadMode]
  private implicit val GmosCommonTypeAmpGainCodec = enumCodec[GmosCommonType.AmpGain]
  private implicit val GmosCommonTypeAmpReadModeCodec = enumCodec[GmosCommonType.AmpReadMode]
  private implicit val GmosCommonTypeBuiltinROICodec = enumCodec[GmosCommonType.BuiltinROI]
  private implicit val GmosCommonTypeCustomSlitWidthCodec = enumCodec[GmosCommonType.CustomSlitWidth]
  private implicit val GmosCommonTypeDetectorManufacturerCodec = enumCodec[GmosCommonType.DetectorManufacturer]
  private implicit val GNIRSParamsCameraCodec = enumCodec[GNIRSParams.Camera]
  private implicit val GNIRSParamsCrossDispersedCodec = enumCodec[GNIRSParams.CrossDispersed]
  private implicit val GNIRSParamsDisperserCodec = enumCodec[GNIRSParams.Disperser]
  private implicit val GNIRSParamsFilterCodec = enumCodec[GNIRSParams.Filter]
  private implicit val GNIRSParamsPixelScaleCodec = enumCodec[GNIRSParams.PixelScale]
  private implicit val GNIRSParamsReadModeCodec = enumCodec[GNIRSParams.ReadMode]
  private implicit val GNIRSParamsSlitWidthCodec = enumCodec[GNIRSParams.SlitWidth]
  private implicit val GNIRSParamsWellDepthCodec = enumCodec[GNIRSParams.WellDepth]
  private implicit val GsaoiFilterCodec = enumCodec[Gsaoi.Filter]
  private implicit val GsaoiReadModeCodec = enumCodec[Gsaoi.ReadMode]
  private implicit val MichelleParamsDisperserCodec = enumCodec[MichelleParams.Disperser]
  private implicit val MichelleParamsFilterCodec = enumCodec[MichelleParams.Filter]
  private implicit val MichelleParamsMaskCodec = enumCodec[MichelleParams.Mask]
  private implicit val NIFSParamsDisperserCodec = enumCodec[NIFSParams.Disperser]
  private implicit val NIFSParamsFilterCodec = enumCodec[NIFSParams.Filter]
  private implicit val NIFSParamsReadModeCodec = enumCodec[NIFSParams.ReadMode]
  private implicit val NiriBuiltinROICodec = enumCodec[Niri.BuiltinROI]
  private implicit val NiriCameraCodec = enumCodec[Niri.Camera]
  private implicit val NiriDisperserCodec = enumCodec[Niri.Disperser]
  private implicit val NiriFilterCodec = enumCodec[Niri.Filter]
  private implicit val NiriMaskCodec = enumCodec[Niri.Mask]
  private implicit val NiriReadModeCodec = enumCodec[Niri.ReadMode]
  private implicit val NiriWellDepthCodec = enumCodec[Niri.WellDepth]
  private implicit val SiteCodec = enumCodec[Site]
  private implicit val TReCSParamsDisperserCodec = enumCodec[TReCSParams.Disperser]
  private implicit val TReCSParamsFilterCodec = enumCodec[TReCSParams.Filter]
  private implicit val TReCSParamsMaskCodec = enumCodec[TReCSParams.Mask]
  private implicit val TReCSParamsWindowWheelCodec = enumCodec[TReCSParams.WindowWheel]
  private implicit val YesNoTypeCodec = enumCodec[YesNoType]
  private implicit val AltairParamsFieldLensCodec = enumCodec[AltairParams.FieldLens]
  private implicit val AltairParamsGuideStarTypeCodec = enumCodec[AltairParams.GuideStarType]

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