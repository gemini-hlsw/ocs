package edu.gemini.spModel.core

import squants.motion.Velocity
import squants.radio.{Irradiance, SpectralIrradiance}

/** Definitions for the spectral distribution of a source.
  * A source can be anything from a star, galaxy, quasar or planet to a comet or asteroid. */
sealed trait SpectralDistribution extends Serializable

/** A black body with a temperature in Kelvin. */
final case class BlackBody(temperature: Double) extends SpectralDistribution

/** Defined by power law function. */
final case class PowerLaw(index: Double) extends SpectralDistribution

/** A single emission line. */
final case class EmissionLine(wavelength: Wavelength, width: Velocity, flux: Irradiance, continuum: SpectralIrradiance) extends SpectralDistribution

/** A user defined spectrum. */
sealed trait UserDefined extends SpectralDistribution {
  def name: String
}
/** User defined spectrum with a name and a spectrum definition as a string. */
final case class UserDefinedSpectrum(name: String, spectrum: String) extends UserDefined
/** A place holder for a user defined spectrum defined by an ODB auxiliary file for the given program. */
final case class AuxFileSpectrum(programId: String, name: String) extends UserDefined
object AuxFileSpectrum {
  // Place holder for invalid states, e.g. the user has selected "User Defined" for the source distribution
  // but there are no sed files or the selected aux file has been deleted.
  val Undefined = AuxFileSpectrum("«undefined»", "«No SEDs attached to program»")
}

/** A library defined spectrum. */
sealed trait Library extends SpectralDistribution {
  val sedSpectrum: String
}

/** A star. */
sealed abstract class LibraryStar(val sedSpectrum: String) extends Library
object LibraryStar {

  /** Finds the library star identified by the spectrum name. */
  def findByName(n: String): Option[LibraryStar] = Values.find(_.sedSpectrum == n)

  /** Known library stars. */
  case object O5V     extends LibraryStar("O5V")
  case object O8III   extends LibraryStar("O8III")
  case object B0V     extends LibraryStar("B0V")
  case object B5_7V   extends LibraryStar("B5-7V")
  case object B5III   extends LibraryStar("B5III")
  case object B5I     extends LibraryStar("B5I")
  case object A0V     extends LibraryStar("A0V")
  case object A0III   extends LibraryStar("A0III")
  case object A0I     extends LibraryStar("A0I")
  case object A5V     extends LibraryStar("A5V")
  case object A5III   extends LibraryStar("A5III")
  case object F0V     extends LibraryStar("F0V")
  case object F0III   extends LibraryStar("F0III")
  case object F0I     extends LibraryStar("F0I")
  case object F5V     extends LibraryStar("F5V")
  case object F5V_w   extends LibraryStar("F5V-w")
  case object F6V_r   extends LibraryStar("F6V-r")
  case object F5III   extends LibraryStar("F5III")
  case object F5I     extends LibraryStar("F5I")
  case object G0V     extends LibraryStar("G0V")
  case object G0V_w   extends LibraryStar("G0V-w")
  case object G0V_r   extends LibraryStar("G0V-r")
  case object G0III   extends LibraryStar("G0III")
  case object G0I     extends LibraryStar("G0I")
  case object G2V     extends LibraryStar("G2V")
  case object G5V     extends LibraryStar("G5V")
  case object G5V_w   extends LibraryStar("G5V-w")
  case object G5V_r   extends LibraryStar("G5V-r")
  case object G5III   extends LibraryStar("G5III")
  case object G5III_w extends LibraryStar("G5III-w")
  case object G5III_r extends LibraryStar("G5III-r")
  case object G5I     extends LibraryStar("G5I")
  case object K0V     extends LibraryStar("K0V")
  case object K0V_r   extends LibraryStar("K0V-r")
  case object K0III   extends LibraryStar("K0III")
  case object K0III_w extends LibraryStar("K0III-w")
  case object K0III_r extends LibraryStar("K0III-r")
  case object K0_1II  extends LibraryStar("K0-1II")
  case object K4V     extends LibraryStar("K4V")
  case object K4III   extends LibraryStar("K4III")
  case object K4III_w extends LibraryStar("K4III-w")
  case object K4III_r extends LibraryStar("K4III-r")
  case object K4I     extends LibraryStar("K4I")
  case object M0V     extends LibraryStar("M0V")
  case object M0III   extends LibraryStar("M0III")
  case object M3V     extends LibraryStar("M3V")
  case object M3III   extends LibraryStar("M3III")
  case object M6V     extends LibraryStar("M6V")
  case object M6III   extends LibraryStar("M6III")
  case object M9III   extends LibraryStar("M9III")
  case object T2800K extends LibraryStar("T2800K")
  case object T2600K extends LibraryStar("T2600K")
  case object T2400K extends LibraryStar("T2400K")
  case object T2200K extends LibraryStar("T2200K")
  case object T2000K extends LibraryStar("T2000K")
  case object T1800K extends LibraryStar("T1800K")
  case object T1600K extends LibraryStar("T1600K")
  case object T1400K extends LibraryStar("T1400K")
  case object T1200K extends LibraryStar("T1200K")
  case object T1000K extends LibraryStar("T1000K")
  case object T0900K extends LibraryStar("T0900K")
  case object T0800K extends LibraryStar("T0800K")
  case object T0600K extends LibraryStar("T0600K")
  case object T0400K extends LibraryStar("T0400K")


  /** All available library stars. */
  val Values = List(
    O5V,      O8III,      B0V,      B5_7V,      B5III,      B5I,      A0V,      A0III,      A0I,
    A5V,      A5III,      F0V,      F0III,      F0I,        F5V,      F5V_w,    F6V_r,      F5III,
    F5I,      G0V,        G0V_w,    G0V_r,      G0III,      G0I,      G2V,      G5V,        G5V_w,
    G5V_r,    G5III,      G5III_w,  G5III_r,    G5I,        K0V,      K0V_r,    K0III,      K0III_w,
    K0III_r,  K0_1II,     K4V,      K4III,      K4III_w,    K4III_r,  K4I,      M0V,        M0III,
    M3V,      M3III,      M6V,      M6III,      M9III,
    T2800K,   T2600K,     T2400K,   T2200K,     T2000K,
    T1800K,   T1600K,     T1400K,   T1200K,     T1000K,
    T0900K,   T0800K,     T0600K,   T0400K
  )
}

/** A non-star object like galaxies and nebulae. */
sealed abstract class LibraryNonStar(val label: String, val sedSpectrum: String) extends Library
object LibraryNonStar {

  /** Finds a non-star spectrum identified by the given spectrum name (i.e. the ITC file name). */
  def findByName(n: String): Option[LibraryNonStar] = values.find(_.sedSpectrum == n)

  /** Known non-star library obejcts. */
  case object EllipticalGalaxy  extends LibraryNonStar("Elliptical Galaxy",                       "elliptical-galaxy")
  case object SpiralGalaxy      extends LibraryNonStar("Spiral Galaxy (Sc)",                      "spiral-galaxy")
  case object QS0               extends LibraryNonStar("QSO (80-855nm)",                          "QSO")
  case object QS02              extends LibraryNonStar("QSO (276-3520nm)",                        "QSO2")
  case object OrionNebula       extends LibraryNonStar("HII region (Orion)",                      "Orion-nebula")
  case object PlanetaryNebula   extends LibraryNonStar("Planetary nebula (NGC7009: 100-1100nm)",  "Planetary-nebula")
  case object PlanetaryNebula2  extends LibraryNonStar("Planetary nebula (IC5117: 480-2500nm)",   "Planetary-nebula2")
  case object PlanetaryNebula3  extends LibraryNonStar("Planetary nebula (NGC7027)",              "Planetary-nebula-NGC7027")
  case object StarburstGalaxy   extends LibraryNonStar("Starburst galaxy (M82)",                  "Starburst-galaxy")
  case object PmsStar           extends LibraryNonStar("Pre-main sequence star (HD100546)",       "PMS-star")
  case object GalacticCenter    extends LibraryNonStar("Galactic center",                         "Galactic-center")
  case object Afgl230           extends LibraryNonStar("AFGL230 (M10II star, silicate absorp.)",  "afgl230")
  case object Afgl3068          extends LibraryNonStar("AFGL3068 (Late N-type star)",             "afgl3068") // orig desc: "AFGL3068 (Late N-type star, optically thick carbon based dust)"
  case object AlphaBoo          extends LibraryNonStar("Alpha Boo (K1.5III star)",                "alphaboo")
  case object AlphaCar          extends LibraryNonStar("Alpha Car (F0II star)",                   "alphacar")
  case object BetaAnd           extends LibraryNonStar("Beta And (M0IIIa star)",                  "betaand")
  case object BetaGru           extends LibraryNonStar("Beta Gru (M5III star)",                   "betagru")
  case object GammaCas          extends LibraryNonStar("Gamma Cas (B0IVe star)",                  "gammacas")
  case object GammaDra          extends LibraryNonStar("Gamma Dra (K5III star)",                  "gammadra")
  case object L1511Irs          extends LibraryNonStar("l1551irs (young stellar object)",         "l1551irs")
  case object NGC1068           extends LibraryNonStar("NGC 1068 (Dusty active galaxy)",          "ngc1068")
  case object NGC2023           extends LibraryNonStar("NGC2023 (Reflection Nebula)",             "ngc2023")
  case object NGC2440           extends LibraryNonStar("NGC2440 (line dominated PN)",             "ngc2440")
  case object OCet              extends LibraryNonStar("O Cet (M7IIIa Star, silicate emission)",  "ocet")
  case object OrionBar          extends LibraryNonStar("Orion Bar (Dusty HII region)",            "orionbar")
  case object Rscl              extends LibraryNonStar("rscl (N-type Dusty Carbon Star, SiC em.)","rscl")
  case object Txpsc             extends LibraryNonStar("txpsc (N-type Visible Carbon Star)",      "txpsc")
  case object Wr104             extends LibraryNonStar("WR 104 (Wolf-Rayet Star + dust)",         "wr104")
  case object Wr34              extends LibraryNonStar("WR 34 (Wolf-Rayet Star)",                 "wr34")
  case object Mars              extends LibraryNonStar("Mars",                                    "Mars")
  case object Jupiter           extends LibraryNonStar("Jupiter",                                 "Jupiter")
  case object Saturn            extends LibraryNonStar("Saturn",                                  "Saturn")
  case object Uranus            extends LibraryNonStar("Uranus",                                  "Uranus")
  case object Neptune           extends LibraryNonStar("Neptune",                                 "Neptune")


  /** All available non-star spectras. */
  val values = List(
    EllipticalGalaxy, SpiralGalaxy, QS0, QS02, OrionNebula, PlanetaryNebula, PlanetaryNebula2, PlanetaryNebula3,
    StarburstGalaxy, PmsStar, GalacticCenter, Afgl230, Afgl3068, AlphaBoo, AlphaCar, BetaAnd, BetaGru, GammaCas,
    GammaDra, L1511Irs, NGC1068, NGC2023, NGC2440, OCet, OrionBar, Rscl, Txpsc, Wr104, Wr34,
    Mars, Jupiter, Saturn, Uranus, Neptune
  )
}

