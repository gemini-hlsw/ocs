package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import edu.gemini.spModel.core._
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosSouthType, GmosNorthType, GmosCommonType}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.trecs.TReCSParams

trait ArbInstrumentDetails {
  import core._

  val genAltairParameters: Gen[AltairParameters] =
    for {
      guideStarSeparation <- arbitrary[Double]
      guideStarMagnitude  <- arbitrary[Double]
      fieldLens           <- arbitrary[AltairParams.FieldLens]
      wfsMode             <- arbitrary[AltairParams.GuideStarType]
    } yield AltairParameters(guideStarSeparation, guideStarMagnitude, fieldLens, wfsMode)

  implicit val arbAltairParameters: Arbitrary[AltairParameters] =
    Arbitrary(genAltairParameters)

  val genGemsParameters: Gen[GemsParameters] =
    for {
      avgStrehl          <- arbitrary[Double]
      strehlBand         <- arbitrary[String]
    } yield GemsParameters(avgStrehl, strehlBand)

  val genAcquisitionCamParameters: Gen[AcquisitionCamParameters] =
    for {
      colorFilter        <- arbitrary[AcqCamParams.ColorFilter]
      ndFilter           <- arbitrary[AcqCamParams.NDFilter]
    } yield AcquisitionCamParameters(colorFilter, ndFilter)

  val genFlamingos2Parameters: Gen[Flamingos2Parameters] =
    for {
      filter             <- arbitrary[Flamingos2.Filter]
      grism              <- arbitrary[Flamingos2.Disperser]
      mask               <- arbitrary[Flamingos2.FPUnit]
      customSlitWidth    <- arbitrary[Option[Flamingos2.CustomSlitWidth]]
      readMode           <- arbitrary[Flamingos2.ReadMode]
    } yield Flamingos2Parameters(filter, grism, mask, customSlitWidth, readMode)

  val genGmosCommonTypeFilter: Gen[GmosCommonType.Filter] =
    Gen.oneOf(
      arbitrary[GmosNorthType.FilterNorth],
      arbitrary[GmosSouthType.FilterSouth]
    )

  val genGmosCommonTypeDisperser: Gen[GmosCommonType.Disperser] =
    Gen.oneOf(
      arbitrary[GmosNorthType.DisperserNorth],
      arbitrary[GmosSouthType.DisperserSouth]
    )

  val genGmosCommonTypeFPUnit: Gen[GmosCommonType.FPUnit] =
    Gen.oneOf(
      arbitrary[GmosNorthType.FPUnitNorth],
      arbitrary[GmosSouthType.FPUnitSouth]
    )

  val genGmosParameters: Gen[GmosParameters] =
    for {
      filter             <- genGmosCommonTypeFilter
      grating            <- genGmosCommonTypeDisperser
      centralWavelength  <- arbitrary[Wavelength]
      fpMask             <- genGmosCommonTypeFPUnit
      ampGain            <- arbitrary[GmosCommonType.AmpGain]
      ampReadMode        <- arbitrary[GmosCommonType.AmpReadMode]
      customSlitWidth    <- arbitrary[Option[GmosCommonType.CustomSlitWidth]]
      spatialBinning     <- arbitrary[Int]
      spectralBinning    <- arbitrary[Int]
      ccdType            <- arbitrary[GmosCommonType.DetectorManufacturer]
      builtinROI         <- arbitrary[GmosCommonType.BuiltinROI]
      site               <- arbitrary[Site]
    } yield GmosParameters(filter, grating, centralWavelength, fpMask, ampGain, ampReadMode,
        customSlitWidth, spatialBinning, spectralBinning, ccdType, builtinROI, site)

  val genGnirsParameters: Gen[GnirsParameters] =
    for {
      pixelScale         <- arbitrary[GNIRSParams.PixelScale]
      filter             <- arbitrary[Option[GNIRSParams.Filter]]
      grating            <- arbitrary[Option[GNIRSParams.Disperser]]
      readMode           <- arbitrary[GNIRSParams.ReadMode]
      crossDispersed     <- arbitrary[GNIRSParams.CrossDispersed]
      centralWavelength  <- arbitrary[Wavelength]
      slitWidth          <- arbitrary[GNIRSParams.SlitWidth]
      camera             <- arbitrary[Option[GNIRSParams.Camera]]
      wellDepth          <- arbitrary[GNIRSParams.WellDepth]
      altair             <- arbitrary[Option[AltairParameters]]
    } yield GnirsParameters(pixelScale, filter, grating, readMode, crossDispersed,
        centralWavelength, slitWidth, camera, wellDepth, altair)

  val genGsaoiParameters: Gen[GsaoiParameters] =
    for {
      filter             <- arbitrary[Gsaoi.Filter]
      readMode           <- arbitrary[Gsaoi.ReadMode]
      largeSkyOffset     <- arbitrary[Int]
      gems               <- genGemsParameters
    } yield GsaoiParameters(filter, readMode, largeSkyOffset, gems)

  val genMichelleParameters: Gen[MichelleParameters] =
    for {
      filter             <- arbitrary[MichelleParams.Filter]
      grating            <- arbitrary[MichelleParams.Disperser]
      centralWavelength  <- arbitrary[Wavelength]
      mask               <- arbitrary[MichelleParams.Mask]
      polarimetry        <- arbitrary[YesNoType]
    } yield MichelleParameters(filter, grating, centralWavelength, mask, polarimetry)

  val genNifsParameters: Gen[NifsParameters] =
    for {
      filter             <- arbitrary[NIFSParams.Filter]
      grating            <- arbitrary[NIFSParams.Disperser]
      readMode           <- arbitrary[NIFSParams.ReadMode]
      centralWavelength  <- arbitrary[Wavelength]
      altair             <- arbitrary[Option[AltairParameters]]
    } yield NifsParameters(filter, grating, readMode, centralWavelength, altair)

  val genNiriParameters: Gen[NiriParameters] =
    for {
      filter             <- arbitrary[Niri.Filter]
      grism              <- arbitrary[Niri.Disperser]
      camera             <- arbitrary[Niri.Camera]
      readMode           <- arbitrary[Niri.ReadMode]
      wellDepth          <- arbitrary[Niri.WellDepth]
      mask               <- arbitrary[Niri.Mask]
      builtinROI         <- arbitrary[Niri.BuiltinROI]
      altair             <- arbitrary[Option[AltairParameters]]
    } yield NiriParameters(filter, grism, camera, readMode, wellDepth, mask, builtinROI, altair)

  val genTRecsParameters: Gen[TRecsParameters] =
    for {
      filter             <- arbitrary[TReCSParams.Filter]
      instrumentWindow   <- arbitrary[TReCSParams.WindowWheel]
      grating            <- arbitrary[TReCSParams.Disperser]
      centralWavelength  <- arbitrary[Wavelength]
      mask               <- arbitrary[TReCSParams.Mask]
    } yield TRecsParameters(filter, instrumentWindow, grating, centralWavelength, mask)

  val genInstrumentDetails: Gen[InstrumentDetails] =
    Gen.oneOf(
      genAcquisitionCamParameters,
      genFlamingos2Parameters,
      genGmosParameters,
      genGnirsParameters,
      genGsaoiParameters,
      genMichelleParameters,
      genNifsParameters,
      genNiriParameters,
      genTRecsParameters
    )

  implicit val arbInstrumentDetails: Arbitrary[InstrumentDetails] =
    Arbitrary(genInstrumentDetails)

}

object instrumentdetails extends ArbInstrumentDetails