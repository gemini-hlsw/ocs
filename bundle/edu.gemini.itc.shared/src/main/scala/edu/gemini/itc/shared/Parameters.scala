package edu.gemini.itc.shared

import edu.gemini.spModel.core.Wavelength

// ==== Source spatial profile

sealed trait SpatialProfile {
  val norm: Double
  val units: BrightnessUnit
}
final case class PointSource(norm: Double, units: BrightnessUnit) extends SpatialProfile
final case class GaussianSource(norm: Double, units: BrightnessUnit, fwhm: Double) extends SpatialProfile {
  require (fwhm >= 0.1, "Please use a Gaussian FWHM greater than 0.1")
}
final case class UniformSource(norm: Double, units: BrightnessUnit) extends SpatialProfile


// ==== Source spectral distribution

sealed trait SpectralDistribution
final case class BlackBody(temperature: Double) extends SpectralDistribution
final case class PowerLaw(index: Double) extends SpectralDistribution
final case class EmissionLine(wavelength: Wavelength, width: Double, flux: Double, fluxUnits: String, continuum: Double, continuumUnits: String) extends SpectralDistribution
final case class UserDefined(spectrum: String) extends SpectralDistribution
sealed trait Library extends SpectralDistribution {
  val sedSpectrum: String
}

sealed case class LibraryStar(sedSpectrum: String) extends Library
object LibraryStar {
  /** Finds the library star identified by the given spectrum name (i.e. the ITC file name). */
  def findByName(n: String) = values.find(_.sedSpectrum.equals(n))

  /** All available library stars. */
  val values = List(
    LibraryStar("O5V"),
    LibraryStar("O8III"),
    LibraryStar("B0V"),
    LibraryStar("B5-7V"),
    LibraryStar("B5III"),
    LibraryStar("B5I"),
    LibraryStar("A0V"),
    LibraryStar("A0III"),
    LibraryStar("A0I"),
    LibraryStar("A5V"),
    LibraryStar("A5III"),
    LibraryStar("F0V"),
    LibraryStar("F0III"),
    LibraryStar("F0I"),
    LibraryStar("F5V"),
    LibraryStar("F5V-w"),
    LibraryStar("F6V-r"),
    LibraryStar("F5III"),
    LibraryStar("F5I"),
    LibraryStar("G0V"),
    LibraryStar("G0V-w"),
    LibraryStar("G0V-r"),
    LibraryStar("G0III"),
    LibraryStar("G0I"),
    LibraryStar("G2V"),
    LibraryStar("G5V"),
    LibraryStar("G5V-w"),
    LibraryStar("G5V-r"),
    LibraryStar("G5III"),
    LibraryStar("G5III-w"),
    LibraryStar("G5III-r"),
    LibraryStar("G5I"),
    LibraryStar("K0V"),
    LibraryStar("K0V-r"),
    LibraryStar("K0III-w"),
    LibraryStar("K0III-r"),
    LibraryStar("K0-1II"),
    LibraryStar("K4V"),
    LibraryStar("K4III"),
    LibraryStar("K4III-w"),
    LibraryStar("K4III-r"),
    LibraryStar("K4I"),
    LibraryStar("M0V"),
    LibraryStar("M0III"),
    LibraryStar("M3V"),
    LibraryStar("M3III"),
    LibraryStar("M6V"),
    LibraryStar("M6III"),
    LibraryStar("M9III")
  )
}

sealed case class LibraryNonStar(label: String, sedSpectrum: String) extends Library
object LibraryNonStar {
  /** Finds a non-star spectrum identified by the given spectrum name (i.e. the ITC file name). */
  def findByName(n: String) = values.find(_.sedSpectrum.equals(n))

  /** All available non-star spectras. */
  val values = List(
    LibraryNonStar("Elliptical Galaxy",                       "elliptical-galaxy"),
    LibraryNonStar("Spiral Galaxy (Sc)",                      "spiral-galaxy"),
    LibraryNonStar("QSO (80-855nm)",                          "QSO"),
    LibraryNonStar("QSO (276-3520nm)",                        "QSO2"),
    LibraryNonStar("HII region (Orion)",                      "Orion-nebula"),
    LibraryNonStar("Planetary nebula (NGC7009: 100-1100nm)",  "Planetary-nebula"),
    LibraryNonStar("Planetary nebula (IC5117: 480-2500nm)",   "Planetary-nebula2"),
    LibraryNonStar("Planetary nebula (NGC7027)",              "Planetary-nebula-NGC7027"),
    LibraryNonStar("Starburst galaxy (M82)",                  "Starburst-galaxy"),
    LibraryNonStar("Pre-main sequence star (HD100546)",       "PMS-star"),
    LibraryNonStar("Galactic center",                         "Galactic-center"),
    LibraryNonStar("AFGL230 (M10II star, silicate absorp.)",  "afgl230"),
    LibraryNonStar("AFGL3068 (Late N-type star)",             "afgl3068"), // orig desc: "AFGL3068 (Late N-type star, optically thick carbon based dust)"
    LibraryNonStar("Alpha Boo (K1.5III star)",                "alphaboo"),
    LibraryNonStar("Alpha Car (F0II star)",                   "alphacar"),
    LibraryNonStar("Beta And (M0IIIa star)",                  "betaand"),
    LibraryNonStar("Beta Gru (M5III star)",                   "betagru"),
    LibraryNonStar("Gamma Cas (B0IVe star)",                  "gammacas"),
    LibraryNonStar("Gamma Dra (K5III star)",                  "gammadra"),
    LibraryNonStar("l1551irs (young stellar object)",         "l1551irs"),
    LibraryNonStar("NGC 1068 (Dusty active galaxy)",          "ngc1068"),
    LibraryNonStar("NGC2023 (Reflection Nebula)",             "ngc2023"),
    LibraryNonStar("NGC2440 (line dominated PN)",             "ngc2440"),
    LibraryNonStar("O Cet (M7IIIa Star, silicate emission)",  "ocet"),
    LibraryNonStar("Orion Bar (Dusty HII region)",            "orionbar"),
    LibraryNonStar("rscl (N-type Dusty Carbon Star, SiC em.)","rscl"),
    LibraryNonStar("txpsc (N-type Visible Carbon Star)",      "txpsc"),
    LibraryNonStar("WR 104 (Wolf-Rayet Star + dust)",         "wr104"),
    LibraryNonStar("WR 34 (Wolf-Rayet Star)",                 "wr34")
  )
}


// ==== Calculation method

// TODO: We can probably get away with only IntegrationTime and S2N methods.
// TODO: The difference between spectroscopy and imaging can/should be deduced from the instrument settings!

sealed trait CalculationMethod {
  val fraction: Double
  val isIntTime: Boolean
  def isS2N: Boolean = !isIntTime
  val isImaging: Boolean
  def isSpectroscopy: Boolean = !isImaging
}
sealed trait Imaging extends CalculationMethod {
  val isImaging = true
}
sealed trait Spectroscopy extends CalculationMethod {
  val isImaging = false
}
final case class ImagingSN(exposures: Int, time: Double, fraction: Double) extends Imaging {
  val isIntTime = false
}
final case class ImagingInt(sigma: Double, expTime: Double, fraction: Double) extends Imaging {
  val isIntTime = true
}
final case class SpectroscopySN(exposures: Int, time: Double, fraction: Double) extends Spectroscopy {
  val isIntTime = false
}


// ==== Analysis method

sealed trait AnalysisMethod {
  val skyAperture: Double
}
final case class AutoAperture(skyAperture: Double) extends AnalysisMethod
final case class UserAperture(diameter: Double, skyAperture: Double) extends AnalysisMethod


// ===== IFU (GMOS & NIFS)

// TODO: Is this an analysis method (instead of the ones above?). If so, should this be reflected here?
sealed trait IfuMethod
final case class IfuSingle(offset: Double) extends IfuMethod
final case class IfuRadial(minOffset: Double, maxOffset: Double) extends IfuMethod
final case class IfuSummed(numX: Int, numY: Int, centerX: Double, centerY: Double) extends IfuMethod


// ===== RESULTS
final case class Parameters(source: SourceDefinition, observation: ObservationDetails, conditions: ObservingConditions, telescope: TelescopeDetails)

