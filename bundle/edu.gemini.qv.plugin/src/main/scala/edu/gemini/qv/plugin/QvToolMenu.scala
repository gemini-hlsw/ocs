package edu.gemini.qv.plugin

import edu.gemini.qv.plugin.data.DataSource
import edu.gemini.qv.plugin.QvToolMenu.RefreshAction
import edu.gemini.qv.plugin.selector.RefreshDialog
import edu.gemini.qv.plugin.util.SolutionProvider
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing._
import edu.gemini.qv.plugin.ui.QvGui


object QvToolMenu {

  private object RefreshAction {
    // keep track of running refresh operations
    val refreshInProgress = new AtomicBoolean(false)
  }

  /**
   * Action for executing a full data refresh in the background. This action takes care that only one
   * refresh operation is running at a time.
   * @param label
   * @param ctx
   * @param prepare
   */
  class RefreshAction(label: String, ctx: QvContext, prepare: () => Unit = ()=>{}) extends Action(label) {
    def apply() = {

      // allow only one refresh at a time
      if (!RefreshAction.refreshInProgress.compareAndSet(false, true)) {

        Dialog.showMessage(
          messageType=Dialog.Message.Info,
          title="Data Update In Progress.",
          message=
            """There is already an update in progress.
              |Please wait for current refresh to finish.
            """.stripMargin)

      } else {

        executeRefresh.

          // whatever the outcome clear refresh in progress boolean
          andThen {
            case _ => RefreshAction.refreshInProgress.set(false)
          }.

          // in case of an error show an error message
          onFailure {
            case t =>
              QvGui.showError(
                "Loading Data Failed.",
                "Could not load data.", t
              )
        }

      }

    }

    /**
     * Executes the refresh by composing all the different actions that are needed to do so.
     * This includes reloading the data from the ODB and then recalculating all constraints.
     * @return
     */
    private def executeRefresh: Future[Unit] = {

      prepare()
      SolutionProvider(ctx).clear()

      for {
        tup <- ctx.dataSource.refresh                     // wait for new observations loaded
        _   <- SolutionProvider(ctx).update(ctx, tup._1)  // wait for all constraints updated
      } yield ()

    }

  }
}

/**
 */
class QvToolMenu(frame: Frame, ctx: QvContext) extends MenuBar {

  private val reloadAction = Action("Change Data and Reload") {
    new RefreshDialog(frame, ctx).open()
  }

  private val refreshAction = new RefreshAction("Refresh", ctx)

  private def exportFilterAction(c: Component) = Action("Export Filter...") { new StoreExporter(c, QvStore.filters) }
  private def exportAxisAction(c: Component) = Action("Export Axis...") { new StoreExporter(c, QvStore.axes) }
  private def exportHistogramAction(c: Component) = Action("Export Histogram...") { new StoreExporter(c, QvStore.histograms) }
  private def exportTableAction(c: Component) = Action("Export Table...") { new StoreExporter(c, QvStore.tables) }
  private def exportBarChartAction(c: Component) = Action("Export Bar Chart...") { new StoreExporter(c, QvStore.visCharts) }
  private def importAction(c: Component) = Action("Import...") { new StoreImporter(c) }

  // === the data menu
  val dataMenu = new Menu("Data") {
    contents += new MenuItem(reloadAction)
    contents += new MenuItem(refreshAction)
  }

  // === the share menu
  val shareMenu = new Menu("Share") {
    contents += new MenuItem("") { action = exportFilterAction(this) }
    contents += new MenuItem("") { action = exportAxisAction(this) }
    contents += new MenuItem("") { action = exportHistogramAction(this) }
    contents += new MenuItem("") { action = exportTableAction(this) }
    contents += new MenuItem("") { action = exportBarChartAction(this) }
    contents += new MenuItem("") { action = importAction(this) }
  }

  // add all menus to the main menu bar
  contents += dataMenu
  contents += shareMenu

}