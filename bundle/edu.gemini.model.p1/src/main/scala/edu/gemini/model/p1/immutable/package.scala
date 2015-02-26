package edu.gemini.model.p1

import java.util.UUID
import scalaz.Monoid

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
  // grep -h '^public enum' src-gen/edu/gemini/model/p1/mutable/*.java | cut -d' ' -f3 | perl -ne 'chop;print qq|type $_ = mutable.$_\nobject $_ extends EnumObject(mutable.$_.values)\n\n|'

  type Band = mutable.Band
  object Band extends EnumObject[mutable.Band] {
    final val BAND_1_2 = mutable.Band.BAND_1_2
    final val BAND_3 = mutable.Band.BAND_3
  }

  type CloudCover = mutable.CloudCover
  object CloudCover extends EnumObject[mutable.CloudCover] {
    val BEST = mutable.CloudCover.cc50
  }

  type CoordinatesEpoch = mutable.CoordinatesEpoch
  object CoordinatesEpoch extends EnumObject[mutable.CoordinatesEpoch] {
    val J_2000 = mutable.CoordinatesEpoch.J_2000
  }

  type NgoPartner = mutable.NgoPartner
  object NgoPartner extends EnumObject[mutable.NgoPartner] {
    val AR = mutable.NgoPartner.AR
    val AU = mutable.NgoPartner.AU
    val BR = mutable.NgoPartner.BR
    val CA = mutable.NgoPartner.CA
    val CL = mutable.NgoPartner.CL
    val KR = mutable.NgoPartner.KR
    val US = mutable.NgoPartner.US
    val UH = mutable.NgoPartner.UH
  }

  type ExchangePartner = mutable.ExchangePartner
  object ExchangePartner extends EnumObject[mutable.ExchangePartner] {
    final val KECK   = mutable.ExchangePartner.KECK
    final val SUBARU = mutable.ExchangePartner.SUBARU
    final val CFHT   = mutable.ExchangePartner.CFHT
  }

  // Singleton used to represent a Large Program "Partner"
  object LargeProgramPartner

  type SpecialProposalType = mutable.SpecialProposalType
  object SpecialProposalType extends EnumObject[mutable.SpecialProposalType] {
    val DEMO_SCIENCE        = mutable.SpecialProposalType.DEMO_SCIENCE
    val DIRECTORS_TIME      = mutable.SpecialProposalType.DIRECTORS_TIME
    val POOR_WEATHER        = mutable.SpecialProposalType.POOR_WEATHER
    val SYSTEM_VERIFICATION = mutable.SpecialProposalType.SYSTEM_VERIFICATION
  }

  type Flamingos2Filter = mutable.Flamingos2Filter
  object Flamingos2Filter extends EnumObject[mutable.Flamingos2Filter] {
    val Y       = mutable.Flamingos2Filter.Y
    val J_LOW   = mutable.Flamingos2Filter.J_LOW
    val J       = mutable.Flamingos2Filter.J
    val H       = mutable.Flamingos2Filter.H
    val K_SHORT = mutable.Flamingos2Filter.K_SHORT
    val JH      = mutable.Flamingos2Filter.JH
    val HK      = mutable.Flamingos2Filter.HK
  }

  type Flamingos2Disperser = mutable.Flamingos2Disperser
  object Flamingos2Disperser extends EnumObject[mutable.Flamingos2Disperser] {
    val R1200JH = mutable.Flamingos2Disperser.R1200JH
    val R1200HK = mutable.Flamingos2Disperser.R1200HK
    val R3000   = mutable.Flamingos2Disperser.R3000
  }

  type Flamingos2Fpu = mutable.Flamingos2Fpu
  object Flamingos2Fpu extends EnumObject[mutable.Flamingos2Fpu]

  type GmosNDisperser = mutable.GmosNDisperser
  object GmosNDisperser extends EnumObject[mutable.GmosNDisperser]

  type GmosNFilter = mutable.GmosNFilter
  object GmosNFilter extends EnumObject[mutable.GmosNFilter] {
    import mutable.GmosNFilter._
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

  type GmosNFpu = mutable.GmosNFpu
  object GmosNFpu extends EnumObject[mutable.GmosNFpu]

  type GmosNMOSFpu = mutable.GmosNMOSFpu
  object GmosNMOSFpu extends EnumObject[mutable.GmosNMOSFpu]

  type GmosNFpuIfu = mutable.GmosNFpuIfu
  object GmosNFpuIfu extends EnumObject[mutable.GmosNFpuIfu]

  type GmosNFpuNs = mutable.GmosNFpuNs
  object GmosNFpuNs extends EnumObject[mutable.GmosNFpuNs]

  type GmosNWavelengthRegime = mutable.GmosNWavelengthRegime
  object GmosNWavelengthRegime extends EnumObject[mutable.GmosNWavelengthRegime]

  type GmosSDisperser = mutable.GmosSDisperser
  object GmosSDisperser extends EnumObject[mutable.GmosSDisperser]

  type GmosSFilter = mutable.GmosSFilter
  object GmosSFilter extends EnumObject[mutable.GmosSFilter] {
    import mutable.GmosSFilter._
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

  type GmosSFpu = mutable.GmosSFpu
  object GmosSFpu extends EnumObject[mutable.GmosSFpu]

  type GmosSMOSFpu = mutable.GmosSMOSFpu
  object GmosSMOSFpu extends EnumObject[mutable.GmosSMOSFpu]

  type GmosSFpuIfu = mutable.GmosSFpuIfu
  object GmosSFpuIfu extends EnumObject[mutable.GmosSFpuIfu]

  type GmosSFpuIfuNs = mutable.GmosSFpuIfuNs
  object GmosSFpuIfuNs extends EnumObject[mutable.GmosSFpuIfuNs]

  type GmosSFpuNs = mutable.GmosSFpuNs
  object GmosSFpuNs extends EnumObject[mutable.GmosSFpuNs]

  type GmosSWavelengthRegime = mutable.GmosSWavelengthRegime
  object GmosSWavelengthRegime extends EnumObject[mutable.GmosSWavelengthRegime]

  type GnirsCrossDisperser = mutable.GnirsCrossDisperser
  object GnirsCrossDisperser extends EnumObject[mutable.GnirsCrossDisperser] {
    val LXD = mutable.GnirsCrossDisperser.LXD
  }

  type GnirsDisperser = mutable.GnirsDisperser
  object GnirsDisperser extends EnumObject[mutable.GnirsDisperser] {
    val D_10 = mutable.GnirsDisperser.D_10
  }

  type GnirsFilter = mutable.GnirsFilter
  object GnirsFilter extends EnumObject[mutable.GnirsFilter]

  type GnirsFpu = mutable.GnirsFpu
  object GnirsFpu extends EnumObject[mutable.GnirsFpu]

  type GnirsPixelScale = mutable.GnirsPixelScale
  object GnirsPixelScale extends EnumObject[mutable.GnirsPixelScale] {
    val PS_005 = mutable.GnirsPixelScale.PS_005
    val PS_015 = mutable.GnirsPixelScale.PS_015
  }

  type GnirsCentralWavelength = mutable.GnirsCentralWavelength
  object GnirsCentralWavelength extends EnumObject[mutable.GnirsCentralWavelength]

  type GracesFiberMode = mutable.GracesFiberMode
  object GracesFiberMode extends EnumObject[mutable.GracesFiberMode]

  type GracesReadMode = mutable.GracesReadMode
  object GracesReadMode extends EnumObject[mutable.GracesReadMode]

  type GpiObservingMode = mutable.GpiObservingMode
  object GpiObservingMode extends EnumObject[mutable.GpiObservingMode] {
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

  type GpiDisperser = mutable.GpiDisperser
  object GpiDisperser extends EnumObject[mutable.GpiDisperser]

  type GuidingEvaluation = mutable.GuidingEvaluation
  object GuidingEvaluation extends EnumObject[mutable.GuidingEvaluation] {
    final val SUCCESS = mutable.GuidingEvaluation.SUCCESS
    final val CAUTION = mutable.GuidingEvaluation.CAUTION
    final val WARNING = mutable.GuidingEvaluation.WARNING
    final val FAILURE = mutable.GuidingEvaluation.FAILURE
  }

  type KeckInstrument = mutable.KeckInstrument
  object KeckInstrument extends EnumObject[mutable.KeckInstrument]

  type MichelleFilter = mutable.MichelleFilter
  object MichelleFilter extends EnumObject[mutable.MichelleFilter]

  type MichellePolarimetry = mutable.MichellePolarimetry
  object MichellePolarimetry extends EnumObject[mutable.MichellePolarimetry] {
    val YES = mutable.MichellePolarimetry.YES
    val NO = mutable.MichellePolarimetry.NO
  }

  type MichelleFpu = mutable.MichelleFpu
  object MichelleFpu extends EnumObject[mutable.MichelleFpu]

  type MichelleDisperser = mutable.MichelleDisperser
  object MichelleDisperser extends EnumObject[mutable.MichelleDisperser]

  type NiciDichroic = mutable.NiciDichroic
  object NiciDichroic extends EnumObject[mutable.NiciDichroic] {
    val MIRROR = mutable.NiciDichroic.MIRROR
    val OPEN = mutable.NiciDichroic.OPEN
  }

  type NiciBlueFilter = mutable.NiciBlueFilter
  object NiciBlueFilter extends EnumObject[mutable.NiciBlueFilter]

  type NiciFpm = mutable.NiciFpm
  object NiciFpm extends EnumObject[mutable.NiciFpm]

  type NiciRedFilter = mutable.NiciRedFilter
  object NiciRedFilter extends EnumObject[mutable.NiciRedFilter]

  type NifsDisperer = mutable.NifsDisperser
  object NifsDisperser extends EnumObject[mutable.NifsDisperser] {
    val Z = mutable.NifsDisperser.Z
  }

  type NifsOccultingDisk = mutable.NifsOccultingDisk
  object NifsOccultingDisk extends EnumObject[mutable.NifsOccultingDisk] {
    val OD_NONE = mutable.NifsOccultingDisk.CLEAR
  }

  type NiriCamera = mutable.NiriCamera
  object NiriCamera extends EnumObject[mutable.NiriCamera] {
    val F6  = mutable.NiriCamera.F6
    val F14 = mutable.NiriCamera.F14
    val F32 = mutable.NiriCamera.F32
  }

  type NiriFilter = mutable.NiriFilter
  object NiriFilter extends EnumObject[mutable.NiriFilter] {
    val BBF_LPRIME  = mutable.NiriFilter.BBF_LPRIME
    val BBF_MPRIME  = mutable.NiriFilter.BBF_MPRIME
    val BBF_BRACONT = mutable.NiriFilter.NBF_BRACONT
  }

  type GsaoiFilter = mutable.GsaoiFilter
  object GsaoiFilter extends EnumObject[mutable.GsaoiFilter]

  type Guider = mutable.Guider
  object Guider extends EnumObject[mutable.Guider]

  type ImageQuality = mutable.ImageQuality
  object ImageQuality extends EnumObject[mutable.ImageQuality] {
    val BEST = mutable.ImageQuality.iq20
  }

  type InvestigatorStatus = mutable.InvestigatorStatus
  object InvestigatorStatus extends EnumObject[mutable.InvestigatorStatus] {
    val PH_D  = mutable.InvestigatorStatus.PH_D
    val OTHER = mutable.InvestigatorStatus.OTHER
  }

  type Keyword = mutable.Keyword
  object Keyword extends EnumObject[mutable.Keyword]

  type MagnitudeBand = mutable.MagnitudeBand
  object MagnitudeBand extends EnumObject[mutable.MagnitudeBand]

  type MagnitudeSystem = mutable.MagnitudeSystem
  object MagnitudeSystem extends EnumObject[mutable.MagnitudeSystem]

//  type NiriWavelengthRegime = mutable.NiriWavelengthRegime
//  object NiriWavelengthRegime extends EnumObject[mutable.NiriWavelengthRegime]

  type SemesterOption = mutable.SemesterOption
  object SemesterOption extends EnumObject[mutable.SemesterOption] {
    val A = mutable.SemesterOption.A
    val B = mutable.SemesterOption.B
  }

  type SubaruInstrument = mutable.SubaruInstrument
  object SubaruInstrument extends EnumObject[mutable.SubaruInstrument]

  type SkyBackground = mutable.SkyBackground
  object SkyBackground extends EnumObject[mutable.SkyBackground] {
    val BEST = mutable.SkyBackground.sb20
  }

  type TacCategory = mutable.TacCategory
  object TacCategory extends EnumObject[mutable.TacCategory] {
    val GALACTIC = mutable.TacCategory.GALACTIC
  }

  type TimeUnit = mutable.TimeUnit
  object TimeUnit extends EnumObject[mutable.TimeUnit] {
    val HR = mutable.TimeUnit.HR
    val NIGHT = mutable.TimeUnit.NIGHT
  }

  type TooOption = mutable.TooOption
  object TooOption extends EnumObject[mutable.TooOption] {
    val None = mutable.TooOption.NONE
  }

  type TexesDisperser = mutable.TexesDisperser
  object TexesDisperser extends EnumObject[mutable.TexesDisperser]

  type TrecsDisperser = mutable.TrecsDisperser
  object TrecsDisperser extends EnumObject[mutable.TrecsDisperser]

  type TrecsFilter = mutable.TrecsFilter
  object TrecsFilter extends EnumObject[mutable.TrecsFilter]

  type TrecsFpu = mutable.TrecsFpu
  object TrecsFpu extends EnumObject[mutable.TrecsFpu]

  type WaterVapor = mutable.WaterVapor
  object WaterVapor extends EnumObject[mutable.WaterVapor] {
    val BEST = mutable.WaterVapor.wv20
  }

  type WavelengthRegime = mutable.WavelengthRegime
  object WavelengthRegime extends EnumObject[mutable.WavelengthRegime]
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

