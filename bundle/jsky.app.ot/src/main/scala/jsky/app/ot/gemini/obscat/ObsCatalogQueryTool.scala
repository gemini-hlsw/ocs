package jsky.app.ot.gemini.obscat

import java.io.File

import edu.gemini.catalog.ui.PreferredSizeFrame
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.catalog.{FieldDescAdapter, Catalog}
import jsky.catalog.gui.QueryResultDisplay
import jsky.catalog.skycat.SkycatConfigFile
import jsky.util.Preferences

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.event.ButtonClicked

object ObsCatalogFrame extends Frame with PreferredSizeFrame {
  val instance = this

  title = "Gemini Science Program Database"

  val cqt = new ObsCatalogQueryTool(ObsCatalog.INSTANCE, null)
  contents = new MigPanel(LC().insets(0).fill().minWidth(1200.px)) {
    add(Component.wrap(cqt.queryPanel), CC().alignY(TopAlign).grow())
    add(cqt.makeButtonPanel, CC().newline().growX())
    add(Component.wrap(cqt.queryResults), CC().newline().growX())
  }
  adjustSize(true)
}

/**
  * Defines the user interface for querying an ObsCatalog.
  * @param catalog the catalog, for which a user interface component is being generated
  * @param display object used to display the results of a query.
  */
final class ObsCatalogQueryTool(catalog: Catalog, display: QueryResultDisplay) /*extends CatalogQueryTool(catalog, display, false)*/ {
  private val PREF_KEY: String = classOf[ObsCatalogQueryTool].getName
  val queryPanel = new ObsCatalogQueryPanel(ObsCatalog.INSTANCE, 6)
  val queryResults = new ObsCatalogQueryResultDisplay(new ObsCatalogQueryResult(ObsCatalog.INSTANCE.getConfigEntry, new java.util.Vector(), new java.util.Vector(), new java.util.ArrayList(), Array[FieldDescAdapter]()))
  private var remote: CheckBox = _

  val saveAsButton =
    new Button("Save As...") {

    }

  def makeButtonPanel: Component = {
    remote = new CheckBox("Include Remote Programs") {
      tooltip = "Check to include programs in the remote database in query results."
      selected = Preferences.get(PREF_KEY + ".remote", true)

      reactions += {
        case ButtonClicked(_) =>
          Preferences.set(PREF_KEY + ".remote", selected)
      }
    }

    val panel = new MigPanel(LC().fill().insets(0)) {
      add(saveAsButton, CC().alignX(RightAlign))
      add(remote, CC().alignX(RightAlign).pushX())
      add(makeQueryButton, CC().alignX(RightAlign))
    }
    panel
  }

  def includeRemote: Boolean = remote.selected

  /**
    * Make and return the button panel
    */
  protected def makeQueryButton: Button = {
    new Button("Query") {
      tooltip = "Start the Query"
      reactions += {
        case ButtonClicked(_) =>
          Future.apply(ObsCatalogHelper.query(queryPanel.getQueryArgs, ObsCatalog.newConfigEntry(), remote.selected)).onSuccess {
            case r =>
              queryResults.setQueryResult(r)
              //new ObsCatalogQueryResultDisplay(r)
          }
      }
    }
  }
}
