package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.spModel.core._
import squants.motion.Velocity
import squants.radio.{ Irradiance, SpectralIrradiance }

trait SpectralDistributionCodec {
  import coproduct._
  import keyed._
  import wavelength._

  private implicit val BlackBodyCodec: CodecJson[BlackBody] =
    casecodec1(BlackBody.apply, BlackBody.unapply)(
      "temperature"
    )

  private implicit val PowerLawCodec: CodecJson[PowerLaw] =
    casecodec1(PowerLaw.apply, PowerLaw.unapply)(
      "index"
    )

  private implicit val EmissionLineCodec: CodecJson[EmissionLine] = {
    implicit val VelocityCodec:           CodecJson[Velocity]           = ???
    implicit val IrradianceCodec:         CodecJson[Irradiance]         = ???
    implicit val SpectralIrradianceCodec: CodecJson[SpectralIrradiance] = ???
    casecodec4(EmissionLine.apply, EmissionLine.unapply)(
      "wavelength",
      "width",
      "flux",
      "continuum"
    )
  }

  private implicit val UserDefinedSpectrumCodec: CodecJson[UserDefinedSpectrum] =
    casecodec2(UserDefinedSpectrum.apply, UserDefinedSpectrum.unapply)(
      "name",
      "spectrum"
    )

  private implicit val AuxFileSpectrumCodec: CodecJson[AuxFileSpectrum] =
    casecodec2(AuxFileSpectrum.apply, AuxFileSpectrum.unapply)(
      "programId",
      "name"
    )

  private implicit val UserDefinedCodec: CodecJson[UserDefined] =
    CoproductCodec[UserDefined]
      .withCase("UserDefinedSpectrum", UserDefinedSpectrumCodec) { case a: UserDefinedSpectrum => a }
      .withCase("AuxFileSpectrum",     AuxFileSpectrumCodec)     { case a: AuxFileSpectrum     => a }
      .asCodecJson

  private implicit val LibraryNonStarCodec: CodecJson[LibraryNonStar] =
    keyedCodec(_.sedSpectrum, LibraryNonStar.findByName)

  private implicit val LibraryStarCodec: CodecJson[LibraryStar] =
    keyedCodec(_.sedSpectrum, LibraryStar.findByName)

  private implicit val LibraryCodec: CodecJson[Library] =
    CoproductCodec[Library]
      .withCase("LibraryStar",    LibraryStarCodec)    { case a: LibraryStar    => a }
      .withCase("LibraryNonStar", LibraryNonStarCodec) { case a: LibraryNonStar => a }
      .asCodecJson

  implicit val SpectralDistributionCodec: CodecJson[SpectralDistribution] =
    CoproductCodec[SpectralDistribution]
      .withCase("BlackBody",    BlackBodyCodec)    { case b: BlackBody    => b }
      .withCase("PowerLaw",     PowerLawCodec)     { case p: PowerLaw     => p}
      .withCase("EmissionLine", EmissionLineCodec) { case e: EmissionLine => e }
      .withCase("UserDefined",  UserDefinedCodec)  { case u: UserDefined  => u}
      .withCase("Library",      LibraryCodec)      { case l: Library      => l }
      .asCodecJson

}

object spectraldistribution extends SpectralDistributionCodec