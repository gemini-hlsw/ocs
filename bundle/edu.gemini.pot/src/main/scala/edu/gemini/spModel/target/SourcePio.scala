package edu.gemini.spModel.target

import edu.gemini.spModel.core.{LibraryNonStar, LibraryStar, AuxFileSpectrum, EmissionLine, PowerLaw, BlackBody, SpectralDistribution, GaussianSource, UniformSource, PointSource, SpatialProfile, SPProgramID}
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import squants.motion.Velocity
import squants.radio.{Irradiance, SpectralIrradiance}

import scalaz.Scalaz._

object SourcePio {

  // Spatial profile
  private val PointSourceName     = "PointSource"

  private val GaussianSourceName  = "GaussianSource"
  private val GaussianSourceFwhm  = "fwhm"

  private val UniformSourceName   = "UniformSource"


  // Spectral distribution
  private val BlackBodyName       = "BlackBody"
  private val BlackBodyTemp       = "temperature"

  private val PowerLawName        = "PowerLaw"
  private val PowerLawIndex       = "index"

  private val EmissionLineName    = "EmissionLine"
  private val ElineWavelength     = "wavelength"
  private val ElineWidth          = "width"
  private val ElineFlux           = "flux"
  private val ElineContinuum      = "continuum"

  private val LibraryStarName     = "LibraryStar"
  private val LibraryNonStarName  = "LibraryNonStar"
  private val LibrarySpectrum     = "spectrum"

  private val UserDefinedName     = "UserDefined"
  private val UserDefinedAuxFile  = "auxFile"
  private val UserDefinedProgramId= "programId"


  /** Turns a spectral distribution into a param set. */
  def toParamSet(spectralDistribution: SpectralDistribution, factory: PioFactory): ParamSet =
    spectralDistribution match {

      case BlackBody(temperature) =>
        factory.createParamSet(BlackBodyName)     <|
          (Pio.addDoubleParam(factory, _, BlackBodyTemp, temperature))

      case PowerLaw(index)        =>
        factory.createParamSet(PowerLawName)      <|
          (Pio.addDoubleParam(factory, _, PowerLawIndex, index))

      case EmissionLine(wavelength, width, flux, continuum)     =>
        factory.createParamSet(EmissionLineName)  <|
          (Pio.addDoubleParam(factory, _, ElineWavelength, wavelength.toNanometers)) <|
          (Pio.addQuantity(factory,    _, ElineWidth,      width))                   <|
          (Pio.addQuantity(factory,    _, ElineFlux,       flux))                    <|
          (Pio.addQuantity(factory,    _, ElineContinuum,  continuum))

      case AuxFileSpectrum(id, name)  =>
        // User defined distributions are for now only supported on the web app end.
        factory.createParamSet(UserDefinedName)  <|
          (Pio.addParam(factory, _, UserDefinedProgramId, id.toString)) <|
          (Pio.addParam(factory, _, UserDefinedAuxFile, name))

      case sd: LibraryStar        =>
        factory.createParamSet(LibraryStarName)    <|
          (Pio.addParam(factory, _, LibrarySpectrum, sd.sedSpectrum))

      case sd: LibraryNonStar     =>
        factory.createParamSet(LibraryNonStarName) <|
          (Pio.addParam(factory, _, LibrarySpectrum, sd.sedSpectrum))

      case _                      =>
        sys.error("unsupported spectral distribution")

    }

  /** Reads a spectral distribution from a param set. */
  def distributionFromParamSet(pset: ParamSet): Option[SpectralDistribution] = {
    val bbody = Option(pset.getParamSet(BlackBodyName)).map { p =>
      BlackBody(Pio.getDoubleValue(p, BlackBodyTemp, 0))
    }
    val plaw = Option(pset.getParamSet(PowerLawName)).map { p =>
      PowerLaw(Pio.getDoubleValue(p, PowerLawIndex, 0))
    }
    val eline = Option(pset.getParamSet(EmissionLineName)).map { p =>
      EmissionLine(
        Pio.getDoubleValue(p, ElineWavelength, 0).nm,
        Pio.getQuantity(p, ElineWidth,     Velocity),
        Pio.getQuantity(p, ElineFlux,      Irradiance),
        Pio.getQuantity(p, ElineContinuum, SpectralIrradiance))
    }
    val star = Option(pset.getParamSet(LibraryStarName)).flatMap { p =>
      LibraryStar.findByName(Pio.getValue(p, LibrarySpectrum))
    }
    val nonStar = Option(pset.getParamSet(LibraryNonStarName)).flatMap { p =>
      LibraryNonStar.findByName(Pio.getValue(p, LibrarySpectrum))
    }
    val userDefined = Option(pset.getParamSet(UserDefinedName)).map { p =>
      AuxFileSpectrum(
        Pio.getValue(p, UserDefinedProgramId),
        Pio.getValue(p, UserDefinedAuxFile))
    }

    // at most one of these will be defined
    bbody.orElse(plaw).orElse(eline).orElse(star).orElse(nonStar).orElse(userDefined)
  }

  /** Turns a spatial profile into a param set. */
  def toParamSet(spatialProfile: SpatialProfile, factory: PioFactory): ParamSet = {
    spatialProfile match {

      case PointSource          =>
        factory.createParamSet(PointSourceName)

      case GaussianSource(fwhm) =>
        factory.createParamSet(GaussianSourceName) <|
          (Pio.addDoubleParam(factory, _, GaussianSourceFwhm, fwhm))

      case UniformSource        =>
        factory.createParamSet(UniformSourceName)
    }

  }

  /** Reads a spatial profile from a param set. */
  def profileFromParamSet(pset: ParamSet): Option[SpatialProfile] = {
    val point = Option(pset.getParamSet(PointSourceName)).map(_ => PointSource)
    val uni = Option(pset.getParamSet(UniformSourceName)).map(_ => UniformSource)
    val gauss = Option(pset.getParamSet(GaussianSourceName)).map { p =>
      GaussianSource(Pio.getDoubleValue(p, GaussianSourceFwhm, 0))
    }

    // at most one of these will be defined
    point.orElse(gauss).orElse(uni)
  }

}
