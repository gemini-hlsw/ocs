package edu.gemini.spModel.target

import java.util.logging.{Level, Logger}

import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.target.EmissionLine.Continuum
import squants.motion.MetersPerSecond
import squants.radio.WattsPerSquareMeter
import squants.space.Nanometers

import scalaz.Scalaz._
import scalaz._

object SourcePio {
  private val LOGGER: Logger = Logger.getLogger(SourcePio.getClass.getName)

  // Spatial profile
  private val PointSourceName     = classOf[PointSource].getSimpleName

  private val GaussianSourceName  = classOf[GaussianSource].getSimpleName
  private val GaussianSourceFwhm  = "fwhm"

  private val UniformSourceName   = classOf[UniformSource].getSimpleName


  // Spectral distribution
  private val BlackBodyName       = classOf[BlackBody].getSimpleName
  private val BlackBodyTemp       = "temperature"

  private val PowerLawName        = classOf[PowerLaw].getSimpleName
  private val PowerLawIndex       = "index"

  private val EmissionLineName    = classOf[EmissionLine].getSimpleName
  private val ElineWavelength     = "wavelength"
  private val ElineWidth          = "width"
  private val ElineFlux           = "flux"
  private val ElineContinuum      = "continuum"

  private val LibraryStarName     = classOf[LibraryStar].getSimpleName
  private val LibraryNonStarName  = classOf[LibraryNonStar].getSimpleName
  private val LibrarySpectrum     = "spectrum"


  /** Turns a spectral distribution into a param set. */
  def toParamSet(spectralDistribution: SpectralDistribution, factory: PioFactory): ParamSet =
    spectralDistribution match {

      case sd: BlackBody        =>
        factory.createParamSet(BlackBodyName)     <|
          (Pio.addDoubleParam(factory, _, BlackBodyTemp, sd.temperature))

      case sd: PowerLaw         =>
        factory.createParamSet(PowerLawName)      <|
          (Pio.addDoubleParam(factory, _, PowerLawIndex, sd.index))

      case sd: EmissionLine     =>
        factory.createParamSet(EmissionLineName)  <|
          (Pio.addDoubleParam(factory, _, ElineWavelength, sd.wavelength.toNanometers))   <|
          (Pio.addDoubleParam(factory, _, ElineWidth,      sd.width.toMetersPerSecond))   <|
          (Pio.addDoubleParam(factory, _, ElineFlux,       sd.flux.toWattsPerSquareMeter))<|
          (Pio.addDoubleParam(factory, _, ElineContinuum,  sd.continuum.toWatts))

      case sd: UserDefined      =>
        // User defined distributions are for now only supported on the web app end.
        // This is only here to satisfy the compiler. However at some point in the future
        // we will add aux file support in the OT.
        throw new Error("user defined spectral distributions are not supported")

      case sd: LibraryStar      =>
        factory.createParamSet(LibraryStarName)    <|
          (Pio.addParam(factory, _, LibrarySpectrum, sd.sedSpectrum))

      case sd: LibraryNonStar   =>
        factory.createParamSet(LibraryNonStarName) <|
          (Pio.addParam(factory, _, LibrarySpectrum, sd.sedSpectrum))

    }

  /** Reads a spectral distribution from a param set. */
  def distributionFromParamSet(pset: ParamSet): Option[SpectralDistribution] =
    try {

      val bbody = Option(pset.getParamSet(BlackBodyName)).map { p =>
        BlackBody(Pio.getDoubleValue(p, BlackBodyTemp, 0))
      }
      val plaw = Option(pset.getParamSet(PowerLawName)).map { p =>
        PowerLaw(Pio.getDoubleValue(p, PowerLawIndex, 0))
      }
      val eline = Option(pset.getParamSet(EmissionLineName)).map { p =>
        EmissionLine(
          Nanometers(Pio.getDoubleValue(p, ElineWavelength, 0)),
          MetersPerSecond(Pio.getDoubleValue(p, ElineWidth, 0)),
          WattsPerSquareMeter(Pio.getDoubleValue(p, ElineFlux, 0)),
          Continuum.fromWatts(Pio.getDoubleValue(p, ElineContinuum, 0))
        )
      }
      val star = Option(pset.getParamSet(LibraryStarName)).flatMap { p =>
        LibraryStar.findByName(Pio.getValue(p, LibrarySpectrum))
      }
      val nonStar = Option(pset.getParamSet(LibraryNonStarName)).flatMap { p =>
        LibraryNonStar.findByName(Pio.getValue(p, LibrarySpectrum))
      }

      // one of these should be defined
      bbody.orElse(plaw).orElse(eline).orElse(star).orElse(nonStar)

    } catch {
      case e: Exception =>
        LOGGER.log(Level.WARNING, "Could not parse source distribution", e)
        None
    }


  /** Turns a spatial profile into a param set. */
  def toParamSet(spatialProfile: SpatialProfile, factory: PioFactory): ParamSet = {
    spatialProfile match {

      case sp: PointSource      =>
        factory.createParamSet(PointSourceName)

      case sp: GaussianSource   =>
        factory.createParamSet(GaussianSourceName) <|
          (Pio.addDoubleParam(factory, _, GaussianSourceFwhm, sp.fwhm))

      case sp: UniformSource    =>
        factory.createParamSet(UniformSourceName)
    }

  }

  /** Reads a spatial profile from a param set. */
  def profileFromParamSet(pset: ParamSet): Option[SpatialProfile] =
    try {

      val point = Option(pset.getParamSet(PointSourceName)).map(_ => PointSource())
      val uni = Option(pset.getParamSet(UniformSourceName)).map(_ => UniformSource())
      val gauss = Option(pset.getParamSet(GaussianSourceName)).map { p =>
        GaussianSource(Pio.getDoubleValue(p, GaussianSourceFwhm, 0))
      }

      // one of these should be defined
      point.orElse(gauss).orElse(uni)

    } catch {
      case e: Exception =>
        LOGGER.log(Level.WARNING, "Could not parse spatial profile", e)
        None
    }

}
