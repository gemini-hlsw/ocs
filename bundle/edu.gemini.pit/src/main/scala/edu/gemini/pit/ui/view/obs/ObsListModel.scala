package edu.gemini.pit.ui.view.obs

import edu.gemini.model.p1.immutable._
import scalaz._
import Scalaz._

import scala.language.existentials

// This class takes a list of observations, a band, and a triple of orderings (from the reorder bar) and generates a
// list of ObsListElems that mimic a tree view. The indentation is all handled at the UI level; the view is fundamentally
// a list, which makes things kind of complicated. It seemed like a good idea at the time. Changes to this class and
// to ObsListElem should be done very carefully because some behaviors are subtle.
case class ObsListModel(all:List[Observation], band:Band, gs:(ObsListGrouping[_], ObsListGrouping[_], ObsListGrouping[_]), sem:Semester) {
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  // Partition our list of obs into those that we can edit and those that are filtered out
  val (visible, hidden) = all.partition(_.band == band)

  /**
   * An ordered list of ObsListElem, which is a flattened tree structure representing the visible
   * observations grouped by <code>gs</code>. Use the depth() method to determine how far each item
   * should be indented.
   */
  lazy val elems = {
    
    // Sort the observations, turn them into ObsElems, and expand the list
    val es = visible.sortBy(_.toString).map(ObsElem(_))
    expand(es)

  }

  /**
   * Returns a new ObsListModel with the specified additional observation.
   */
  def +(o:Observation) = copy(all = o :: all)

  /**
   * Returns a new ObsListModel with the specified additional observations.
   */
  def ++(os:Seq[Observation]) = copy(all = all ++ os)

  /**
   * Returns a new ObsListModel with the specified ObsListElem removed. It's unclear what this should
   * really mean, so for now let's assume it means that we should simple clear out the grouped element
   * from child observations (i.e., don't treat groups like folders).
   */
  def -(e:ObsListElem) = {

    def update(o:Observation) = e match {
      case _:ObsElem        => o.copy(intTime = None, calculatedTimes = None)
      case _:ConditionGroup => o.copy(condition = None)
      case _:TargetGroup    => o.copy(target = None)
      case _:BlueprintGroup => o.copy(blueprint = None)
    }

    val os = selected(e)

    (this /: os.zip(os.map(update)))(_.replacePair(_))
  }

  /**
   * Returns a new ObsListModel with the specified mapping applied to all <i>visible</i> observations.
   */
  def map(f:Observation => Observation) = copy(all = visible.map(f) ++ hidden)

  /**
   * Returns a new ObsListModel with the first occurrence of the specified <i>visible</i> obs replaced, if it exists.
   */
  def replace(o:Observation, o0:Observation) = visible.indexOf(o) match {
    case -1 => this
    case n  => copy(all = visible.updated(n, o0) ++ hidden)
  }

  private val replacePair = (replace _).tupled

  /**
   * Returns a new ObsListModel with the specified flat mapping applied to all <i>visible</i> observations.
   */
  def flatMap(f:Observation => List[Observation]) = copy(all = visible.flatMap(f) ++ hidden)

  /**
   * How deep would a given ObsListElem be in the current grouping hierarchy? Zero means it's a root
   */
  def depth(o:ObsListElem) = o match {
    case g:ObsGroup[_] => gs.productIterator.indexOf(g.grouping)
    case _:ObsElem     => gs.productArity
  }

  /**
   * Does it make sense to drag the given element?
   */
  def isDraggable(o:ObsListElem) = depth(o) > 0

  /**
   * Does it make sense to drop <code>source</code> on <code>target</code>?
   */
  def isDroppable(source:(ObsListModel, List[ObsListElem]), target:ObsGroup[_]):Boolean =
    source._1.gs == gs && // Groupings must be the same
      source._2.forall(source._1.depth(_) == depth(target) + 1)

  /**
   * What visible observations are children of the specified group?
   */
  def childrenOf(g:ObsGroup[_]):List[Observation] = {
    val groupings = Seq(gs._1, gs._2, gs._3).take(depth(g) + 1)
    visible.filter {o => groupings.forall((og:ObsListGrouping[_]) => og.matches(g, o))}
  }

  /**
   * What visible observations are selected if the given ObsListElem is selected?
   */
  def selected(e:ObsListElem) = e match {
    case ObsElem(o)    => List(o)
    case g:ObsGroup[_] => childrenOf(g)
  }


  def move(source:(ObsListModel, ObsListElem), target:ObsGroup[_]):Option[ObsListModel] = isDroppable((source._1, List(source._2)), target) match {
    case false => None
    case true  =>

      // We're dropping a list of observations (os) onto an ObsGroup. There are two operations here. First, we can
      // stamp the grouped properties of the target group on the observations; second, we can clear the ungrouped
      // properties of the group from the observations. The first operation gives us the dropped obs. The second gives
      // us what's left of the original obs (so we have empty folders left behind when dragging).

      // Our groups, and the position at which we want to split the stamp/clear list
      val (toStamp, toClear) = Seq(gs._1, gs._2, gs._3).splitAt(depth(target) + 1)

      // To stamp and clear we fold over the associated groups. When stamping we also want to be sure to switch the
      // band to whatever this model's primary band is. This lets us paste stuff between BAND_1_2 and BAND_3.
      def stamp(o:Observation) = (o /: toStamp)((o, g) => g.stamp(target, o).copy(band = band))
      def clear(o:Observation) = (o.copy(intTime = None, calculatedTimes = None) /: toClear)((o, g) => g.clear(o))

      // Our list of dropped Observations,
      val os = source._1.selected(source._2)

      // Now we fold over the obs, replacing each with its cleared version, and finally add the stamped obs.
      Some((this /: os.zip(os.map(clear)))(_.replacePair(_)) ++ os.map(stamp))

  }

  def paste(source:(ObsListModel, List[ObsListElem]), target:Option[ObsGroup[_]]):ObsListModel = {
    (this /: source._2.map((source._1, _)))(_.pasteOne(_, target))
  }

  
  // When you paste, the target might be valid, invalid, or missing altogether. These are all legal.
  def pasteOne(source:(ObsListModel, ObsListElem), target:Option[ObsGroup[_]]):ObsListModel = {

    // Our list of dropped Observations
    val os0 = source._1.selected(source._2)
    val droppable = ~target.map(isDroppable((source._1, List(source._2)), _))

    // Ok we want to wipe out any data contained in groupings above source
    val prefix = source._1.depth(source._2)
    val toClear = Seq(gs._1, gs._2, gs._3).take(prefix)
    def clear(o:Observation) = (o.copy() /: toClear)((o, g) => g.clear(o))

    val os = os0.map(clear)
    
    target match {
      case None                  => this ++ os.map(_.copy(band = band))
      case Some(t) if !droppable => this ++ os.map(_.copy(band = band))
      case Some(target)          =>

        // Our groups, and the position at which we want to split the stamp/clear list
        val (toStamp, _) = Seq(gs._1, gs._2, gs._3).splitAt(depth(target) + 1)

        // To stamp and clear we fold over the associated groups. When stamping we also want to be sure to switch the
        // band to whatever this model's primary band is. This lets us paste stuff between BAND_1_2 and BAND_3.
        def stamp(o:Observation) = (o /: toStamp)((o, g) => g.stamp(target, o).copy(band = band))

        // Our list of dropped Observations,
        val os = source._1.selected(source._2)

        // Now we just add the stamped obs.
        this ++ os.map(stamp)

    }

  }



  def cut(elem:ObsListElem):ObsListModel = elem match {
    case target:ObsGroup[_] =>
      val (_, toClear) = Seq(gs._1, gs._2, gs._3).splitAt(depth(target))
      def clear(o:Observation) = (o.copy(intTime = None, calculatedTimes = None) /: toClear)((o, g) => g.clear(o))
      val os = selected(target)
      (this /: os.zip(os.map(clear)))(_.replacePair(_))
    case o:ObsElem          => this - o
  }

  // Group the given ObsElems (which is just a trivally wrapped Observation) by three levels of grouping, then
  // flatten the tree structure into a list. This looks worse than it is. The only tricky bit is the |> operator,
  // which is defined on ObsGroup.
  private def expand(os:List[ObsElem]) = {

    // A few helpers
    def cons[A](p:(A, List[A])) = p._1 :: p._2
    def lift[A](f:Observation => A) = {e:ObsElem => f(e.o)}

    val (ga, gb, gc) = (lift(gs._1), lift(gs._2), lift(gs._3))

    lazy val outerGroups = os.groupBy(ga).toList.sortBy(_._1.toString).map {
      case (a, osByA) =>
        val innerGroups:List[ObsListElem] = osByA.groupBy(gb).toList.sortBy(_._1.toString).map {
          case (b, osByAB) =>
            val innerInnerGroups:List[ObsListElem] = osByAB.groupBy(gc).toList.sortBy(_._1.toString).map {
              case (c, osByABC) => (a |> b |> c, osByABC)
            }.flatMap(cons).toList
            (a |> b, innerInnerGroups)
        }.filterNot(p => p._1.isEmpty && p._2.isEmpty).flatMap(cons).toList
        (a, innerGroups)
    }.flatMap(cons).toList:List[ObsListElem]

    lazy val outerGroupsFiltered = os.groupBy(ga).toList.sortBy(_._1.toString).map {
      case (a, osByA) =>
        val innerGroups:List[ObsListElem] = osByA.groupBy(gb).toList.sortBy(_._1.toString).map {
          case (b, osByAB) =>
            val innerInnerGroups:List[ObsListElem] = osByAB.groupBy(gc).toList.sortBy(_._1.toString).map {
              case (c, osByABC) => (a |> b |> c, osByABC.filterNot(_.o.intTime.isEmpty))
            }.filterNot(p => p._1.isEmpty && p._2.isEmpty).flatMap(cons).toList
            (a |> b, innerInnerGroups)
        }.filterNot(p => p._1.isEmpty && p._2.isEmpty).flatMap(cons).toList
        (a, innerGroups)
    }.flatMap(cons).toList:List[ObsListElem]

    if (ObsListModel.SHOW_EMPTY_LEAVES) outerGroups else outerGroupsFiltered

  }


}

object ObsListModel {
  private val SHOW_EMPTY_LEAVES = true
}
