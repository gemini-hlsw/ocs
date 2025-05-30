package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ObservationDetailsCodec {
  import edu.gemini.json.coproduct._
  import edu.gemini.json.keyed._

  private val ImagingExpCountCodec: CodecJson[ImagingExposureCount] =
    casecodec5(ImagingExposureCount.apply, ImagingExposureCount.unapply)(
      "sigma",
      "exposureTime",
      "coadds",
      "sourceFraction",
      "offset"
    )

  private val ImagingIntCodec: CodecJson[ImagingIntegrationTime] =
    casecodec4(ImagingIntegrationTime.apply, ImagingIntegrationTime.unapply)(
      "sigma",
      "coadds",
      "sourceFraction",
      "offset"
    )

  private val SpectroscopyIntCodec: CodecJson[SpectroscopyIntegrationTime] =
    casecodec5(SpectroscopyIntegrationTime.apply, SpectroscopyIntegrationTime.unapply)(
      "sigma",
      "wavelengthAt",
      "coadds",
      "sourceFraction",
      "offset"
    )

  private val IntMethodCodec: CodecJson[IntegrationTimeMethod] =
    CoproductCodec[IntegrationTimeMethod]
      .withCase("SpectroscopyIntegrationTime", SpectroscopyIntCodec) { case a: SpectroscopyIntegrationTime => a }
      .withCase("ImagingIntegrationTime", ImagingIntCodec)           { case a: ImagingIntegrationTime => a }
      .asCodecJson

  private val ImagingS2NCodec: CodecJson[ImagingS2N] =
    casecodec5(ImagingS2N.apply, ImagingS2N.unapply)(
      "exposures",
      "coadds",
      "exposureTime",
      "sourceFraction",
      "offset"
    )

  private val SpectroscopyS2NCodec: CodecJson[SpectroscopyS2N] =
    casecodec6(SpectroscopyS2N.apply, SpectroscopyS2N.unapply)(
      "exposures",
      "coadds",
      "exposureTime",
      "sourceFraction",
      "offset",
      "wavelengthAt"
    )

  private val S2NMethodCodec: CodecJson[S2NMethod] =
    CoproductCodec[S2NMethod]
      .withCase("ImagingS2N",      ImagingS2NCodec)      { case a: ImagingS2N      => a }
      .withCase("SpectroscopyS2N", SpectroscopyS2NCodec) { case a: SpectroscopyS2N => a }
      .asCodecJson

  private implicit val CalculationMethodCodec: CodecJson[CalculationMethod] =
    CoproductCodec[CalculationMethod]
      .withCase("IntegrationTimeMethod", IntMethodCodec)     { case a: IntegrationTimeMethod => a }
      .withCase("ExposureCountMethod", ImagingExpCountCodec) { case a: ImagingExposureCount => a }
      .withCase("S2NMethod", S2NMethodCodec)                 { case a: S2NMethod => a }
      .asCodecJson

  private val AutoApertureCodec: CodecJson[AutoAperture] =
    casecodec1(AutoAperture.apply, AutoAperture.unapply)(
      "skyAperture"
    )

  private val UserApertureCodec: CodecJson[UserAperture] =
    casecodec2(UserAperture.apply, UserAperture.unapply)(
      "diameter",
      "skyAperture"
    )

  private val ApertureMethodCodec: CodecJson[ApertureMethod] =
    CoproductCodec[ApertureMethod]
      .withCase("AutoAperture", AutoApertureCodec) { case a: AutoAperture => a }
      .withCase("UserAperture", UserApertureCodec) { case a: UserAperture => a}
      .asCodecJson

  private val IfuSingleCodec: CodecJson[IfuSingle] =
    casecodec2(IfuSingle.apply, IfuSingle.unapply)(
      "skyFibres",
      "offset"
    )

  private val IfuRadialCodec: CodecJson[IfuRadial] =
    casecodec3(IfuRadial.apply, IfuRadial.unapply)(
      "skyFibres",
      "minOffset",
      "maxOffset"
    )

  private val IfuSummedCodec: CodecJson[IfuSummed] =
    casecodec5(IfuSummed.apply, IfuSummed.unapply)(
      "skyFibres",
      "numX",
      "numY",
      "centerX",
      "centerY"
    )

  private val IfuSumCodec: CodecJson[IfuSum] =
    casecodec3(IfuSum.apply, IfuSum.unapply)(
      "skyFibres",
      "num",
      "isIfu2"
    )

  private val IfuMethodCodec: CodecJson[IfuMethod] =
    CoproductCodec[IfuMethod]
      .withCase("IfuSingle", IfuSingleCodec) { case a: IfuSingle => a }
      .withCase("IfuRadial", IfuRadialCodec) { case a: IfuRadial => a }
      .withCase("IfuSummed", IfuSummedCodec) { case a: IfuSummed => a }
      .withCase("IfuSum",    IfuSumCodec)    { case a: IfuSum    => a }
      .asCodecJson


  private implicit val AnalysisMethodCodec: CodecJson[AnalysisMethod] =
    CoproductCodec[AnalysisMethod]
    .withCase("IfuMethod", IfuMethodCodec)           { case a: IfuMethod      => a }
    .withCase("ApertureMethod", ApertureMethodCodec) { case a: ApertureMethod => a }
    .asCodecJson

  implicit val ObservationDetailsCodec: CodecJson[ObservationDetails] =
    casecodec2(ObservationDetails.apply, ObservationDetails.unapply)(
      "calculationMethod",
      "analysisMethod"
    )

}

object observationdetails extends ObservationDetailsCodec
