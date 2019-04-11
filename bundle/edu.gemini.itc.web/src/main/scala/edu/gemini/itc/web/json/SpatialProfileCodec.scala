package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.spModel.core.{ SpatialProfile, PointSource, UniformSource, GaussianSource }

trait SpatialProfileCodec {
  import edu.gemini.json.coproduct._

  private val PointSourceCodec: CodecJson[PointSource.type] =
    constCodec(PointSource)

  private val UniformSourceCodec: CodecJson[UniformSource.type] =
    constCodec(UniformSource)

  private val GaussianSourceCodec: CodecJson[GaussianSource] =
    casecodec1(GaussianSource.apply, GaussianSource.unapply)(
      "fwhm"
    )

  implicit val SpatialProfileCodec: CodecJson[SpatialProfile] = {
    CoproductCodec[SpatialProfile]
      .withCase("PointSource",    PointSourceCodec)    { case p @ PointSource    => p }
      .withCase("UniformSource",  UniformSourceCodec)  { case p @ UniformSource  => p }
      .withCase("GaussianSource", GaussianSourceCodec) { case p : GaussianSource => p }
      .asCodecJson
  }

}

object spatialprofile extends SpatialProfileCodec