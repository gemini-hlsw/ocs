package jsky.app.ot.tpe.feat

import edu.gemini.ags.api.{AgsRegistrar, AgsHash}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api.ConeSearchCatalogQuery
import edu.gemini.catalog.ui.adapters.TableQueryResultAdapter
import edu.gemini.catalog.ui.{ObservationInfo, QueryResultsFrame, TargetsModel}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.obs.context.ObsContext
import jsky.app.ot.tpe._
import jsky.app.ot.tpe.feat.TpeCatalogFeature.PlotState._
import jsky.catalog.gui.TablePlotter
import java.awt.Graphics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing

/**
 * The TpeCatalogFeature is used to show guide star candidates.  When enabled,
 * it has two modes.  Normally AGS candidates are displayed but when the manual
 * catalog search tool is visible then it plots the results from there.  The
 * actual drawing of the plot symbols is delegated to the catalog code.
 */
final class TpeCatalogFeature extends TpeImageFeature("Catalog", "Show or hide catalog symbols.") {
  import TpeCatalogFeature._

  override def isEnabledByDefault: Boolean =
    true

  override def getCategory: TpeImageFeatureCategory =
    TpeImageFeatureCategory.target

  override def unloaded(): Unit = {
    super.unloaded()
    draw()
  }

  override def draw(g: Graphics, t: TpeImageInfo): Unit =
    draw()

  private def draw(): Unit =
    for {
      w <- Option(_iw)
      p <- Option(w.plotter)
    } draw(w, p)

  // Sketchy: plotting guide stars triggers a repaint, which results in the
  // TpeCatalogFeature being asked to draw again.  Since draw calls plot which
  // triggers repaint which calls draw, something has to break the loop.  That's
  // where PlotState comes in.  It can be computed and compared to determine
  // whether what would be drawn differs from the previous instance.
  private var state: PlotState = NoPlot

  private def draw(w: TpeImageWidget, p: TablePlotter): Unit = {
    val newState = plotState(w)
    if (state != newState) {
      state = newState

      p.unplotAll()

      newState match {
        case NoPlot        => // do nothing
        case ManualPlot(_) => QueryResultsFrame.plotResults()
        case GemsPlot(_)   => w.getGemsGuideStarSearchDialog.asScalaOpt.foreach(_.plot());
        case AgsPlot(_)    => showAgsCandidates(w, p, newState)
      }
    }
  }

  // Displays the candidates considered by AGS.  This requires performing an
  // asynchronous query and packaging up the results so that they appear to be
  // from the catalog ui.
  private def showAgsCandidates(
    w: TpeImageWidget,
    p: TablePlotter,
    state: PlotState
  ): Unit =
    for {
      c <- w.getObsContext.asScalaOpt
      s <- AgsRegistrar.currentStrategy(c)
      m  = ProbeLimitsTable.loadOrThrow()
      q <- s.catalogQueries(c, m).headOption.collect { case cs: ConeSearchCatalogQuery => cs }
    } s.candidates(c, m)(global).map(_.flatMap(_._2)).onSuccess { case ts =>
      Swing.onEDT {
        // assuming the state of the world is the same after the candidate
        // search, plot the candidates
        if (state == plotState(w)) {
          val in = ObservationInfo(c, m)
          val tm = TargetsModel(Some(in), q.base, q.radiusConstraint, ts)
          p.plot(TableQueryResultAdapter(tm))
        }
      }
    }

  private def plotState(iw: TpeImageWidget): PlotState =
    if (!isVisible) NoPlot
    else if (QueryResultsFrame.visible) ManualPlot(QueryResultsFrame.targetsModel)
    else iw.getGemsGuideStarSearchDialog.asScalaOpt.filter(_.isVisible) match {
      case None    => iw.getObsContext.asScalaOpt.fold(NoPlot: PlotState)(c => AgsPlot(agsHash(c)))
      case Some(d) => GemsPlot(d.getModelVersion)
    }
}

object TpeCatalogFeature {

  private def agsHash(c: ObsContext): Int = {
    // Need a reasonable stable value for when to compute coordinates for an
    // observation, but which precise time (and hence the site) doesn't matter
    val when = c.getSchedulingBlockStart.asScalaOpt.map(_.toLong).getOrElse {
      new ObservingNight(c.getSite.getOrElse(Site.GN)).getStartTime
    }
    AgsHash.hash(c, when)
  }

  /** PlotState contains all relevant information for the catalog plot. */
  sealed trait PlotState extends Product with Serializable

  object PlotState {
    case object NoPlot extends PlotState
    final case class AgsPlot(hash: Int) extends PlotState
    final case class GemsPlot(cnt: Int) extends PlotState
    final case class ManualPlot(targets: Option[TargetsModel]) extends PlotState
  }
}
