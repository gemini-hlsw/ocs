package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared.SourceDefinition
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
import edu.gemini.spModel.core._

trait SourceDefinitionCodec {
  import coproduct._
  import keyed._
  import spatialprofile._
  import spectraldistribution._

  private implicit val BrightnessUnitCodec: CodecJson[BrightnessUnit] = {
    val MagnitudeSystemCodec   = keyedCodec[MagnitudeSystem,   String](_.name, MagnitudeSystem.fromString)
    val SurfaceBrightnessCodec = keyedCodec[SurfaceBrightness, String](_.name, SurfaceBrightness.fromString)
    CoproductCodec[BrightnessUnit]
      .withCase("MagnitudeSystem",   MagnitudeSystemCodec)   { case m: MagnitudeSystem   => m }
      .withCase("SurfaceBrightness", SurfaceBrightnessCodec) { case b: SurfaceBrightness => b }
      .asCodecJson
  }

  private implicit val MagnitudeBandCodec: CodecJson[MagnitudeBand] =
    keyedCodec[MagnitudeBand, String](_.name, MagnitudeBand.fromString)

  private implicit val RedshiftCodec: CodecJson[Redshift] =
    casecodec1(Redshift.apply, Redshift.unapply)(
      "z"
    )

  implicit val SourceDefinitionCodec: CodecJson[SourceDefinition] =
    casecodec6(SourceDefinition.apply, SourceDefinition.unapply)(
      "profile",
      "distribution",
      "norm",
      "units",
      "normBand",
      "redshift"
    )

}

object sourcedefinition extends SourceDefinitionCodec