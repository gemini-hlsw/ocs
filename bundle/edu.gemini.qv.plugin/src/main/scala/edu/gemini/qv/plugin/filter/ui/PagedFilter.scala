package edu.gemini.qv.plugin.filter.ui

import edu.gemini.qv.plugin.data.ObservationProvider
import edu.gemini.qv.plugin.filter.core.Filter._
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.qv.plugin.filter.ui.FilterElement._
import scala.swing.TabbedPane.Page
import scala.swing.{Dimension, ScrollPane, TabbedPane}
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.filter.core.ConfigurationFilter
import edu.gemini.qv.plugin.filter.core.EnumFilter
import edu.gemini.qv.plugin.filter.core.EmptyFilter

/**
 * Panel with a set of filter elements on it.
 */
abstract class PagedFilter(ctx: QvContext, init: Set[Filter], showAvailableOnly: Boolean = true, showCounts: Boolean = true) extends TabbedPane {

  // Define which filters should show up on which tabs and in which order.
  // This also defines the default values for each filter, this can be overridden by filters passed in constructor.
  protected def defaultMainFilters: Seq[Filter]
  protected def defaultConditionFilters: Seq[Filter]
  protected def defaultPriorityFilters: Seq[Filter]
  protected def defaultConfigFilters: Set[ConfigurationFilter[_]]

  protected var optTree: OptionsTree = null
  protected var filters: Seq[FilterElement.Combi] = Seq()

  preferredSize = new Dimension(300, 850)

  init(init)

  reactions += {
    case FilterElementChanged => publish(FilterElementChanged2)
  }

  // =====================================================================================

  def init(init: Set[Filter]) = {

    // clear everything
    pages.clear()
    if (optTree != null) deafTo(optTree)
    filters.foreach(deafTo(_))

    // use the label of the init filters for a lookup map
    // this map will be used to replace the default with filters passed in from the outside
    // if applicable; this is to make sure that if filters are added and not contained in the defaults
    // we still display the new filters in the UI, it also allows to only store the filters which are relevant
    // i.e. are different from the default
    val initMap = init.map {
       // adapt context to current context if needed!!
      case SetTime(_, min, max) => SetTime(ctx, min, max)
      case RemainingHours(_, min, max, enabled, thisSemester, nextSemester) => RemainingHours(ctx, min, max, enabled, thisSemester, nextSemester)
      case RemainingNights(_, min, max, enabled, thisSemester, nextSemester) => RemainingNights(ctx, min, max, enabled, thisSemester, nextSemester)
      case RemainingHoursFraction(_, min, max, enabled, thisSemester, nextSemester) => RemainingHoursFraction(ctx, min, max, enabled, thisSemester, nextSemester)
      case f => f
    } .map(f => (f.label, f)).toMap
    // create actual filter elements base on defaults (replace default with init values where applicable)
    val mainElements = defaultMainFilters.map(replaceWithInit(_, initMap)).map(toUI(_, ctx.source))
    val conditionElements = defaultConditionFilters.map(replaceWithInit(_, initMap)).map(toUI(_, ctx.source))
    val priorityElements = defaultPriorityFilters.map(replaceWithInit(_, initMap)).map(toUI(_, ctx.source))
    val configFilters = defaultConfigFilters.map(replaceWithInit(_, initMap)).map(_.asInstanceOf[ConfigurationFilter[_]])
    // list of all panes and their filters
    val panes = Seq(
      ("Main", mainElements),
      ("Priorities", priorityElements),
      ("Conditions", conditionElements)
    )

    optTree = new OptionsTree(ctx, ctx.source, configFilters, showAvailableOnly, showCounts)
    val optTreePane = new ScrollPane { contents = optTree }

    filters = panes.map({case (name, elements) =>
      val filterElem   = new FilterElement.Combi(ctx.source, elements)
      val scrollPane = new ScrollPane { contents = filterElem }
      pages += new Page(name, scrollPane)
      filterElem
    })
    pages += new Page("Configurations", optTreePane)

    filters.foreach(listenTo(_))
    listenTo(optTree)

    publish(FilterElementChanged2)
  }

  def filter = {
    val allFilters = filters.map(_.filter) :+ optTree.filter
    val nonEmptyFilters = allFilters.filter(!_.isEmpty)
    nonEmptyFilters.reduceOption(_.and(_)).getOrElse(new EmptyFilter)
  }

  def activeFilter = {
    val allFilters = filters.map(_.filter).filter(_.isActive) :+ optTree.activeFilter
    allFilters.reduceOption(_.and(_)).getOrElse(new EmptyFilter)
  }

  def filterSet: Set[Filter] = {
    val allFilters = filters.map(_.filter.elements).flatten ++ optTree.filter.elements
    val nonEmptyFilters = allFilters.filter(!_.isEmpty)
    nonEmptyFilters.toSet
  }

  // ==== HELPERS

  // helper to create a UI element based on a filter
  private def toUI(f: Filter, data: ObservationProvider) = f match {
    case f: StringFilter => new Strings(f)
    case f: RemainingTimeFilter => new RemainingTime(f)
    case f: SimpleRangeFilter => new Range(f)
    case f: BooleanFilter => new Booleans(f)
    case f: EnumFilter[_] => new Options(ctx, data, f, showAvailableOnly, showCounts)
  }

  // helper to replace default with init filters, lookup done with label of filter
  private def replaceWithInit(f: Filter, init: Map[String, Filter]) = init.getOrElse(f.label, f)



}