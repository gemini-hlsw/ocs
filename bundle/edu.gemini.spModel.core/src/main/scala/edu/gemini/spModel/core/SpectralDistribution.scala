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
sealed abstract class LibraryStar(val label: String, val sedSpectrum: String) extends Library
object LibraryStar {

  /** Finds the library star identified by the spectrum name. */
  def findByName(n: String): Option[LibraryStar] = Values.find(_.sedSpectrum == n)

  /** Known library stars. */
  // O
  case object O5V       extends LibraryStar("O5V (300nm-6.0um)",        "o5v")
  case object O8III     extends LibraryStar("O8III (300nm-6.0um)",      "o8iii")
  case object O9V       extends LibraryStar("O9V (100nm-27um model)",   "O9V_calspec")
  case object O95V      extends LibraryStar("O9.5V (100nm-27um model)", "O9.5V_calspec")
  // B
  case object B0V       extends LibraryStar("B0V (300nm-6.0um)",        "b0v")
  case object B05V      extends LibraryStar("B0.5V (100nm-27um model)", "B0.5V_calspec")
  case object B3V       extends LibraryStar("B3V (100nm-32um model)",   "B3V_calspec")
  case object B5_7V     extends LibraryStar("B5-7V (300nm-6.0um)",      "b5-7v")
  case object B5III     extends LibraryStar("B5III (300nm-6.0um)",      "b5iii")
  case object B5I       extends LibraryStar("B5I (300nm-6.0um)",        "b5i")
  case object B9III     extends LibraryStar("B9III (100nm-32um model)", "B9III_calspec")
  // A
  case object A0I       extends LibraryStar("A0I (300nm-6.0um)",        "a0i")
  case object A0III     extends LibraryStar("A0III (300nm-6.0um)",      "a0iii")
  case object A0III_new extends LibraryStar("A0III (100nm-27um model)", "A0III_calspec")
  case object A0V       extends LibraryStar("A0V (300nm-6.0um)",        "a0v")
  case object A0V_new   extends LibraryStar("A0V (100nm-32um model)",   "A0V_calspec")
  case object A1V       extends LibraryStar("A1V (100nm-32um model)",   "A1V_calspec")
  case object A2V       extends LibraryStar("A2V (100nm-27um model)",   "A2V_calspec")
  case object A3V       extends LibraryStar("A3V (100nm-27um model)",   "A3V_calspec")
  case object A4V       extends LibraryStar("A4V (100nm-27um model)",   "A4V_calspec")
  case object A5III     extends LibraryStar("A5III (300nm-6.0um)",      "a5iii")
  case object A5V       extends LibraryStar("A5V (300nm-6.0um)",        "a5v")
  case object A5V_new   extends LibraryStar("A5V (100nm-27um model)",   "A5V_calspec")
  case object A6V       extends LibraryStar("A6V (100nm-32um model)",   "A6V_calspec")
  case object A8III     extends LibraryStar("A8III (100nm-27um model)", "A8III_calspec")
  // F
  case object F0I       extends LibraryStar("F0I (300nm-6.0um)",       "f0i")
  case object F0I_new   extends LibraryStar("F0I (115nm-4.15um)",      "F0I_pickles_irtf")
  case object F0II      extends LibraryStar("F0II (115nm-4.07um)",     "F0II_pickles_irtf")
  case object F0III     extends LibraryStar("F0III (300nm-6.0um)",     "f0iii")
  case object F0III_new extends LibraryStar("F0III (115nm-4.16um)",    "F0III_pickles_irtf")
  case object F0IV      extends LibraryStar("F0IV (115nm-5.04um)",     "F0IV_pickles_irtf")
  case object F0V       extends LibraryStar("F0V (300nm-6.0um)",       "f0v")
  case object F0V_new   extends LibraryStar("F0V (115nm-4.16um)",      "F0V_pickles_irtf")
  case object F2II      extends LibraryStar("F2II (115nm-4.08um)",     "F2II_pickles_irtf")
  case object F2III     extends LibraryStar("F2III (115nm-5.00um)",    "F2III_pickles_irtf")
  case object F2V       extends LibraryStar("F2V (115nm-4.92um)",      "F2V_pickles_irtf")
  case object F4V       extends LibraryStar("F4V (100nm-27um)",        "F4V_calspec")
  case object F5I       extends LibraryStar("F5I (300nm-6.0um)",       "f5i")
  case object F5I_new   extends LibraryStar("F5I (115nm-5.0um)",       "F5I_pickles_irtf")
  case object F5III     extends LibraryStar("F5III (300nm-6.0um)",     "f5iii")
  case object F5III_new extends LibraryStar("F5III (115nm-5.41um)",    "F5III_pickles_irtf")
  case object F5V       extends LibraryStar("F5V (300nm-6.0um)",       "f5v")
  case object F5V_new   extends LibraryStar("F5V (115nm-5.01um)",      "F5V_pickles_irtf")
  case object F5V_w     extends LibraryStar("F5V-w (300nm-6.0um)",     "f5v-w")
  case object F6V_r     extends LibraryStar("F6V-r (300nm-6.0um)",     "f6v-r")
  case object F7V       extends LibraryStar("F7V (100nm-27um model)",  "F7V_calspec")
  case object F8I       extends LibraryStar("F8I (115nm-4.98um)",      "F8I_pickles_irtf")
  case object F8IV      extends LibraryStar("F8IV (100nm-27um model)", "F8IV_calspec")
  case object F8V       extends LibraryStar("F8V (115nm-5.41um)",      "F8V_pickles_irtf")
  // G
  case object G0I       extends LibraryStar("G0I (300nm-6.0um)",        "g0i")
  case object G0I_new   extends LibraryStar("G0I (115nm-5.01um)",       "G0I_pickles_irtf")
  case object G0III     extends LibraryStar("G0III (300nm-6.0um)",      "g0iii")
  case object G0V       extends LibraryStar("G0V (300nm-6.0um)",        "g0v")
  case object G0V_new   extends LibraryStar("G0V (100nm-27um model)",   "G0V_calspec")
  case object G0V_w     extends LibraryStar("G0V-w (300nm-6.0um)",      "g0v-w")
  case object G0V_r     extends LibraryStar("G0V-r (300nm-6.0um)",      "g0v-r")
  case object G1V       extends LibraryStar("G1V (100nm-32um model)",   "G1V_calspec")
  case object G2I       extends LibraryStar("G2I (115nm-5.03um)",       "G2I_pickles_irtf")
  case object G2IV      extends LibraryStar("G2IV (115nm-5.08um)",      "G2IV_pickles_irtf")
  case object G2V       extends LibraryStar("G2V (300nm-6.0um)",        "g2v")
  case object G2V_new   extends LibraryStar("G2V (100nm-32um model)",   "G2V_calspec")
  case object G3V       extends LibraryStar("G3V (100nm-32um model)",   "G3V_calspec")
  case object G5I       extends LibraryStar("G5I (300nm-6.0um)",        "g5i")
  case object G5I_new   extends LibraryStar("G5I (115nm-4.11um)",       "G5I_pickles_irtf")
  case object G5III     extends LibraryStar("G5III (300nm-6.0um)",      "g5iii")
  case object G5III_new extends LibraryStar("G5III (115nm-5.01um)",     "G5III_pickles_irtf")
  case object G5III_w   extends LibraryStar("G5III-w (300nm-6.0um)",    "g5iii-w")
  case object G5III_r   extends LibraryStar("G5III-r (300nm-6.0um)",    "g5iii-r")
  case object G5V       extends LibraryStar("G5V (300nm-6.0um)",        "g5v")
  case object G5V_new   extends LibraryStar("G5V (100nm-27um model)",   "G5V_calspec")
  case object G5V_w     extends LibraryStar("G5V-w (300nm-6.0um)",      "g5v-w")
  case object G5V_r     extends LibraryStar("G5V-r (300nm-6.0um)",      "g5v-r")
  case object G7III     extends LibraryStar("G7III (100nm-27um model)", "G7III_calspec")
  case object G8I       extends LibraryStar("G8I (115nm-4.05um)",       "G8I_pickles_irtf")
  case object G8III     extends LibraryStar("G8III (115nm-5.01um)",     "G8III_pickles_irtf")
  case object G8V       extends LibraryStar("G8V (115nm-4.93um)",       "G8V_pickles_irtf")
  // K
  case object K0III     extends LibraryStar("K0III (300nm-6.0um)",        "k0iii")
  case object K0III_new extends LibraryStar("K0III (115nm-4.12um)",       "K0III_pickles_irtf")
  case object K0III_w   extends LibraryStar("K0III-w (300nm-6.0um)",      "k0iii-w")
  case object K0III_r   extends LibraryStar("K0III-r (300nm-6.0um)",      "k0iii-r")
  case object K0IV      extends LibraryStar("K0IV (115nm-5.34um)",        "K0IV_pickles_irtf")
  case object K0V       extends LibraryStar("K0V (300nm-6.0um)",          "k0v")
  case object K0V_new   extends LibraryStar("K0V (115nm-4.99um)",         "K0V_pickles_irtf")
  case object K0V_r     extends LibraryStar("K0V-r (300nm-6.0um)",        "k0v-r")
  case object K05III    extends LibraryStar("K0.5III (100nm-27um model)", "K0.5III_calspec")
  case object K0_1II    extends LibraryStar("K0-1II (300nm-6.0um)",       "k0-1ii")
  case object K15III    extends LibraryStar("K1.5III (100nm-27um model)", "K1.5III_calspec")
  case object K2I       extends LibraryStar("K2I (115nm-5.01um)",         "K2I_pickles_irtf")
  case object K2III     extends LibraryStar("K2III (115nm-4.94um)",       "K2III_pickles_irtf")
  case object K2V       extends LibraryStar("K2V (115nm-5.0um)",          "K2V_pickles_irtf")
  case object K3II      extends LibraryStar("K3II (115nm-5.03um)",        "K3II_pickles_irtf")
  case object K3III     extends LibraryStar("K3III (115nm-4.97um)",       "K3III_pickles_irtf")
  case object K3V       extends LibraryStar("K3V (115nm-4.93um)",         "K3V_pickles_irtf")
  case object K4I       extends LibraryStar("K4I (300nm-6.0um)",          "k4i")
  case object K4I_new   extends LibraryStar("K4I (115nm-4.08um)",         "K4I_pickles_irtf")
  case object K4III     extends LibraryStar("K4III (300nm-6.0um)",        "k4iii")
  case object K4III_new extends LibraryStar("K4III (115nm-5.02um)",       "K4III_pickles_irtf")
  case object K4III_w   extends LibraryStar("K4III-w (300nm-6.0um)",      "k4iii-w")
  case object K4III_r   extends LibraryStar("K4III-r (300nm-6.0um)",      "k4iii-r")
  case object K4V       extends LibraryStar("K4V (300nm-6.0um)",          "k4v")
  case object K5III     extends LibraryStar("K5III (115nm-5.0um)",        "K5III_pickles_irtf")
  case object K5V       extends LibraryStar("K5V (115nm-5.2um)",          "K5V_pickles_irtf")
  // M
  case object M0III     extends LibraryStar("M0III (300nm-6.0um)",  "m0iii")
  case object M0III_new extends LibraryStar("M0III (115nm-5.07um)", "M0III_pickles_irtf")
  case object M0V       extends LibraryStar("M0V (300nm-6.0um)",    "m0v")
  case object M0V_new   extends LibraryStar("M0V (115nm-5.41um)",   "M0V_pickles_irtf")
  case object M1III     extends LibraryStar("M1III (115nm-5.05um)", "M1III_pickles_irtf")
  case object M1V       extends LibraryStar("M1V (115nm-4.91um)",   "M1V_pickles_irtf")
  case object M2I       extends LibraryStar("M2I (115nm-5.0um)",    "M2I_pickles_irtf")
  case object M2III     extends LibraryStar("M2III (115nm-4.95um)", "M2III_pickles_irtf")
  case object M2V       extends LibraryStar("M2V (115nm-5.34um)",   "M2V_pickles_irtf")
  case object M3III     extends LibraryStar("M3III (300nm-6.0um)",  "m3iii")
  case object M3III_new extends LibraryStar("M3III (115nm-4.91um)", "M3III_pickles_irtf")
  case object M3V       extends LibraryStar("M3V (300nm-6.0um)",    "m3v")
  case object M3V_new   extends LibraryStar("M3V (115nm-5.07um)",   "M3V_pickles_irtf")
  case object M4III     extends LibraryStar("M4III (115nm-5.02um)", "M4III_pickles_irtf")
  case object M4V       extends LibraryStar("M4V (115nm-5.08um)",   "M4V_pickles_irtf")
  case object M5V       extends LibraryStar("M5V (115nm-4.18um)",   "M5V_pickles_irtf")
  case object M6III     extends LibraryStar("M6III (300nm-6.0um)",  "m6iii")
  case object M6III_new extends LibraryStar("M6III (115nm-5.02um)", "M6III_pickles_irtf")
  case object M6V       extends LibraryStar("M6V (300nm-6.0um)",    "m6v")
  case object M7III     extends LibraryStar("M7III (115nm-5.02um)", "M7III_pickles_irtf")
  case object M8III     extends LibraryStar("M8III (115nm-5.02um)", "M8III_pickles_irtf")
  case object M9III     extends LibraryStar("M9III (300nm-6.0um)",  "m9iii")
  // sub-dwarfs
  case object sdB       extends LibraryStar("sdB (100nm-32um model)",  "sdB_calspec")
  case object sdF8      extends LibraryStar("sdF8 (171nm-2.48um)",     "sdF8_calspec")
  case object sdO       extends LibraryStar("sdO (115nm-1.02um)",      "sdO_calspec")
  // white dwarfs
  case object DA08      extends LibraryStar("DA.8 (100nm-32um model)",  "DA08_calspec")
  case object DA09      extends LibraryStar("DA.9 (100nm-30um model)",  "DA09_calspec")
  case object DA12      extends LibraryStar("DA1.2 (100nm-30um model)", "DA12_calspec")
  case object DA15      extends LibraryStar("DA1.5 (100nm-30um model)", "DA15_calspec")
  case object DA18      extends LibraryStar("DA1.8 (100nm-27um model)", "DA18_calspec")
  case object DA24      extends LibraryStar("DA2.4 (100nm-32um model)", "DA24_calspec")
  case object DA28      extends LibraryStar("DA2.8 (114nm-1.71um)",     "DA28_calspec")
  case object DA30      extends LibraryStar("DA3.0 (114nm-1.71um)",     "DA30_calspec")
  case object DA31      extends LibraryStar("DA3.1 (114nm-1.71um)",     "DA31_calspec")
  case object DA33      extends LibraryStar("DA3.3 (114nm-1.71um)",     "DA33_calspec")
  case object DA36      extends LibraryStar("DA3.6 (114nm-1.71um)",     "DA36_calspec")
  case object DA38      extends LibraryStar("DA3.8 (114nm-1.71um)",     "DA38_calspec")
  case object DA48      extends LibraryStar("DA4.8 (114nm-1.71um)",     "DA48_calspec")
  case object DA57      extends LibraryStar("DA5.7 (114nm-1.71um)",     "DA57_calspec")
  case object DBQ40     extends LibraryStar("DBQ4 (100nm-32um model)",  "DBQ40_calspec")
  case object DBQA50    extends LibraryStar("DBQA5 (100nm-32um model)", "DBQA50_calspec")
  case object DO20      extends LibraryStar("DO2 (114nm-1.02um)",       "DO20_calspec")
  // cool star models
  case object T2800K    extends LibraryStar("T=2800K (300nm-6um model)",  "t2800k")
  case object T2600K    extends LibraryStar("T=2600K (300nm-6um model)",  "t2600k")
  case object T2400K    extends LibraryStar("T=2400K (300nm-6um model)",  "t2400k")
  case object T2200K    extends LibraryStar("T=2200K (300nm-6um model)",  "t2200k")
  case object T2000K    extends LibraryStar("T=2000K (300nm-6um model)",  "t2000k")
  case object T1800K    extends LibraryStar("T=1800K (300nm-6um model)",  "t1800k")
  case object T1600K    extends LibraryStar("T=1600K (300nm-6um model)",  "t1600k")
  case object T1400K    extends LibraryStar("T=1400K (300nm-6um model)",  "t1400k")
  case object T1200K    extends LibraryStar("T=1200K (300nm-6um model)",  "t1200k")
  case object T1000K    extends LibraryStar("T=1000K (600nm-10um model)", "t1000k")
  case object T0900K    extends LibraryStar("T=900K (600nm-10um model)",  "t0900k")
  case object T0800K    extends LibraryStar("T=800K (600nm-10um model)",  "t0800k")
  case object T0600K    extends LibraryStar("T=600K (600nm-10um model)",  "t0600k")
  case object T0400K    extends LibraryStar("T=400K (600nm-10um model)",  "t0400k")

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

