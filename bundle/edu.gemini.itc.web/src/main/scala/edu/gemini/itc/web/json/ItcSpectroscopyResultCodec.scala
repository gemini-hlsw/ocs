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
      .withCase("S2NChartPerRes",   constCodec(S2NChartPerRes))   { case S2NChartPerRes   => S2NChartPerRes }
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

  implicit val SpcDataTypeCodec: CodecJson[SpcDataType] =
    CoproductCodec[SpcDataType]
      .withCase("SignalData",         constCodec(SignalData))         { case SignalData         => SignalData }
      .withCase("BackgroundData",     constCodec(BackgroundData))     { case BackgroundData     => BackgroundData }
      .withCase("SingleS2NData",      constCodec(SingleS2NData))      { case SingleS2NData      => SingleS2NData }
      .withCase("FinalS2NData",       constCodec(FinalS2NData))       { case FinalS2NData       => FinalS2NData }
      .withCase("PixSigData",         constCodec(PixSigData))         { case PixSigData         => PixSigData }
      .withCase("PixBackData",        constCodec(PixBackData))        { case PixBackData        => PixBackData }
      .withCase("FinalS2NPerResEle",  constCodec(FinalS2NPerResEle))  { case FinalS2NPerResEle  => FinalS2NPerResEle }
      .withCase("SingleS2NPerResEle", constCodec(SingleS2NPerResEle)) { case SingleS2NPerResEle => SingleS2NPerResEle }
      .asCodecJson

  // Default used by the OT. Callers can shadow this with a local implicit
  // to customise encoding (e.g. for GPP).
  implicit val SpcSeriesDataCodec: CodecJson[SpcSeriesData] =
    casecodec4(SpcSeriesData.apply, SpcSeriesData.unapply)(
      "dataType",
      "title",
      "data",
      "color"
    )

  // implicit def so that the SpcSeriesData codec — and the entire chart-data chain —
  // is resolved at the outermost call site rather than at object-init time.
  //
  // The parameter is deliberately named `SpcSeriesDataCodec` so it shadows the
  // inherited val of the same name inside this method body. That way there is
  // exactly one candidate for CodecJson[SpcSeriesData] in scope here (the param),
  // and Argonaut's list-codec derivation has no ambiguity to resolve.
  implicit def ItcSpectroscopyResultCodec(implicit SpcSeriesDataCodec: CodecJson[SpcSeriesData]): CodecJson[ItcSpectroscopyResult] = {
    implicit val SpcChartDataCodec: CodecJson[SpcChartData] =
      casecodec6(SpcChartData.apply, SpcChartData.unapply)(
        "chartType",
        "title",
        "xAxis",
        "yAxis",
        "series",
        "axes"
      )
    implicit val SpcChartGroupCodec: CodecJson[SpcChartGroup] =
      casecodec1(SpcChartGroup.apply, SpcChartGroup.unapply)(
        "charts"
      )
    casecodec4(ItcSpectroscopyResult.apply, ItcSpectroscopyResult.unapply)(
      "ccds",
      "chartGroups",
      "times",
      "signalToNoiseAt"
    )
  }

}

object itcspectroscopyresult extends ItcSpectroscopyResultCodec
