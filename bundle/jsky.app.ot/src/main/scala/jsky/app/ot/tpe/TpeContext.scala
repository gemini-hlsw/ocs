package jsky.app.ot.tpe

import edu.gemini.pot.sp._
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.{IOffsetPosListProvider, ISPDataObject}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.altair.InstAltair
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.env.{Asterism, TargetEnvironment}
import edu.gemini.spModel.target.obsComp.{TargetObsComp, TargetSelection}
import edu.gemini.spModel.target.offset.{OffsetPosBase, OffsetPosList, OffsetPosSelection, OffsetUtil}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider}
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._
import edu.gemini.spModel.obs.{SPObservation, SchedulingBlock}
import edu.gemini.skycalc.Offset

object TpeContext {
  // Simplify access from Java ...
  val empty: TpeContext = TpeContext(None)

  def apply(n: ISPNode): TpeContext = TpeContext(Option(n))

  private[tpe] def find[T](obs: Option[ISPObservation], f: ISPObservation => T): Option[T] = for {
    o <- obs
    c <- Option(f(o))
  } yield c

  /**
    * Attempts to get the TpeContext from the observation on the TPE
    * @return
    */
  def fromTpeManager: Option[TpeContext] =
    for {
      tpe <- Option(TpeManager.get())
      iw  <- Option(tpe.getImageWidget)
      ctx <- Option(iw.getContext)
    } yield ctx
}

import TpeContext._

abstract class TpeSubContext[S <: ISPNode, D >: Null <: ISPDataObject](val shell: Option[S]) {
  val dataObject: Option[D] = for {
    s <- shell
    d <- s.dataObject
  } yield d.asInstanceOf[D]

  def isEmpty: Boolean = dataObject.isEmpty

  def isDefined: Boolean = !isEmpty

  def get: D = dataObject.get

  def orNull: D = dataObject.orNull

  def commit() {
    for (s <- shell; d <- dataObject) {
      s.setDataObject(d)
    }
  }
}

final class TargetContext(obs: Option[ISPObservation]) extends TpeSubContext[ISPObsComponent, TargetObsComp](find(obs, SPTreeUtil.findTargetEnvNode)) {
  def env: Option[TargetEnvironment] = dataObject.map(_.getTargetEnvironment)

  def envOrNull: TargetEnvironment = env.orNull

  def envOrDefault: TargetEnvironment = env.getOrElse(TargetEnvironment.create(new SPTarget));

  def asterism: Option[Asterism] = env.map(_.getAsterism)

  def asterismOrNull: Asterism = asterism.orNull // can't call this from Java, so we provide it

  def asterismOrZero: Asterism = asterism.getOrElse(Asterism.single(new SPTarget()))

  def selected: Option[SPTarget] = for {
    s <- shell
    e <- env
    r <- TargetSelection.getTargetForNode(e, s).asScalaOpt
  } yield r

  def selectedOrNull: SPTarget = selected.orNull
}

final class InstrumentContext(obs: Option[ISPObservation]) extends TpeSubContext[ISPObsComponent, SPInstObsComp](find(obs, SPTreeUtil.findInstrument)) {
  def is(t: SPComponentType): Boolean =
    dataObject.exists(_.getType == t)

  def ifIs[T >: Null <: SPInstObsComp](t: SPComponentType): Option[T] =
    dataObject.filter(_.getType == t).map(_.asInstanceOf[T])

  def orNull[T >: Null <: SPInstObsComp](t: SPComponentType): T =
    ifIs(t).orNull

  def posAngleOrZero: Double =
    dataObject.map(_.getPosAngleDegrees).getOrElse(0.0)

  def issPortOrDefault: IssPort = (dataObject flatMap {
    case pp: IssPortProvider => Option(pp.getIssPort)
    case _ => None
  }).getOrElse(IssPort.DEFAULT)
}

final class BasicObsComponentContext[D >: Null <: ISPDataObject](obs: Option[ISPObservation], f: ISPObservation => ISPObsComponent) extends TpeSubContext[ISPObsComponent, D](TpeContext.find(obs, f))

final class SingleOffsetListContext(shell: ISPNode) extends TpeSubContext[ISPNode, IOffsetPosListProvider[OffsetPosBase]](Some(shell)) {
  val posList: OffsetPosList[OffsetPosBase] = dataObject.get.getPosList

  val selected: Option[OffsetPosBase] = Option(OffsetPosSelection.apply(shell).firstSelectedPosition(posList))

  def selectedOrNull: OffsetPosBase = selected.orNull

  def allSelected: List[OffsetPosBase] = allSelectedJava.asScala.toList

  val allSelectedJava: java.util.List[OffsetPosBase] =
    OffsetPosSelection.apply(shell).selectedPositions(posList)

}

final class AllOffsetListContext(obs: Option[ISPObservation], node: Option[ISPNode]) {
  private def isOffsetNode(n: ISPNode): Boolean =
    n.dataObject exists {
      case plp: IOffsetPosListProvider[_] => plp.isOffsetPosListProviderEnabled
      case _ => false
    }

  val all: List[SingleOffsetListContext] =
    obs.toList flatMap {
      _.toStream.filter(isOffsetNode).toList
    } map {
      new SingleOffsetListContext(_)
    }

  def allJava: java.util.List[SingleOffsetListContext] = all.asJava

  val allPosLists: List[OffsetPosList[OffsetPosBase]] = all.map(_.posList)

  def allPosListsJava: java.util.List[OffsetPosList[OffsetPosBase]] =
    allPosLists.asJava

  def scienceOffsets: Set[Offset] = scienceOffsetsJava.asScala.toSet

  def scienceOffsetsJava: java.util.Set[Offset] = OffsetUtil.getSciencePositions2(allPosLists.toArray)

  val selected: Option[SingleOffsetListContext] =
    node flatMap {
      n =>
        n.findAncestor(isOffsetNode) orElse n.findDescendant(isOffsetNode)
    } flatMap {
      n =>
        all.find(_.shell.exists(_ == n))
    } orElse all.headOption

  def selectedOrNull: SingleOffsetListContext = selected.orNull

  def selectedPosList: Option[OffsetPosList[OffsetPosBase]] = selected.map(_.posList)

  def selectedPosListOrNull: OffsetPosList[OffsetPosBase] = selectedPosList.orNull

  def selectedPos: Option[OffsetPosBase] = selected.flatMap(_.selected)

  def selectedPosOrNull: OffsetPosBase = selectedPos.orNull

  // A bit aggressive in that we commit all the offset iterators every time
  // regardless of whether they have changed.  The alternative is to watch
  // every offset position and every offset list to determine which one changed.
  def commit() {
    all.foreach(_.commit())
  }
}

case class TpeContext(node: Option[ISPNode]) {
  val nodeOrNull: ISPNode = node.orNull
  val progShell: Option[ISPProgram] = node.flatMap(n => Option(n.getProgram))
  val obsShell: Option[ISPObservation] = node.flatMap(n => Option(n.getContextObservation))
  val obsKey: Option[SPNodeKey] = obsShell.map(_.getNodeKey)
  val obsShellOrNull: ISPObservation = obsShell.orNull

  val noneSite: JOption[Site] = JNone.instance[Site]
  val site: JOption[Site] = obsShell.fold(noneSite)(x => ObsContext.getSiteFromObservation(x))

  def isEmpty: Boolean = obsShell.isEmpty

  val targets = new TargetContext(obsShell)
  val instrument = new InstrumentContext(obsShell)
  val siteQuality = new BasicObsComponentContext[SPSiteQuality](obsShell, SPTreeUtil.findObsCondNode)
  val altair = new BasicObsComponentContext[InstAltair](obsShell, SPTreeUtil.findObsComponent(_, InstAltair.SP_TYPE))
  val gems = new BasicObsComponentContext[Gems](obsShell, SPTreeUtil.findObsComponent(_, Gems.SP_TYPE))
  val offsets = new AllOffsetListContext(obsShell, node)

  def obsContext: Option[ObsContext] = siteQuality.dataObject.flatMap(sq => obsContextWithConditions(sq.conditions))

  def obsContextJava: JOption[ObsContext] = obsContext.asGeminiOpt

  def obsContextWithConditions(c: Conditions): Option[ObsContext] = for {
    s <- obsShell
    t <- targets.env
    i <- instrument.dataObject
    ao = (gems.dataObject orElse altair.dataObject).orNull
    obs = s.getDataObject.asInstanceOf[SPObservation]
  } yield ObsContext.create(obs.getAgsStrategyOverride, t, i, site, c, offsets.scienceOffsetsJava, ao, obs.getSchedulingBlock)

  def obsContextJavaWithConditions(c: Conditions): JOption[ObsContext] =
    obsContextWithConditions(c).asGeminiOpt

  def schedulingBlock: Option[SchedulingBlock] =
    obsShell.flatMap(_.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt)

  def schedulingBlockStart: Option[Long] =
    schedulingBlock.map(_.start)

  def schedulingBlockJava: JOption[SchedulingBlock] =
    schedulingBlock.asGeminiOpt

  def schedulingBlockStartJava: JOption[java.lang.Long] =
    schedulingBlockStart.map(a => a : java.lang.Long).asGeminiOpt

}
