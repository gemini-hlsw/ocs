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
import jsky.catalog.gui.TablePlotter
import java.awt._

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
  private var state: Option[PlotState] = None

  private def draw(w: TpeImageWidget, p: TablePlotter): Unit = {
    val newState = plotState(w)
    if (state != newState) {
      state = newState

      p.unplotAll()
      if (state.exists(_.visible)) {
        if (state.exists(_.manual)) QueryResultsFrame.plotResults()
        else showAgsCandidates(w, p, newState)
      }
    }
  }

  // Displays the candidates considered by AGS.  This requires performing an
  // asynchronous query and packaging up the results so that they appear to be
  // from the catalog ui.
  private def showAgsCandidates(
    w: TpeImageWidget,
    p: TablePlotter,
    state: Option[PlotState]
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

  private def plotState(iw: TpeImageWidget): Option[PlotState] =
    iw.getObsContext.asScalaOpt.map { c =>
      PlotState(isVisible, agsHash(c), QueryResultsFrame.visible, QueryResultsFrame.targetsModel)
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
  private final case class PlotState(
    visible: Boolean,
    agsHash: Int,
    manual:  Boolean,
    targets: Option[TargetsModel]
  )

}
