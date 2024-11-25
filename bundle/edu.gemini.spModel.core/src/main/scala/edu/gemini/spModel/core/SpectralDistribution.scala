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

/** Stars */
sealed abstract class LibraryStar(val label: String, val sedSpectrum: String) extends Library
object LibraryStar {

  /** Finds the library star identified by the spectrum name. */
  def findByName(n: String): Option[LibraryStar] = Values.find(_.sedSpectrum == n)

  /** Known library stars. */
  // O
  case object O5V     extends LibraryStar("O5V", "o5v")
  case object O8III   extends LibraryStar("O8III", "o8iii")
  case object O9V     extends LibraryStar("O9V model", "O9V")
  case object O95V    extends LibraryStar("O9.5V model", "O9.5V")
  // B
  case object B0V     extends LibraryStar("B0V", "b0v")
  case object B05V    extends LibraryStar("B0.5V model", "B0.5V")
  case object B3V     extends LibraryStar("B3V model", "B3V")
  case object B5_7V   extends LibraryStar("B5-7V", "b5-7v")
  case object B5III   extends LibraryStar("B5III", "b5iii")
  case object B5I     extends LibraryStar("B5I", "b5i")
  case object B9III   extends LibraryStar("B9III model", "B9III")
  // A
  case object A0I     extends LibraryStar("A0I", "a0i")
  case object A0III   extends LibraryStar("A0III", "a0iii")
  case object A0III_new extends LibraryStar("A0III model", "A0III")
  case object A0V     extends LibraryStar("A0V", "a0v")
  case object A0V_new extends LibraryStar("A0V model", "A0V")
  case object A1V     extends LibraryStar("A1V model", "A1V")
  case object A2V     extends LibraryStar("A2V model", "A2V")
  case object A3V     extends LibraryStar("A3V model", "A3V")
  case object A4V     extends LibraryStar("A4V model", "A4V")
  case object A5III   extends LibraryStar("A5III", "a5iii")
  case object A5V     extends LibraryStar("A5V", "a5v")
  case object A5V_new extends LibraryStar("A5V model", "A5V")
  case object A6V     extends LibraryStar("A6V model", "A6V")
  case object A8III   extends LibraryStar("A8III model", "A8III")
  // F
  case object F0I     extends LibraryStar("F0I", "f0i")
  case object F0I_new extends LibraryStar("F0I", "F0I")
  case object F0II    extends LibraryStar("F0II", "F0II")
  case object F0III   extends LibraryStar("F0III", "f0iii")
  case object F0III_new extends LibraryStar("F0III", "F0III")
  case object F0IV    extends LibraryStar("F0IV", "F0IV")
  case object F0V     extends LibraryStar("F0V", "f0v")
  case object F0V_new extends LibraryStar("F0V", "F0V")
  case object F2II    extends LibraryStar("F2II", "F2II")
  case object F2III   extends LibraryStar("F2III", "F2III")
  case object F2V     extends LibraryStar("F2V", "F2V")
  case object F4V     extends LibraryStar("F4V", "F4V")
  case object F5V     extends LibraryStar("F5V", "f5v")
  case object F5V_new extends LibraryStar("F5V", "F5V")
  case object F5V_w   extends LibraryStar("F5V-w", "f5v-w")
  case object F6V_r   extends LibraryStar("F6V-r", "f6v-r")
  case object F5III   extends LibraryStar("F5III", "f5iii")
  case object F5III_new extends LibraryStar("F5III", "F5III")
  case object F5I     extends LibraryStar("F5I", "f5i")
  case object F5I_new extends LibraryStar("F5I", "F5I")
  case object F7V     extends LibraryStar("F7V model", "F7V")
  case object F8I     extends LibraryStar("F8I", "F8I")
  case object F8IV    extends LibraryStar("F8IV model", "F8IV")
  case object F8V     extends LibraryStar("F8V", "F8V")
  // G
  case object G0I     extends LibraryStar("G0I", "g0i")
  case object G0I_new extends LibraryStar("G0I", "G0I")
  case object G0III   extends LibraryStar("G0III", "g0iii")
  case object G0V     extends LibraryStar("G0V", "g0v")
  case object G0V_new extends LibraryStar("G0V model", "G0V")
  case object G0V_w   extends LibraryStar("G0V-w", "g0v-w")
  case object G0V_r   extends LibraryStar("G0V-r", "g0v-r")
  case object G1V     extends LibraryStar("G1V model", "G1V")
  case object G2I     extends LibraryStar("G2I", "G2I")
  case object G2IV    extends LibraryStar("G2IV", "G2IV")
  case object G2V     extends LibraryStar("G2V", "g2v")
  case object G2V_new extends LibraryStar("G2V", "G2V")
  case object G3V     extends LibraryStar("G3V model", "G3V")
  case object G5I     extends LibraryStar("G5I", "g5i")
  case object G5I_new extends LibraryStar("G5I", "G5I")
  case object G5III   extends LibraryStar("G5III", "g5iii")
  case object G5III_new extends LibraryStar("G5III", "G5III")
  case object G5III_w extends LibraryStar("G5III-w", "g5iii-w")
  case object G5III_r extends LibraryStar("G5III-r", "g5iii-r")
  case object G5V     extends LibraryStar("G5V", "g5v")
  case object G5V_new extends LibraryStar("G5V model", "G5V")
  case object G5V_w   extends LibraryStar("G5V-w", "g5v-w")
  case object G5V_r   extends LibraryStar("G5V-r", "g5v-r")
  case object G7III   extends LibraryStar("G7III model", "G7III")
  case object G8I     extends LibraryStar("G8I", "G8I")
  case object G8III   extends LibraryStar("G8III", "G8III")
  case object G8V     extends LibraryStar("G8V", "G8V")
  // K
  case object K0III   extends LibraryStar("K0III", "k0iii")
  case object K0III_new extends LibraryStar("K0III", "K0III")
  case object K0III_w extends LibraryStar("K0III-w", "k0iii-w")
  case object K0III_r extends LibraryStar("K0III-r", "k0iii-r")
  case object K0IV    extends LibraryStar("K0IV", "K0IV")
  case object K0V     extends LibraryStar("K0V", "k0v")
  case object K0V_new extends LibraryStar("K0V", "K0V")
  case object K0V_r   extends LibraryStar("K0V-r", "k0v-r")
  case object K05III  extends LibraryStar("K0.5III model", "K0.5III")
  case object K0_1II  extends LibraryStar("K0-1II", "k0-1ii")
  case object K15III  extends LibraryStar("K1.5III", "K1.5III")
  case object K2I     extends LibraryStar("K2I", "K2I")
  case object K2III   extends LibraryStar("K2III", "K2III")
  case object K2V     extends LibraryStar("K2V", "K2V")
  case object K3II    extends LibraryStar("K3II", "K3II")
  case object K3III   extends LibraryStar("K3III", "K3III")
  case object K3V     extends LibraryStar("K3V", "K3V")
  case object K4I     extends LibraryStar("K4I", "k4i")
  case object K4I_new extends LibraryStar("K4I", "K4I")
  case object K4III   extends LibraryStar("K4III", "k4iii")
  case object K4III_new extends LibraryStar("K4III", "K4III")
  case object K4III_w extends LibraryStar("K4III-w", "k4iii-w")
  case object K4III_r extends LibraryStar("K4III-r", "k4iii-r")
  case object K4V     extends LibraryStar("K4V", "k4v")
  case object K5III   extends LibraryStar("K5III", "K5III")
  case object K5V     extends LibraryStar("K5V", "K5V")
  // M
  case object M0III   extends LibraryStar("M0III", "m0iii")
  case object M0III_new extends LibraryStar("M0III", "M0III")
  case object M0V     extends LibraryStar("M0V", "m0v")
  case object M0V_new extends LibraryStar("M0V", "M0V")
  case object M1III   extends LibraryStar("M1III", "M1III")
  case object M1V     extends LibraryStar("M1V", "M1V")
  case object M2I     extends LibraryStar("M2I", "M2I")
  case object M2III   extends LibraryStar("M2III", "M2III")
  case object M2V     extends LibraryStar("M2V", "M2V")
  case object M3III   extends LibraryStar("M3III", "m3iii")
  case object M3III_new extends LibraryStar("M3III", "M3III")
  case object M3V     extends LibraryStar("M3V", "m3v")
  case object M3V_new extends LibraryStar("M3V", "M3V")
  case object M4III   extends LibraryStar("M4III", "M4III")
  case object M4V     extends LibraryStar("M4V", "M4V")
  case object M5V     extends LibraryStar("M5V", "M5V")
  case object M6III   extends LibraryStar("M6III", "m6iii")
  case object M6III_new extends LibraryStar("M6III", "M6III")
  case object M6V     extends LibraryStar("M6V", "m6v")
  case object M7III   extends LibraryStar("M7III", "M7III")
  case object M8III   extends LibraryStar("M8III", "M8III")
  case object M9III   extends LibraryStar("M9III", "m9iii")
  // sub-dwarfs
  case object sdB     extends LibraryStar("sdB", "sdB")
  case object sdF8    extends LibraryStar("sdF8", "sdF8")
  case object sdO     extends LibraryStar("sdO", "sdO")
  // white dwarfs
  case object DA08    extends LibraryStar("DA.8", "DA08")
  case object DA09    extends LibraryStar("DA.9", "DA09")
  case object DA12    extends LibraryStar("DA1.2", "DA12")
  case object DA15    extends LibraryStar("DA1.5", "DA15")
  case object DA18    extends LibraryStar("DA1.8", "DA18")
  case object DA24    extends LibraryStar("DA2.4", "DA24")
  case object DA28    extends LibraryStar("DA2.8", "DA28")
  case object DA30    extends LibraryStar("DA3.0", "DA30")
  case object DA31    extends LibraryStar("DA3.1", "DA31")
  case object DA33    extends LibraryStar("DA3.3", "DA33")
  case object DA36    extends LibraryStar("DA3.6", "DA36")
  case object DA38    extends LibraryStar("DA3.8", "DA38")
  case object DA48    extends LibraryStar("DA4.8", "DA48")
  case object DA57    extends LibraryStar("DA5.7", "DA57")
  case object DBQ40   extends LibraryStar("DBQ4", "DBQ40")
  case object DBQA50  extends LibraryStar("DBQA5", "DBQA50")
  case object DO20    extends LibraryStar("DO2", "DO20")
  // cool star models
  case object T2800K extends LibraryStar("T=2800K model", "t2800k")
  case object T2600K extends LibraryStar("T=2600K model", "t2600k")
  case object T2400K extends LibraryStar("T=2400K model", "t2400k")
  case object T2200K extends LibraryStar("T=2200K model", "t2200k")
  case object T2000K extends LibraryStar("T=2000K model", "t2000k")
  case object T1800K extends LibraryStar("T=1800K model", "t1800k")
  case object T1600K extends LibraryStar("T=1600K model", "t1600k")
  case object T1400K extends LibraryStar("T=1400K model", "t1400k")
  case object T1200K extends LibraryStar("T=1200K model", "t1200k")
  case object T1000K extends LibraryStar("T=1000K model", "t1000k")
  case object T0900K extends LibraryStar("T=900K model", "t0900k")
  case object T0800K extends LibraryStar("T=800K model", "t0800k")
  case object T0600K extends LibraryStar("T=600K model", "t0600k")
  case object T0400K extends LibraryStar("T=400K model", "t0400k")


  /** All available library stars. */
  val Values: List[LibraryStar] = List(
    O5V, O8III, O9V, O95V,
    B0V, B05V, B3V, B5_7V, B5III, B5I, B9III,
    A0I, A0III, A0III_new, A0V, A0V_new, A1V, A2V, A3V, A4V, A5V, A5V_new, A5III, A6V, A8III,
    F0I, F0I_new, F0II, F0III, F0III_new, F0IV, F0V, F0V_new, F2II, F2III, F2V, F4V, F5I, F5I_new, F5III, F5III_new, F5V, F5V_new, F5V_w, F6V_r, F7V, F8I, F8IV, F8V,
    G0I, G0I_new, G0V, G0V_new, G0V_w, G0V_r, G0III, G1V, G2I, G2IV, G2V, G2V_new, G3V, G5I, G5I_new, G5V, G5V_new, G5V_w, G5V_r, G5III, G5III_new, G5III_w, G5III_r, G7III, G8I, G8III, G8V,
    K0III, K0III_new, K0III_w, K0III_r, K0IV, K0V, K0V_new, K0V_r, K0_1II, K05III, K15III, K2I, K2III, K2V, K3II, K3III, K3V, K4I, K4I_new, K4III, K4III_new, K4III_w, K4III_r, K4V, K5III, K5V,
    M0III, M0III_new, M0V, M0V_new, M1III, M1V, M2I, M2III, M2V, M3III, M3III_new, M3V, M3V_new, M4III, M4V, M5V, M6V, M6III, M6III_new, M7III, M8III, M9III,
    sdB, sdF8, sdO,
    DA08, DA09, DA12, DA15, DA18, DA24, DA28, DA30, DA31, DA33, DA36, DA38, DA48, DA57, DBQ40, DBQA50, DO20,
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

  /** Known non-star library objects. */
  case object EllipticalGalaxy  extends LibraryNonStar("Elliptical Galaxy",                       "elliptical-galaxy")
  case object SpiralGalaxy      extends LibraryNonStar("Spiral Galaxy (Sc)",                      "spiral-galaxy")
  case object QS0               extends LibraryNonStar("QSO (80-855nm)",                          "QSO")
  case object QS02              extends LibraryNonStar("QSO (276-3520nm)",                        "QSO2")
  case object OrionNebula       extends LibraryNonStar("HII region (Orion)",                      "Orion-nebula")
  case object PlanetaryNebula   extends LibraryNonStar("Planetary nebula (NGC7009)",              "Planetary-nebula")
  case object PlanetaryNebula2  extends LibraryNonStar("Planetary nebula (IC5117)",               "Planetary-nebula2")
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


  /** All available non-star spectra. */
  val values: List[LibraryNonStar] = List(
    EllipticalGalaxy, SpiralGalaxy, QS0, QS02, OrionNebula, PlanetaryNebula, PlanetaryNebula2, PlanetaryNebula3,
    StarburstGalaxy, PmsStar, GalacticCenter, Afgl230, Afgl3068, AlphaBoo, AlphaCar, BetaAnd, BetaGru, GammaCas,
    GammaDra, L1511Irs, NGC1068, NGC2023, NGC2440, OCet, OrionBar, Rscl, Txpsc, Wr104, Wr34,
    Mars, Jupiter, Saturn, Uranus, Neptune
  )
}

