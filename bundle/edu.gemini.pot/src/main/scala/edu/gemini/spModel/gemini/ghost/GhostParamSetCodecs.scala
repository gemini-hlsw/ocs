package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.HighResolution._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.StandardResolution._
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.target.TargetParamSetCodecs._


object GhostParamSetCodecs {
  private val IFU1 = "ifu1"
  private val IFU2 = "ifu2"
  private val Target = "target"
  private val ExplicitGuideFiberState = "explicitGuideFiberState"
  private val Base = "base"
  
  implicit val GuideFiberStateParamCodec: ParamCodec[GuideFiberState] =
    ParamCodec[String].xmap(GuideFiberState.unsafeFromString, _.name)

  implicit val GhostTargetParamSetCodec: ParamSetCodec[GhostTarget] =
    ParamSetCodec.initial(GhostTarget.empty)
      .withParamSet(Target, GhostTarget.target)
      .withOptionalParam(ExplicitGuideFiberState, GhostTarget.explicitGuideFiberState)

  implicit val SingleTargetParamSetCodec: ParamSetCodec[SingleTarget] =
    ParamSetCodec.initial(emptySingleTarget)
      .withParamSet(IFU1, SingleTargetIFU1)
      .withOptionalParamSet(Base, SingleTargetBase)

  implicit val DualTargetParamSetCodec: ParamSetCodec[DualTarget] =
    ParamSetCodec.initial(emptyDualTarget)
      .withParamSet(IFU1, DualTargetIFU1)
      .withParamSet(IFU2, DualTargetIFU2)
      .withOptionalParamSet(Base, DualTargetBase)

  implicit val TargetPlusSkyParamSetCodec: ParamSetCodec[TargetPlusSky] =
    ParamSetCodec.initial(emptyTargetPlusSky)
      .withParamSet(IFU1, TargetPlusSkyIFU1)
      .withParamSet(IFU2, TargetPlusSkyIFU2)
      .withOptionalParamSet(Base, TargetPlusSkyBase)

  implicit val SkyPlusTargetParamSetCodec: ParamSetCodec[SkyPlusTarget] =
    ParamSetCodec.initial(emptySkyPlusTarget)
      .withParamSet(IFU1, SkyPlusTargetIFU1)
      .withParamSet(IFU2, SkyPlusTargetIFU2)
      .withOptionalParamSet(Base, SkyPlusTargetBase)

  implicit val HRTargetParamSetCodec: ParamSetCodec[HighResolutionTarget] =
    ParamSetCodec.initial(emptyHRTarget)
      .withParamSet(IFU1, HRTargetIFU1)
      .withOptionalParamSet(Base, HRTargetBase)


  implicit val HRTargetPlusSkyParamSetCodec: ParamSetCodec[HighResolutionTargetPlusSky] =
    ParamSetCodec.initial(emptyHRTargetPlusSky)
      .withParamSet(IFU1, HRTargetPlusSkyIFU1)
      .withParamSet(IFU2, HRTargetPlusSkyIFU2)
      .withOptionalParamSet(Base, HRTargetPlusSkyBase)
}
