package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsStrategy
import edu.gemini.catalog.api.CatalogName.GaiaGemini
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey._
import edu.gemini.spModel.core.{NiciBandsList, Site}
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe
import edu.gemini.spModel.guide.{GuideProbe, GuideProbeUtil}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe

import scala.Function.const

import scalaz.syntax.foldable._

// Used to implement AgsRegistrar
object Strategy {
  import SingleProbeStrategyParams._

  val AltairAowfs: SingleProbeStrategy = SingleProbeStrategy(AltairAowfsKey,     AltairAowfsParams)
  val Flamingos2Oiwfs: SingleProbeStrategy = SingleProbeStrategy(Flamingos2OiwfsKey, Flamingos2OiwfsParams)
  val GmosNorthOiwfs: SingleProbeStrategy = SingleProbeStrategy(GmosNorthOiwfsKey,  GmosOiwfsParams(Site.GN))
  val GmosSouthOiwfs: SingleProbeStrategy = SingleProbeStrategy(GmosSouthOiwfsKey,  GmosOiwfsParams(Site.GS))
  val GnirsOiwfs: SingleProbeStrategy = SingleProbeStrategy(GnirsOiwfsKey,      GnirsOiwfsParams)
  val NifsOiwfs: SingleProbeStrategy = SingleProbeStrategy(NifsOiwfsKey,       NifsOiwfsParams)
  val NiriOiwfs: SingleProbeStrategy = SingleProbeStrategy(NiriOiwfsKey,       NiriOiwfsParams)
  val Pwfs1North: SingleProbeStrategy = SingleProbeStrategy(Pwfs1NorthKey,      PwfsParams(Site.GN, PwfsGuideProbe.pwfs1))
  val Pwfs2North: SingleProbeStrategy = SingleProbeStrategy(Pwfs2NorthKey,      PwfsParams(Site.GN, PwfsGuideProbe.pwfs2))
  val Pwfs1South: SingleProbeStrategy = SingleProbeStrategy(Pwfs1SouthKey,      PwfsParams(Site.GS, PwfsGuideProbe.pwfs1))
  val Pwfs2South: SingleProbeStrategy = SingleProbeStrategy(Pwfs2SouthKey,      PwfsParams(Site.GS, PwfsGuideProbe.pwfs2))
  val GhostPwfs1: GhostStrategy = GhostStrategy(GhostPwfs1Key,            Pwfs1South)
  val GhostPwfs2: GhostStrategy = GhostStrategy(GhostPwfs2Key,            Pwfs2South)

  val NiciOiwfs: ScienceTargetStrategy = ScienceTargetStrategy(NiciOiwfsKey,     NiciOiwfsGuideProbe.instance, NiciBandsList)
  val Off: OffStrategy.type = OffStrategy

  val GemsNgs2: Ngs2Strategy = Ngs2Strategy(GaiaGemini, None)

  /**
   * All strategies that correspond to top-level AGS queries. Internally, some
   * strategies (e.g., NGS2Strategy) may combine secondary strategies that are
   * not listed here.
   */
  val Auto: List[AgsStrategy] = List(
    AltairAowfs,
    Flamingos2Oiwfs,
    GemsNgs2,
    GhostPwfs1,
    GhostPwfs2,
    GmosNorthOiwfs,
    GmosSouthOiwfs,
    GnirsOiwfs,
    NiciOiwfs,
    NifsOiwfs,
    NiriOiwfs,
    Pwfs1North,
    Pwfs2North,
    Pwfs1South,
    Pwfs2South,
    Off
  )

  private val KeyMap: Map[AgsStrategyKey, AgsStrategy] =
    Auto.map(s => s.key -> s).toMap

  /** Obtains the matching Strategy configured for use in AGS. */
  def fromKey(k: AgsStrategyKey): Option[AgsStrategy] = KeyMap.get(k)

  //
  // The logic for determining the valid strategies for an observation context,
  // in order of preference.
  //

  private def oiStategies(ctx: ObsContext, oiStrategy: AgsStrategy): List[AgsStrategy] = {
    val pwfs = ctx.getSite.asScalaOpt.toList.flatMap {
      case Site.GN => List(Pwfs2North, Pwfs1North)
      case Site.GS => List(Pwfs2South, Pwfs1South)
    }
    if (allSidereal(ctx)) oiStrategy :: pwfs
    else                  pwfs :+ oiStrategy
  }

  private def allSidereal(ctx: ObsContext): Boolean =
    ctx.getTargets.getAsterism.allSidereal

  val InstMap: Map[SPComponentType, ObsContext => List[AgsStrategy]] = Map[SPComponentType, ObsContext => List[AgsStrategy]](
    SPComponentType.INSTRUMENT_ACQCAM     -> const(List(Pwfs1North, Pwfs2North, Pwfs1South, Pwfs2South)),

    SPComponentType.INSTRUMENT_FLAMINGOS2 -> ((ctx: ObsContext) =>
      List(GemsNgs2) ++ oiStategies(ctx, Flamingos2Oiwfs)
    ),

    SPComponentType.INSTRUMENT_GHOST      -> const(List(GhostPwfs2, GhostPwfs1)),

    SPComponentType.INSTRUMENT_GMOS       -> ((ctx: ObsContext) => {
      val ao = ctx.getAOComponent.asScalaOpt
      if (ao.exists(_.isInstanceOf[InstAltair])) {
        ao.get.asInstanceOf[InstAltair].getMode match {
          case AltairParams.Mode.LGS_P1 => List(Pwfs1North)
          case AltairParams.Mode.LGS_OI => List(GmosNorthOiwfs)
          case _                        => List(AltairAowfs)
        }
      } else oiStategies(ctx, GmosNorthOiwfs)
    }),

    SPComponentType.INSTRUMENT_GMOSSOUTH  -> ((ctx: ObsContext) => oiStategies(ctx, GmosSouthOiwfs)),

    SPComponentType.INSTRUMENT_GNIRS      -> const(List(AltairAowfs, Pwfs2North, Pwfs1North, GnirsOiwfs)),
    SPComponentType.INSTRUMENT_GSAOI      -> const(List(GemsNgs2, Pwfs1South)),
    SPComponentType.INSTRUMENT_IGRINS2    -> const(List(Pwfs2North, Pwfs1North)),
    SPComponentType.INSTRUMENT_MICHELLE   -> const(List(Pwfs2North, Pwfs1North)),
    SPComponentType.INSTRUMENT_NICI       -> const(List(NiciOiwfs, Pwfs2South, Pwfs1South)),
    SPComponentType.INSTRUMENT_NIFS       -> const(List(AltairAowfs, Pwfs2North, Pwfs1North, NifsOiwfs)),
    SPComponentType.INSTRUMENT_NIRI       -> const(List(AltairAowfs, Pwfs2North, Pwfs1North, NiriOiwfs)),
    SPComponentType.INSTRUMENT_PHOENIX    -> const(List(Pwfs2North, Pwfs2South, Pwfs1North, Pwfs1South)),
    SPComponentType.INSTRUMENT_TEXES      -> const(List(Pwfs2North, Pwfs1North)),
    SPComponentType.INSTRUMENT_TRECS      -> const(List(Pwfs2South, Pwfs1South)),
    SPComponentType.INSTRUMENT_VISITOR    -> const(List(Pwfs2North, Pwfs2South, Pwfs1North, Pwfs1South))
  )

  private def guidersAvailable(ctx: ObsContext)(s: AgsStrategy): Boolean = {
    val isAvailable = GuideProbeUtil.instance.isAvailable(ctx, _: GuideProbe)
    s match {
      case SingleProbeStrategy(_, params, _) => isAvailable(params.guideProbe)
      case GhostStrategy(_, delegate)        => guidersAvailable(ctx)(delegate)
      case ScienceTargetStrategy(_, gp, _)   => isAvailable(gp)
      case Ngs2Strategy(_, _)                => isAvailable(CanopusWfs.cwfs3) && isAvailable(PwfsGuideProbe.pwfs1)
      case _                                 => false
    }
  }

  private def siteAvailability(ctx: ObsContext)(s: AgsStrategy): Boolean = s match {
      case Pwfs1North | Pwfs2North => ctx.getSite.asScalaOpt.forall(_ == Site.GN)
      case Pwfs1South | Pwfs2South => ctx.getSite.asScalaOpt.forall(_ == Site.GS)
      case _                       => true
    }

  def validStrategies(ctx: ObsContext): List[AgsStrategy] =
    InstMap.get(ctx.getInstrument.getType).map(
      _.apply(ctx)
    ).toList.flatten.filter(guidersAvailable(ctx)).filter(siteAvailability(ctx)) :+ OffStrategy
}
