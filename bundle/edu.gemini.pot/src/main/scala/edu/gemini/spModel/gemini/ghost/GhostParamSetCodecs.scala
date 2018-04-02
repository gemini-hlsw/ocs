package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostStandardResTargets._
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.pio.{ParamSet, Pio}
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.target.TargetParamSetCodecs._

import scalaz._
import Scalaz._


object GhostParamSetCodecs {
  private val IFU1 = "ifu1"
  private val IFU2 = "ifu2"
  private val Target = "target"
  private val Targets = "targets"
  private val ExplicitGuideFiberState = "explicitGuideFiberState"
  private val SingleTarget  = "singleTarget"
  private val DualTarget    = "dualTarget"
  private val TargetPlusSky = "targetPlusSky"
  private val SkyPlusTarget = "skyPlusTarget"
  private val GhostType = "ghostType"
  private val Base = "base"
  private val Tag = "tag"
  
  implicit val GuideFiberStateParamCodec: ParamCodec[GuideFiberState] =
    ParamCodec[String].xmap(GuideFiberState.unsafeFromString, _.name)

  implicit val GhostTargetParamSetCodec: ParamSetCodec[GhostTarget] =
    ParamSetCodec.initial(GhostTarget.empty)
      .withParamSet(Target, GhostTarget.target)
      .withOptionalParam(ExplicitGuideFiberState, GhostTarget.explicitGuideFiberState)

  implicit val SingleTargetParamSetCodec: ParamSetCodec[SingleTarget] =
    ParamSetCodec.initial(emptySingleTarget)
      .withParamSet(IFU1, SingleTargetIFU1)

  implicit val DualTargetParamSetCodec: ParamSetCodec[DualTarget] =
    ParamSetCodec.initial(emptyDualTarget)
      .withParamSet(IFU1, DualTargetIFU1)
      .withParamSet(IFU2, DualTargetIFU2)

  implicit val TargetPlusSkyParamSetCodec: ParamSetCodec[TargetPlusSky] =
    ParamSetCodec.initial(emptyTargetPlusSky)
      .withParamSet(IFU1, TargetPlusSkyIFU1)
      .withParamSet(IFU2, TargetPlusSkyIFU2)

  implicit val SkyPlusTargetParamSetCodec: ParamSetCodec[SkyPlusTarget] =
    ParamSetCodec.initial(emptySkyPlusTarget)
      .withParamSet(IFU1, SkyPlusTargetIFU1)
      .withParamSet(IFU2, SkyPlusTargetIFU2)

  implicit val GhostStandardResTargetsParamSetCodec: ParamSetCodec[GhostStandardResTargets] =
    new ParamSetCodec[GhostStandardResTargets] {
      val pf = new PioXmlFactory

      override def encode(key: String, a: GhostStandardResTargets): ParamSet = {
        val (tag, ps) = a match {
          case t: SingleTarget  => (SingleTarget,  SingleTargetParamSetCodec.encode(key, t))
          case t: DualTarget    => (DualTarget,    DualTargetParamSetCodec.encode(key, t))
          case t: TargetPlusSky => (TargetPlusSky, TargetPlusSkyParamSetCodec.encode(key, t))
          case t: SkyPlusTarget => (SkyPlusTarget, SkyPlusTargetParamSetCodec.encode(key, t))
        }
        Pio.addParam(pf, ps, Tag, tag)
        ps
      }

      override def decode(ps: ParamSet): PioError \/ GhostStandardResTargets = {
        Option(ps.getParam(Tag)).map(_.getValue) \/> MissingKey(Tag) flatMap {
          case SingleTarget  => SingleTargetParamSetCodec.decode(ps)
          case DualTarget    => DualTargetParamSetCodec.decode(ps)
          case TargetPlusSky => TargetPlusSkyParamSetCodec.decode(ps)
          case SkyPlusTarget => SkyPlusTargetParamSetCodec.decode(ps)
          case other           => UnknownTag(other, "GhostStandardResTargets").left
        }
      }
    }

  implicit val StandardResolutionParamSetCodec: ParamSetCodec[StandardResolution] =
    ParamSetCodec.initial(StandardResolution.empty)
      .withParamSet(Targets, StandardResolution.Targets)
      .withOptionalParamSet(Base, StandardResolution.Base)

  implicit val HighResolutionParamSetCodec: ParamSetCodec[HighResolution] =
    ParamSetCodec.initial(HighResolution.empty)
      .withParamSet(IFU1, HighResolution.IFU1)
      .withOptionalParamSet(IFU2, HighResolution.IFU2)
      .withOptionalParamSet(Base, HighResolution.Base)

  implicit val GhostAsterismParamSetCodec: ParamSetCodec[GhostAsterism] =
    new ParamSetCodec[GhostAsterism] {
      val pf = new PioXmlFactory

      override def encode(key: String, a: GhostAsterism): ParamSet = {
        val (tag, ps) = a match {
          case a: StandardResolution => (AsterismType.GhostStandardResolution.tag, StandardResolutionParamSetCodec.encode(key, a))
          case a: HighResolution     => (AsterismType.GhostHighResolution.tag,     HighResolutionParamSetCodec.encode(key, a))
        }
        Pio.addParam(pf, ps, GhostType, tag)
        ps
      }

      override def decode(ps: ParamSet): PioError \/ GhostAsterism = {
        Option(ps.getParam(GhostType)).map(_.getValue) \/> MissingKey(GhostType) flatMap {
          case AsterismType.GhostStandardResolution.tag => StandardResolutionParamSetCodec.decode(ps)
          case AsterismType.GhostHighResolution.tag     => HighResolutionParamSetCodec.decode(ps)
          case other                                    => UnknownTag(other, "GhostAsterism").left
        }
      }
    }
}
