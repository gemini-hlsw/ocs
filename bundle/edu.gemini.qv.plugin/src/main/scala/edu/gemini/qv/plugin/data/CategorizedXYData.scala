package edu.gemini.qv.plugin.data

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.filter.core.{EmptyFilter, Filter}

/** Observations categorized along one axis. */
case class CategorizedObservations(ctx: QvContext, categories: Seq[Filter], observations: Set[Obs]) extends CategorizedData(ctx)

/** Observations categorized along one axis. */
case class CategorizedYObservations(ctx: QvContext, yCategories: Seq[Filter], observations: Set[Obs]) extends CategorizedYData(ctx)

/** Categorized data. */
case class CategorizedXYObservations(ctx: QvContext, xCategories: Seq[Filter], yCategories: Seq[Filter], observations: Set[Obs]) extends CategorizedXYData(ctx)

/** Categorized observations including a calculated value for each group of observations. */
case class CategorizedXYValues(ctx: QvContext, xCategories: Seq[Filter], yCategories: Seq[Filter], observations: Set[Obs], calculation: Set[Obs] => Double) extends CategorizedXYData(ctx) {

  /**
   * The result of the calculation for all observations grouped by the groups along the x and y axis.
   */
  lazy val data: Map[Filter, Map[Filter, Double]] = {
    // replace the sets of observations in the inner maps with the results of
    // the calculation for the corresponding set of observations
    xySets.mapValues(yGroupsMap => yGroupsMap.mapValues(obs => calculation(obs)))
  }

  /**
   * The value at the intersection of two groups.
   * @param x
   * @param y
   * @return
   */
  def value(x: Int, y: Int): Double = {
    // not every group is necessarily represented!
    val xGroup = activeXGroups(x)
    val yGroup = activeYGroups(y)
    value(xGroup, yGroup)
  }

  /**
   * The value at the intersection of two groups.
   * @param x
   * @param y
   * @return
   */
  def value(x: Filter, y: Filter): Double = {
    // not every group is necessarily represented!
    if (data.isDefinedAt(x) && data(x).isDefinedAt(y)) data(x)(y) else 0
  }

}

abstract class CategorizedData(ctx: QvContext) {

  private val Other: Filter = EmptyFilter("Other")
  private val Ambiguous: Filter = EmptyFilter("Ambiguous")

  val observations: Set[Obs]
  val categories: Seq[Filter]

  lazy val hasOthers: Boolean = sets.keys.exists({
    case Filter.Other(_) => true
    case _ => false
  })
  lazy val hasAmbiguous: Boolean = sets.keys.exists({
    case Filter.Ambiguous(_) => true
    case _ => false
  })

  protected lazy val sets: Map[Filter, Set[Obs]] = groupBy(observations, categories)
  lazy val activeGroups: Seq[Filter] = sets.keys.toList.sorted


  // -- internal helpers for grouping
  protected def groupBy(observations: Set[Obs], categories: Seq[Filter]): Map[Filter, Set[Obs]] = {
    val p0 = observations groupBy(findGroup(categories, _))
    val p1 = if (p0.contains(Other)) p0.updated(Filter.Other(p0(Other)), p0(Other)) else p0
    val p2 = if (p1.contains(Ambiguous)) p1.updated(Filter.Ambiguous(p1(Ambiguous)), p1(Ambiguous)) else p1
    p2 - Other - Ambiguous
  }

  private def findGroup(categories: Seq[Filter], o: Obs): Filter = {
    val matchingGroups = categories.filter(_.predicate(o, ctx))
    matchingGroups.size match {
      case 0 => Other                   // no group covers this observation
      case 1 => matchingGroups.head     // exactly one group covers this observation
      case _ => Ambiguous               // more than one group covers this observation
    }
  }

}

abstract class CategorizedYData(ctx: QvContext) extends CategorizedData(ctx: QvContext) {

  val categories = yCategories
  val yCategories: Seq[Filter]

  protected lazy val ySets: Map[Filter, Set[Obs]] = sets

  lazy val activeYGroups: Seq[Filter] = activeGroups

  def observationsFor(y: Filter): Set[Obs] = if (ySets.contains(y)) ySets(y) else Set()


}

/**
 * Categorize a bunch of observations along the x and y axis grouped by the given filters.
 * All observations are sorted into their corresponding groups and the value of the calculation function
 * is calculated for each of those sets.
 */
abstract class CategorizedXYData(ctx: QvContext) extends CategorizedYData(ctx: QvContext) {

  val xCategories: Seq[Filter]

  /**
   * The observations grouped by the groups along the x and y axis.
   */
  protected lazy val xySets: Map[Filter, Map[Filter, Set[Obs]]] = {
    // group observations by x categories
    val byX = groupBy(observations, xCategories)
    // now group all observations per x by y categories
    byX.mapValues(obs => groupBy(obs, yCategories))
  }

  /** The categories along x that can be found in the data.
    * In contrast to the groups from the xAxis this set will only contain the groups that really exist. */
  lazy val activeXGroups: Seq[Filter] = xySets.keys.toList.sorted

  /** The categories along y that can be found in the data.
    * In contrast to the groups from the yAxis this set will only contain the groups that really exist. */
  override lazy val activeYGroups: Seq[Filter] = xySets.map({case (key, map) => map.keySet}).flatten.toSet.toList.sorted
  override lazy val activeGroups = activeYGroups

  /**
   * The set of observations at the intersection of two groups.
   * @param x
   * @param y
   * @return
   */
  def observations(x: Filter, y: Filter): Set[Obs] = {
    // not every group is necessarily represented!
    if (xySets.isDefinedAt(x) && xySets(x).isDefinedAt(y)) xySets(x)(y) else Set()
  }

  def observations(x: Int, y: Int): Set[Obs] = {
    val xGroup = activeXGroups(x)
    val yGroup = activeYGroups(y)
    observations(xGroup, yGroup)
  }

}

