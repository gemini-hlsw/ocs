package edu.gemini.model.p1

import java.util.UUID
import edu.gemini.spModel.core.{MagnitudeSystem, Magnitude, MagnitudeBand}
import edu.gemini.model.p1.{ mutable => M }

package object immutable {

  /** Email regex used for validation. */
  lazy val EmailRegex = "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]*$".r

  type Airmass = Double

  type Ref[A] = Proposal => Option[A]

  implicit class StringPimp(val s: String) extends AnyVal {

    // Trim lines, join adjacent lines if non-empty
    def unwrapLines: String = s.lines.map(_.trim).dropWhile(_.isEmpty).map {
      case "" => "\n"
      case s0 => s"$s0 "
    }.mkString

    def trimLines: String = s.lines.map(_.trim).dropWhile(_.isEmpty).mkString("\n")

  }

  // Enumerated types are pulled in directly from the generated code.
  // A macro system sure would be nice here.
  // grep -h '^public enum' src-gen/edu/gemini/model/p1/mutable/*.java | cut -d' ' -f3 | perl -ne 'chop;print qq|type $_ = M.$_\nobject $_ extends EnumObject(M.$_.values)\n\n|'

  type Band = M.Band
  object Band extends EnumObject[M.Band] {
    final val BAND_1_2 = M.Band.BAND_1_2
    final val BAND_3 = M.Band.BAND_3
  }

  type CloudCover = M.CloudCover
  object CloudCover extends EnumObject[M.CloudCover] {
    val BEST = M.CloudCover.cc50
    val CC70 = M.CloudCover.cc70
    val CC80 = M.CloudCover.cc80
    val ANY  = M.CloudCover.cc100
  }

  type CoordinatesEpoch = M.CoordinatesEpoch
  object CoordinatesEpoch extends EnumObject[M.CoordinatesEpoch] {
    val J_2000 = M.CoordinatesEpoch.J_2000
  }

  type NgoPartner = M.NgoPartner
  object NgoPartner extends EnumObject[M.NgoPartner] {
    val AR = M.NgoPartner.AR
    val BR = M.NgoPartner.BR
    val CA = M.NgoPartner.CA
    val CL = M.NgoPartner.CL
    val KR = M.NgoPartner.KR
    val US = M.NgoPartner.US
    val UH = M.NgoPartner.UH
  }

  type ExchangePartner = M.ExchangePartner
  object ExchangePartner extends EnumObject[M.ExchangePartner] {
    final val KECK   = M.ExchangePartner.KECK
    final val SUBARU = M.ExchangePartner.SUBARU
    final val CFH    = M.ExchangePartner.CFH
  }

  // Singleton used to represent a Large Program "Partner"
  object LargeProgramPartner

  // Singleton used to represent a GT "Partner"
  object GuaranteedTimePartner

  type SpecialProposalType = M.SpecialProposalType
  object SpecialProposalType extends EnumObject[M.SpecialProposalType] {
    val DEMO_SCIENCE        = M.SpecialProposalType.DEMO_SCIENCE
    val DIRECTORS_TIME      = M.SpecialProposalType.DIRECTORS_TIME
    val POOR_WEATHER        = M.SpecialProposalType.POOR_WEATHER
    val SYSTEM_VERIFICATION = M.SpecialProposalType.SYSTEM_VERIFICATION
    val GUARANTEED_TIME     = M.SpecialProposalType.GUARANTEED_TIME
  }

  type Flamingos2Filter = M.Flamingos2Filter
  object Flamingos2Filter extends EnumObject[M.Flamingos2Filter] {
    val Y       = M.Flamingos2Filter.Y
    val J_LOW   = M.Flamingos2Filter.J_LOW
    val J       = M.Flamingos2Filter.J
    val H       = M.Flamingos2Filter.H
    val K_LONG  = M.Flamingos2Filter.K_LONG
    val K_SHORT = M.Flamingos2Filter.K_SHORT
    val K_BLUE  = M.Flamingos2Filter.K_BLUE
    val K_RED   = M.Flamingos2Filter.K_RED
    val JH      = M.Flamingos2Filter.JH
    val HK      = M.Flamingos2Filter.HK
  }

  type Flamingos2Disperser = M.Flamingos2Disperser
  object Flamingos2Disperser extends EnumObject[M.Flamingos2Disperser] {
    val R1200JH = M.Flamingos2Disperser.R1200JH
    val R1200HK = M.Flamingos2Disperser.R1200HK
    val R3000   = M.Flamingos2Disperser.R3000
  }

  type Flamingos2Fpu = M.Flamingos2Fpu
  object Flamingos2Fpu extends EnumObject[M.Flamingos2Fpu] {
    val LONGSLIT_4 = M.Flamingos2Fpu.LONGSLIT_4
  }

  type GmosNDisperser = M.GmosNDisperser
  object GmosNDisperser extends EnumObject[M.GmosNDisperser] {
    val B1200 = M.GmosNDisperser.B1200_G5301
    val R831  = M.GmosNDisperser.R831_G5302
    val B480  = M.GmosNDisperser.B480_G5309
    val B600  = M.GmosNDisperser.B600_G5307
    val R600  = M.GmosNDisperser.R600_G5304
    val R400  = M.GmosNDisperser.R400_G5305
    val R150  = M.GmosNDisperser.R150_G5308
  }

  type GmosNFilter = M.GmosNFilter
  object GmosNFilter extends EnumObject[M.GmosNFilter] {
    import M.GmosNFilter._
    val None = NONE
    val IMAGING = List(
      g_G0301,
      r_G0303,
      i_G0302,
      CaT_G0309,
      z_G0304,
      Z_G0322,
      Y_G0323,
      ri_G0349,
      HeII_G0320,
      HeIIC_G0321,
      OIII_G0318,
      OIIIC_G0319,
      Ha_G0310,
      HaC_G0311,
      SII_G0317,
      OVI_G0345,
      OVIC_G0346,
      DS920_G0312,
      USER_SUPPLIED
    )
    val ALTAIR_IMAGING = List(
      CaT_G0309,
      z_G0304,
      Z_G0322,
      Y_G0323,
      DS920_G0312,
      USER_SUPPLIED
    )
    val ALTAIR_SPECTROSCOPY = List(
      NONE,
      CaT_G0309,
      z_G0304,
      Z_G0322,
      Y_G0323,
      DS920_G0312,
      USER_SUPPLIED
    )
  }

  type GmosNFpu = M.GmosNFpu
  object GmosNFpu extends EnumObject[M.GmosNFpu]

  type GmosNMOSFpu = M.GmosNMOSFpu
  object GmosNMOSFpu extends EnumObject[M.GmosNMOSFpu]

  type GmosNFpuIfu = M.GmosNFpuIfu
  object GmosNFpuIfu extends EnumObject[M.GmosNFpuIfu]

  type GmosNFpuNs = M.GmosNFpuNs
  object GmosNFpuNs extends EnumObject[M.GmosNFpuNs]

  type GmosNWavelengthRegime = M.GmosNWavelengthRegime
  object GmosNWavelengthRegime extends EnumObject[M.GmosNWavelengthRegime]

  type GmosSDisperser = M.GmosSDisperser
  object GmosSDisperser extends EnumObject[M.GmosSDisperser] {
    val B1200 = M.GmosSDisperser.B1200_G5321
    val R831  = M.GmosSDisperser.R831_G5322
    val B600  = M.GmosSDisperser.B600_G5323
    val R600  = M.GmosSDisperser.R600_G5324
    val R400  = M.GmosSDisperser.R400_G5325
    val R150  = M.GmosSDisperser.R150_G5326
  }

  type GmosSFilter = M.GmosSFilter
  object GmosSFilter extends EnumObject[M.GmosSFilter] {
    import M.GmosSFilter._
    val None = NONE
    val IMAGING = List(
      u_G0332,
      g_G0325,
      r_G0326,
      i_G0327,
      CaT_G0333,
      z_G0328,
      Z_G0343,
      Y_G0344,
      Lya395_G0342,
      HeII_G0340,
      HeIIC_G0341,
      OIII_G0338,
      OIIIC_G0339,
      Ha_G0336,
      HaC_G0337,
      SII_G0335,
      OVI_G0347,
      OVIC_G0348,
      USER_SUPPLIED
    )
  }

  type GmosSFpu = M.GmosSFpu
  object GmosSFpu extends EnumObject[M.GmosSFpu]

  type GmosSMOSFpu = M.GmosSMOSFpu
  object GmosSMOSFpu extends EnumObject[M.GmosSMOSFpu]

  type GmosSFpuIfu = M.GmosSFpuIfu
  object GmosSFpuIfu extends EnumObject[M.GmosSFpuIfu]

  type GmosSFpuIfuNs = M.GmosSFpuIfuNs
  object GmosSFpuIfuNs extends EnumObject[M.GmosSFpuIfuNs]

  type GmosSFpuNs = M.GmosSFpuNs
  object GmosSFpuNs extends EnumObject[M.GmosSFpuNs]

  type GmosSWavelengthRegime = M.GmosSWavelengthRegime
  object GmosSWavelengthRegime extends EnumObject[M.GmosSWavelengthRegime]

  type GnirsCrossDisperser = M.GnirsCrossDisperser
  object GnirsCrossDisperser extends EnumObject[M.GnirsCrossDisperser] {
    val No  = M.GnirsCrossDisperser.NO
    val LXD = M.GnirsCrossDisperser.LXD
  }

  type GnirsDisperser = M.GnirsDisperser
  object GnirsDisperser extends EnumObject[M.GnirsDisperser] {
    val D_10 = M.GnirsDisperser.D_10
  }

  type GnirsFilter = M.GnirsFilter
  object GnirsFilter extends EnumObject[M.GnirsFilter]

  type GnirsFpu = M.GnirsFpu
  object GnirsFpu extends EnumObject[M.GnirsFpu] {
    val SW1    = M.GnirsFpu.SW_1
    val SW2    = M.GnirsFpu.SW_2
    val SW3    = M.GnirsFpu.SW_3
    val SW4    = M.GnirsFpu.SW_4
    val SW5    = M.GnirsFpu.SW_5
    val SW6    = M.GnirsFpu.SW_6
    val SW7    = M.GnirsFpu.SW_7
    val LR_IFU = M.GnirsFpu.LR_IFU
    val HR_IFU = M.GnirsFpu.HR_IFU
  }

  type GnirsPixelScale = M.GnirsPixelScale
  object GnirsPixelScale extends EnumObject[M.GnirsPixelScale] {
    val PS_005 = M.GnirsPixelScale.PS_005
    val PS_015 = M.GnirsPixelScale.PS_015
  }

  type GnirsCentralWavelength = M.GnirsCentralWavelength
  object GnirsCentralWavelength extends EnumObject[M.GnirsCentralWavelength] {
    val LT_25  = M.GnirsCentralWavelength.LT_25
    val GTE_25 = M.GnirsCentralWavelength.GTE_25
  }

  type GracesFiberMode = M.GracesFiberMode
  object GracesFiberMode extends EnumObject[M.GracesFiberMode]

  type GracesReadMode = M.GracesReadMode
  object GracesReadMode extends EnumObject[M.GracesReadMode]

  type GpiObservingMode = M.GpiObservingMode
  object GpiObservingMode extends EnumObject[M.GpiObservingMode] {
    val HStar   = M.GpiObservingMode.H_STAR
    val HLiwa   = M.GpiObservingMode.H_LIWA
    val HDirect = M.GpiObservingMode.DIRECT_H_BAND

    def isCoronographMode(mode: GpiObservingMode):Boolean = mode.value.startsWith("Coronograph")
    def isDirectMode(mode: GpiObservingMode):Boolean = mode.value.endsWith("direct")
    def scienceBand(mode: GpiObservingMode):Option[String] = {
      val CoronographMode = """Coronograph (\w+)-band""".r
      val DirectMode = """(\w+) direct""".r
      val UnblockedMode = """([A-Z1-2]+) unblocked""".r
      val NRMMode = """Non Redundant Mask (\w+)""".r
      val LiwaStarMode = """H_(.*)""".r
      // This is quite weak as it is based on the naming conventions
      mode.value match {
        case CoronographMode(band) => Some(band)
        case DirectMode(band)      => Some(band)
        case UnblockedMode(band)   => Some(band)
        case NRMMode(band)         => Some(band)
        case LiwaStarMode(_)       => Some("H")
        case _                     => None
      }
    }
  }

  type GpiDisperser = M.GpiDisperser
  object GpiDisperser extends EnumObject[M.GpiDisperser]

  type GhostResolutionMode = M.GhostResolutionMode
  object GhostResolutionMode extends EnumObject[M.GhostResolutionMode] {
    val Standard                = M.GhostResolutionMode.STANDARD
    val High                    = M.GhostResolutionMode.HIGH
    val PrecisionRadialVelocity = M.GhostResolutionMode.PRECISION_RADIAL_VELOCITY
  }

  type GhostTargetMode = M.GhostTargetMode
  object GhostTargetMode extends EnumObject[M.GhostTargetMode] {
    val Single       = M.GhostTargetMode.SINGLE
    val Dual         = M.GhostTargetMode.DUAL
    val TargetAndSky = M.GhostTargetMode.TARGET_AND_SKY
  }

  type GuidingEvaluation = M.GuidingEvaluation
  object GuidingEvaluation extends EnumObject[M.GuidingEvaluation] {
    final val SUCCESS = M.GuidingEvaluation.SUCCESS
    final val CAUTION = M.GuidingEvaluation.CAUTION
    final val WARNING = M.GuidingEvaluation.WARNING
    final val FAILURE = M.GuidingEvaluation.FAILURE
  }

  type KeckInstrument = M.KeckInstrument
  object KeckInstrument extends EnumObject[M.KeckInstrument]

  type MichelleFilter = M.MichelleFilter
  object MichelleFilter extends EnumObject[M.MichelleFilter]

  type MichellePolarimetry = M.MichellePolarimetry
  object MichellePolarimetry extends EnumObject[M.MichellePolarimetry] {
    val YES = M.MichellePolarimetry.YES
    val NO = M.MichellePolarimetry.NO
  }

  type MichelleFpu = M.MichelleFpu
  object MichelleFpu extends EnumObject[M.MichelleFpu]

  type MichelleDisperser = M.MichelleDisperser
  object MichelleDisperser extends EnumObject[M.MichelleDisperser]

  type NiciDichroic = M.NiciDichroic
  object NiciDichroic extends EnumObject[M.NiciDichroic] {
    val MIRROR = M.NiciDichroic.MIRROR
    val OPEN = M.NiciDichroic.OPEN
  }

  type NiciBlueFilter = M.NiciBlueFilter
  object NiciBlueFilter extends EnumObject[M.NiciBlueFilter]

  type NiciFpm = M.NiciFpm
  object NiciFpm extends EnumObject[M.NiciFpm]

  type NiciRedFilter = M.NiciRedFilter
  object NiciRedFilter extends EnumObject[M.NiciRedFilter]

  type NifsDisperer = M.NifsDisperser
  object NifsDisperser extends EnumObject[M.NifsDisperser] {
    val Z = M.NifsDisperser.Z
  }

  type NifsOccultingDisk = M.NifsOccultingDisk
  object NifsOccultingDisk extends EnumObject[M.NifsOccultingDisk] {
    val OD_NONE = M.NifsOccultingDisk.CLEAR
  }

  type NiriCamera = M.NiriCamera
  object NiriCamera extends EnumObject[M.NiriCamera] {
    val F6  = M.NiriCamera.F6
    val F14 = M.NiriCamera.F14
    val F32 = M.NiriCamera.F32
  }

  type NiriFilter = M.NiriFilter
  object NiriFilter extends EnumObject[M.NiriFilter] {
    val BBF_LPRIME  = M.NiriFilter.BBF_LPRIME
    val BBF_MPRIME  = M.NiriFilter.BBF_MPRIME
    val BBF_BRACONT = M.NiriFilter.NBF_BRACONT
  }

  type PhoenixFocalPlaneUnit = M.PhoenixFocalPlaneUnit
  object PhoenixFocalPlaneUnit extends EnumObject[M.PhoenixFocalPlaneUnit]

  type PhoenixFilter = M.PhoenixFilter
  object PhoenixFilter extends EnumObject[M.PhoenixFilter]

  type GsaoiFilter = M.GsaoiFilter
  object GsaoiFilter extends EnumObject[M.GsaoiFilter]

  type Guider = M.Guider
  object Guider extends EnumObject[M.Guider]

  type ImageQuality = M.ImageQuality
  object ImageQuality extends EnumObject[M.ImageQuality] {
    val BEST = M.ImageQuality.iq20
    val IQ70 = M.ImageQuality.iq70
    val IQ85 = M.ImageQuality.iq85
    val IQANY = M.ImageQuality.iq100
  }

  type InvestigatorStatus = M.InvestigatorStatus
  object InvestigatorStatus extends EnumObject[M.InvestigatorStatus] {
    val PH_D  = M.InvestigatorStatus.PH_D
    val OTHER = M.InvestigatorStatus.OTHER
  }

  type InvestigatorGender = M.InvestigatorGender
  object InvestigatorGender extends EnumObject[M.InvestigatorGender] {
    val NONE_SELECTED     = M.InvestigatorGender.NONE_SELECTED
    val WOMAN             = M.InvestigatorGender.WOMAN
    val MAN               = M.InvestigatorGender.MAN
    val ANOTHER_GENDER    = M.InvestigatorGender.ANOTHER_GENDER
    val PREFER_NOT_TO_SAY = M.InvestigatorGender.PREFER_NOT_TO_SAY
  }

  // Bands that are on the phase1 model
  val allowedBands = List(
    MagnitudeBand._u,
    MagnitudeBand._g,
    MagnitudeBand._r,
    MagnitudeBand._i,
    MagnitudeBand._z,
    MagnitudeBand.U,
    MagnitudeBand.B,
    MagnitudeBand.V,
    MagnitudeBand.UC,
    MagnitudeBand.R,
    MagnitudeBand.I,
    MagnitudeBand.Y,
    MagnitudeBand.J,
    MagnitudeBand.H,
    MagnitudeBand.K,
    MagnitudeBand.L,
    MagnitudeBand.M,
    MagnitudeBand.N,
    MagnitudeBand.Q)

  implicit class MagnitudeBandOps(val band: MagnitudeBand) extends AnyVal {
    def mutable: M.MagnitudeBand = band match {
      case MagnitudeBand._u => M.MagnitudeBand._u
      case MagnitudeBand._g => M.MagnitudeBand._g
      case MagnitudeBand._r => M.MagnitudeBand._r
      case MagnitudeBand._i => M.MagnitudeBand._i
      case MagnitudeBand._z => M.MagnitudeBand._z
      case MagnitudeBand.U  => M.MagnitudeBand.U
      case MagnitudeBand.B  => M.MagnitudeBand.B
      case MagnitudeBand.V  => M.MagnitudeBand.V
      case MagnitudeBand.UC => M.MagnitudeBand.UC
      case MagnitudeBand.R  => M.MagnitudeBand.R
      case MagnitudeBand.I  => M.MagnitudeBand.I
      case MagnitudeBand.Y  => M.MagnitudeBand.Y
      case MagnitudeBand.J  => M.MagnitudeBand.J
      case MagnitudeBand.H  => M.MagnitudeBand.H
      case MagnitudeBand.K  => M.MagnitudeBand.K
      case MagnitudeBand.L  => M.MagnitudeBand.L
      case MagnitudeBand.M  => M.MagnitudeBand.M
      case MagnitudeBand.N  => M.MagnitudeBand.N
      case MagnitudeBand.Q  => M.MagnitudeBand.Q
      case _                => sys.error("Should not happen")
    }
  }

  implicit class MagnitudeSystemOps(val system: MagnitudeSystem) extends AnyVal {
    def mutable: M.MagnitudeSystem = system match {
      case MagnitudeSystem.AB   => M.MagnitudeSystem.AB
      case MagnitudeSystem.Jy   => M.MagnitudeSystem.JY
      case MagnitudeSystem.Vega => M.MagnitudeSystem.VEGA
      case _                    => sys.error("Should not happen")
    }
  }

  implicit class MutableMagnitudeBandOps(val band: M.MagnitudeBand) extends AnyVal {
    def toBand: MagnitudeBand = band match {
      case M.MagnitudeBand._u => MagnitudeBand._u
      case M.MagnitudeBand._g => MagnitudeBand._g
      case M.MagnitudeBand._r => MagnitudeBand._r
      case M.MagnitudeBand._i => MagnitudeBand._i
      case M.MagnitudeBand._z => MagnitudeBand._z
      case M.MagnitudeBand.U  => MagnitudeBand.U
      case M.MagnitudeBand.B  => MagnitudeBand.B
      case M.MagnitudeBand.V  => MagnitudeBand.V
      case M.MagnitudeBand.UC => MagnitudeBand.UC
      case M.MagnitudeBand.R  => MagnitudeBand.R
      case M.MagnitudeBand.I  => MagnitudeBand.I
      case M.MagnitudeBand.Y  => MagnitudeBand.Y
      case M.MagnitudeBand.J  => MagnitudeBand.J
      case M.MagnitudeBand.H  => MagnitudeBand.H
      case M.MagnitudeBand.K  => MagnitudeBand.K
      case M.MagnitudeBand.L  => MagnitudeBand.L
      case M.MagnitudeBand.M  => MagnitudeBand.M
      case M.MagnitudeBand.N  => MagnitudeBand.N
      case M.MagnitudeBand.Q  => MagnitudeBand.Q
    }
  }

  implicit class MagnitudeOps(val magnitude: Magnitude) extends AnyVal {
    def mutable: M.Magnitude = {
      val m = Factory.createMagnitude
          m.setValue(BigDecimal(magnitude.value).bigDecimal)
          m.setBand(magnitude.band.mutable)
          m.setSystem(magnitude.system.mutable)
          m
    }
  }

  type SemesterOption = M.SemesterOption
  object SemesterOption extends EnumObject[M.SemesterOption] {
    val A = M.SemesterOption.A
    val B = M.SemesterOption.B
  }

  type SubaruInstrument = M.SubaruInstrument
  object SubaruInstrument extends EnumObject[M.SubaruInstrument] {
    val SUPRIME_CAM = M.SubaruInstrument.SUPRIME_CAM
    val COMICS = M.SubaruInstrument.COMICS
    val FMOS   = M.SubaruInstrument.FMOS
    val IRCS   = M.SubaruInstrument.IRCS
    val MOIRCS = M.SubaruInstrument.MOIRCS
  }

  type SkyBackground = M.SkyBackground
  object SkyBackground extends EnumObject[M.SkyBackground] {
    val BEST = M.SkyBackground.sb20
    val ANY  = M.SkyBackground.sb100
  }

  type TacCategory = M.TacCategory

  final case class TacCategoryGroup(title: String)

  object TacCategoryGroup extends EnumObject[M.TacCategory] {
    val SOLAR_SYSTEM = new TacCategoryGroup("SOLAR SYSTEM")
    val EXOPLANETS = new TacCategoryGroup("EXOPLANETS")
    val GALACTIC = new TacCategoryGroup("GALACTIC/LOCAL GROUP")
    val EXTRA_GALACTIC = new TacCategoryGroup("EXTRAGALACTIC")

    val all = List(SOLAR_SYSTEM, EXOPLANETS, GALACTIC, EXTRA_GALACTIC)
  }

  object TacCategory extends EnumObject[M.TacCategory] {
    val SMALL_BODIES_ASTEROIDS_COMETS_MOONS_KUIPER_BELT            = M.TacCategory.SMALL_BODIES_ASTEROIDS_COMETS_MOONS_KUIPER_BELT
    val PLANETARY_ATMOSPHERES                                      = M.TacCategory.PLANETARY_ATMOSPHERES
    val PLANETARY_SURFACES                                         = M.TacCategory.PLANETARY_SURFACES
    val SOLAR_SYSTEM_OTHER                                         = M.TacCategory.SOLAR_SYSTEM_OTHER
    val EXOPLANET_RADIAL_VELOCITIES                                = M.TacCategory.EXOPLANET_RADIAL_VELOCITIES
    val EXOPLANET_ATMOSPHERES_ACTIVITY                             = M.TacCategory.EXOPLANET_ATMOSPHERES_ACTIVITY
    val EXOPLANET_TRANSITS_ROSSITER_MC_LAUGHLIN                    = M.TacCategory.EXOPLANET_TRANSITS_ROSSITER_MC_LAUGHLIN
    val EXOPLANET_HOST_STAR_PROPERTIES_CONNECTIONS                 = M.TacCategory.EXOPLANET_HOST_STAR_PROPERTIES_CONNECTIONS
    val EXOPLANET_OTHER                                            = M.TacCategory.EXOPLANET_OTHER
    val STELLAR_ASTROPHYSICS_EVOLUTION_SUPERNOVAE_ABUNDANCES       = M.TacCategory.STELLAR_ASTROPHYSICS_EVOLUTION_SUPERNOVAE_ABUNDANCES
    val STELLAR_POPULATIONS_CLUSTERS_CHEMICAL_EVOLUTION            = M.TacCategory.STELLAR_POPULATIONS_CLUSTERS_CHEMICAL_EVOLUTION
    val STAR_FORMATION                                             = M.TacCategory.STAR_FORMATION
    val GASEOUS_ASTROPHYSICS_H_II_REGIONS_PN_ISM_SN_REMNANTS_NOVAE = M.TacCategory.GASEOUS_ASTROPHYSICS_H_II_REGIONS_PN_ISM_SN_REMNANTS_NOVAE
    val STELLAR_REMNANTS_COMPACT_OBJECTS_WD_NS_BH                  = M.TacCategory.STELLAR_REMNANTS_COMPACT_OBJECTS_WD_NS_BH
    val GALACTIC_OTHER                                             = M.TacCategory.GALACTIC_OTHER
    val COSMOLOGY_FUNDAMENTAL_PHYSICS_LARGE_SCALE_STRUCTURE        = M.TacCategory.COSMOLOGY_FUNDAMENTAL_PHYSICS_LARGE_SCALE_STRUCTURE
    val CLUSTERS_GROUPS_OF_GALAXIES                                = M.TacCategory.CLUSTERS_GROUPS_OF_GALAXIES
    val HIGH_Z_UNIVERSE                                            = M.TacCategory.HIGH_Z_UNIVERSE
    val LOW_Z_UNIVERSE                                             = M.TacCategory.LOW_Z_UNIVERSE
    val ACTIVE_GALAXIES_QUASARS_SMBH                               = M.TacCategory.ACTIVE_GALAXIES_QUASARS_SMBH
    val EXTRAGALACTIC_OTHER                                        = M.TacCategory.EXTRAGALACTIC_OTHER

    val groups: List[(TacCategoryGroup, TacCategory)] = List(
      TacCategoryGroup.SOLAR_SYSTEM   -> SMALL_BODIES_ASTEROIDS_COMETS_MOONS_KUIPER_BELT,
      TacCategoryGroup.SOLAR_SYSTEM   -> PLANETARY_ATMOSPHERES,
      TacCategoryGroup.SOLAR_SYSTEM   -> PLANETARY_SURFACES,
      TacCategoryGroup.SOLAR_SYSTEM   -> SOLAR_SYSTEM_OTHER,
      TacCategoryGroup.EXOPLANETS     -> EXOPLANET_RADIAL_VELOCITIES,
      TacCategoryGroup.EXOPLANETS     -> EXOPLANET_ATMOSPHERES_ACTIVITY,
      TacCategoryGroup.EXOPLANETS     -> EXOPLANET_TRANSITS_ROSSITER_MC_LAUGHLIN,
      TacCategoryGroup.EXOPLANETS     -> EXOPLANET_HOST_STAR_PROPERTIES_CONNECTIONS,
      TacCategoryGroup.EXOPLANETS     -> EXOPLANET_OTHER,
      TacCategoryGroup.GALACTIC       -> STELLAR_ASTROPHYSICS_EVOLUTION_SUPERNOVAE_ABUNDANCES,
      TacCategoryGroup.GALACTIC       -> STELLAR_POPULATIONS_CLUSTERS_CHEMICAL_EVOLUTION,
      TacCategoryGroup.GALACTIC       -> STAR_FORMATION,
      TacCategoryGroup.GALACTIC       -> GASEOUS_ASTROPHYSICS_H_II_REGIONS_PN_ISM_SN_REMNANTS_NOVAE,
      TacCategoryGroup.GALACTIC       -> STELLAR_REMNANTS_COMPACT_OBJECTS_WD_NS_BH,
      TacCategoryGroup.GALACTIC       -> GALACTIC_OTHER,
      TacCategoryGroup.EXTRA_GALACTIC -> COSMOLOGY_FUNDAMENTAL_PHYSICS_LARGE_SCALE_STRUCTURE,
      TacCategoryGroup.EXTRA_GALACTIC -> CLUSTERS_GROUPS_OF_GALAXIES,
      TacCategoryGroup.EXTRA_GALACTIC -> HIGH_Z_UNIVERSE,
      TacCategoryGroup.EXTRA_GALACTIC -> LOW_Z_UNIVERSE,
      TacCategoryGroup.EXTRA_GALACTIC -> ACTIVE_GALAXIES_QUASARS_SMBH,
      TacCategoryGroup.EXTRA_GALACTIC -> EXTRAGALACTIC_OTHER
    )

    val items: List[Either[TacCategoryGroup, TacCategory]] = TacCategoryGroup.all.flatMap { g =>
      List(Left(g)) ++ groups.filter(_._1 == g).map(x => Right(x._2))
    }
  }

  type TimeUnit = M.TimeUnit
  object TimeUnit extends EnumObject[M.TimeUnit] {
    val HR = M.TimeUnit.HR
    val NIGHT = M.TimeUnit.NIGHT
  }

  type ToOChoice = M.TooOption
  object ToOChoice extends EnumObject[M.TooOption] {
    val None  = M.TooOption.NONE
    val Rapid = M.TooOption.RAPID
  }

  type TexesDisperser = M.TexesDisperser
  object TexesDisperser extends EnumObject[M.TexesDisperser]

  type TrecsDisperser = M.TrecsDisperser
  object TrecsDisperser extends EnumObject[M.TrecsDisperser]

  type TrecsFilter = M.TrecsFilter
  object TrecsFilter extends EnumObject[M.TrecsFilter]

  type TrecsFpu = M.TrecsFpu
  object TrecsFpu extends EnumObject[M.TrecsFpu]

  type WaterVapor = M.WaterVapor
  object WaterVapor extends EnumObject[M.WaterVapor] {
    val BEST = M.WaterVapor.wv20
    val ANY  = M.WaterVapor.wv100
  }

  type WavelengthRegime = M.WavelengthRegime
  object WavelengthRegime extends EnumObject[M.WavelengthRegime]

  type AlopekeMode = M.AlopekeMode
  object AlopekeMode extends EnumObject[M.AlopekeMode] {
    val SPECKLE    = M.AlopekeMode.SPECKLE_0_0096_PIX_6_7_FO_V
    val WIDE_FIELD = M.AlopekeMode.WIDE_FIELD_0_0725_PIX_60_FO_V
  }

  type ZorroMode = M.ZorroMode
  object ZorroMode extends EnumObject[M.ZorroMode] {
    val SPECKLE    = M.ZorroMode.SPECKLE_0_0096_PIX_6_7_FO_V
    val WIDE_FIELD = M.ZorroMode.WIDE_FIELD_0_0725_PIX_60_FO_V
  }

  type Igrins2NoddingOption = M.Igrins2NoddingOption
  object Igrins2NoddingOption extends EnumObject[M.Igrins2NoddingOption] {
    val NodAlongTheSlit = M.Igrins2NoddingOption.NOD_ALONG_THE_SLIT
    val NodOffToSky     = M.Igrins2NoddingOption.NOD_OFF_TO_SKY
  }
}


package immutable {

  // We need this so we get the same UUID for any given mutable object
  trait UuidCache[T <: { def getId():String }] {
    var cache:Map[T, UUID] = Map.empty
    def uuid(t:T) = synchronized {
      import scala.language.reflectiveCalls

      cache.get(t) match {
        case Some(uuid) => uuid
        case None =>
          val uuid = UUID.randomUUID
          // println(s"*** Mapping mutable ${t.getId()} to $uuid")
          cache = cache + (t -> uuid)
          uuid
      }
    }
  }

  abstract class EnumObject[A <: java.lang.Enum[A] : Manifest] {
    private lazy val c:Class[A] = implicitly[Manifest[A]].runtimeClass.asInstanceOf[Class[A]]
    def values:Array[A] = c.getEnumConstants
    def forName(s:String):A = Enum.valueOf(c, s)
  }

}

