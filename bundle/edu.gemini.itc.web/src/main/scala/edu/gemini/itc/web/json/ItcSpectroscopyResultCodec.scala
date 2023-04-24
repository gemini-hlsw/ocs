package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcSpectroscopyResultCodec {
  import edu.gemini.json.array._
  import color._
  import edu.gemini.json.coproduct._
  import itcccd._

  private implicit val SpcChartTypeCodec: CodecJson[SpcChartType] =
    CoproductCodec[SpcChartType]
      .withCase("SignalChart",      constCodec(SignalChart))      { case SignalChart      => SignalChart }
      .withCase("S2NChart",         constCodec(S2NChart))         { case S2NChart         => S2NChart }
      .withCase("SignalPixelChart", constCodec(SignalPixelChart)) { case SignalPixelChart => SignalPixelChart }
      .asCodecJson

  private implicit val ChartAxisRangeCodec: CodecJson[ChartAxisRange] =
    casecodec2(ChartAxisRange.apply, ChartAxisRange.unapply)(
      "start",
      "end"
    )

  private implicit val ChartAxisCodec: CodecJson[ChartAxis] =
    casecodec3(ChartAxis.apply, ChartAxis.unapply)(
      "label",
      "inverted",
      "range"
    )

  private implicit val SpcDataTypeCodec: CodecJson[SpcDataType] =
    CoproductCodec[SpcDataType]
      .withCase("SignalData", constCodec(SignalData)) { case SignalData => SignalData }
      .withCase("BackgroundData", constCodec(BackgroundData)) { case BackgroundData => BackgroundData }
      .withCase("SingleS2NData", constCodec(SingleS2NData)) { case SingleS2NData => SingleS2NData }
      .withCase("FinalS2NData", constCodec(FinalS2NData)) { case FinalS2NData => FinalS2NData }
      .withCase("PixSigData", constCodec(PixSigData)) { case PixSigData => PixSigData }
      .withCase("PixBackData", constCodec(PixBackData)) { case PixBackData => PixBackData }
      .asCodecJson

  private implicit val SpcSeriesDataCodec: CodecJson[SpcSeriesData] =
    casecodec4(SpcSeriesData.apply, SpcSeriesData.unapply)(
      "dataType",
      "title",
      "data",
      "color"
    )

  private implicit val SpcChartDataCodec: CodecJson[SpcChartData] =
    casecodec6(SpcChartData.apply, SpcChartData.unapply)(
      "chartType",
      "title",
      "xAxis",
      "yAxis",
      "series",
      "axes"
    )

  private implicit val SpcChartGroupCodec: CodecJson[SpcChartGroup] =
    casecodec1(SpcChartGroup.apply, SpcChartGroup.unapply)(
      "charts"
    )

  private implicit val ExposureCalculationCodec: CodecJson[ExposureCalculation] =
    casecodec3(ExposureCalculation.apply, ExposureCalculation.unapply)(
      "exposureTime",
      "exposures",
      "signalToNoise"
    )

  val ItcSpectroscopyResultCodec: CodecJson[ItcSpectroscopyResult] =
    casecodec3(ItcSpectroscopyResult.apply, ItcSpectroscopyResult.unapply)(
      "ccds",
      "chartGroups",
      "exposureCalculation"
  )

}

object itcspectroscopyresult extends ItcSpectroscopyResultCodec