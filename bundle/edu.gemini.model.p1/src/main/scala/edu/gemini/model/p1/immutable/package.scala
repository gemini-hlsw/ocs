package edu.gemini.model.p1

import java.util.UUID
import edu.gemini.spModel.core.{MagnitudeSystem, Magnitude, MagnitudeBand}
import edu.gemini.model.p1.{ mutable => M }

package object immutable {

  /** Email regex used for validation. */
  lazy val EmailRegex = "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$".r

  type Airmass = Double

  type Ref[A] = Proposal => Option[A]

  implicit class StringPimp(val s: String) extends AnyVal {

    // Trim lines, join adjacent lines if non-empty
    def unwrapLines: String = s.lines.map(_.trim).dropWhile(_.isEmpty).map {
      case s0 if s0.isEmpty => "\n"
      case s0 => s0 + " "
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
  }

  type CoordinatesEpoch = M.CoordinatesEpoch
  object CoordinatesEpoch extends EnumObject[M.CoordinatesEpoch] {
    val J_2000 = M.CoordinatesEpoch.J_2000
  }

  type NgoPartner = M.NgoPartner
  object NgoPartner extends EnumObject[M.NgoPartner] {
    val AR = M.NgoPartner.AR
    val AU = M.NgoPartner.AU
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
    final val CFHT   = M.ExchangePartner.CFHT
  }

  // Singleton used to represent a Large Program "Partner"
  object LargeProgramPartner

  type SpecialProposalType = M.SpecialProposalType
  object SpecialProposalType extends EnumObject[M.SpecialProposalType] {
    val DEMO_SCIENCE        = M.SpecialProposalType.DEMO_SCIENCE
    val DIRECTORS_TIME      = M.SpecialProposalType.DIRECTORS_TIME
    val POOR_WEATHER        = M.SpecialProposalType.POOR_WEATHER
    val SYSTEM_VERIFICATION = M.SpecialProposalType.SYSTEM_VERIFICATION
  }

  type Flamingos2Filter = mutable.Flamingos2Filter
  object Flamingos2Filter extends EnumObject[mutable.Flamingos2Filter] {
    val Y       = mutable.Flamingos2Filter.Y
    val J_LOW   = mutable.Flamingos2Filter.J_LOW
    val J       = mutable.Flamingos2Filter.J
    val H       = mutable.Flamingos2Filter.H
    val K_LONG  = mutable.Flamingos2Filter.K_LONG
    val K_SHORT = mutable.Flamingos2Filter.K_SHORT
    val JH      = mutable.Flamingos2Filter.JH
    val HK      = mutable.Flamingos2Filter.HK
  }

  type Flamingos2Disperser = M.Flamingos2Disperser
  object Flamingos2Disperser extends EnumObject[M.Flamingos2Disperser] {
    val R1200JH = M.Flamingos2Disperser.R1200JH
    val R1200HK = M.Flamingos2Disperser.R1200HK
    val R3000   = M.Flamingos2Disperser.R3000
  }

  type Flamingos2Fpu = M.Flamingos2Fpu
  object Flamingos2Fpu extends EnumObject[M.Flamingos2Fpu]

  type GmosNDisperser = M.GmosNDisperser
  object GmosNDisperser extends EnumObject[M.GmosNDisperser]

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
      HeII_G0320,
      HeIIC_G0321,
      OIII_G0318,
      OIIIC_G0319,
      Ha_G0310,
      HaC_G0311,
      SII_G0317,
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
  object GmosSDisperser extends EnumObject[M.GmosSDisperser]

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
    val LXD = M.GnirsCrossDisperser.LXD
  }

  type GnirsDisperser = M.GnirsDisperser
  object GnirsDisperser extends EnumObject[M.GnirsDisperser] {
    val D_10 = M.GnirsDisperser.D_10
  }

  type GnirsFilter = M.GnirsFilter
  object GnirsFilter extends EnumObject[M.GnirsFilter]

  type GnirsFpu = M.GnirsFpu
  object GnirsFpu extends EnumObject[M.GnirsFpu]

  type GnirsPixelScale = M.GnirsPixelScale
  object GnirsPixelScale extends EnumObject[M.GnirsPixelScale] {
    val PS_005 = M.GnirsPixelScale.PS_005
    val PS_015 = M.GnirsPixelScale.PS_015
  }

  type GnirsCentralWavelength = M.GnirsCentralWavelength
  object GnirsCentralWavelength extends EnumObject[M.GnirsCentralWavelength]

  type GracesFiberMode = M.GracesFiberMode
  object GracesFiberMode extends EnumObject[M.GracesFiberMode]

  type GracesReadMode = M.GracesReadMode
  object GracesReadMode extends EnumObject[M.GracesReadMode]

  type GpiObservingMode = M.GpiObservingMode
  object GpiObservingMode extends EnumObject[M.GpiObservingMode] {
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

  type PhoenixFocalPlaneUnit = mutable.PhoenixFocalPlaneUnit
  object PhoenixFocalPlaneUnit extends EnumObject[mutable.PhoenixFocalPlaneUnit]

  type PhoenixFilter = mutable.PhoenixFilter
  object PhoenixFilter extends EnumObject[mutable.PhoenixFilter]

  type GsaoiFilter = mutable.GsaoiFilter
  object GsaoiFilter extends EnumObject[mutable.GsaoiFilter]

  type Guider = M.Guider
  object Guider extends EnumObject[M.Guider]

  type ImageQuality = M.ImageQuality
  object ImageQuality extends EnumObject[M.ImageQuality] {
    val BEST = M.ImageQuality.iq20
  }

  type InvestigatorStatus = M.InvestigatorStatus
  object InvestigatorStatus extends EnumObject[M.InvestigatorStatus] {
    val PH_D  = M.InvestigatorStatus.PH_D
    val OTHER = M.InvestigatorStatus.OTHER
  }

  type Keyword = M.Keyword
  object Keyword extends EnumObject[M.Keyword]

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
  object SubaruInstrument extends EnumObject[M.SubaruInstrument]

  type SkyBackground = M.SkyBackground
  object SkyBackground extends EnumObject[M.SkyBackground] {
    val BEST = M.SkyBackground.sb20
  }

  type TacCategory = M.TacCategory
  object TacCategory extends EnumObject[M.TacCategory] {
    val GALACTIC = M.TacCategory.GALACTIC
  }

  type TimeUnit = M.TimeUnit
  object TimeUnit extends EnumObject[M.TimeUnit] {
    val HR = M.TimeUnit.HR
    val NIGHT = M.TimeUnit.NIGHT
  }

  type ToOChoice = mutable.TooOption
  object ToOChoice extends EnumObject[M.TooOption] {
    val None = mutable.TooOption.NONE
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
  }

  type WavelengthRegime = M.WavelengthRegime
  object WavelengthRegime extends EnumObject[M.WavelengthRegime]
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

