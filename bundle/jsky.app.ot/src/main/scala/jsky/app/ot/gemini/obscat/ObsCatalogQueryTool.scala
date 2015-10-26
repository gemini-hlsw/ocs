package jsky.app.ot.gemini.obscat

import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.catalog.Catalog
import jsky.catalog.gui.CatalogQueryPanel
import jsky.catalog.gui.CatalogQueryTool
import jsky.catalog.gui.QueryResultDisplay
import jsky.util.Preferences
import javax.swing._

import scala.swing.{Component, CheckBox}
import scala.swing.event.ButtonClicked

/**
  * Defines the user interface for querying an ObsCatalog.
  * @param catalog the catalog, for which a user interface component is being generated
  * @param display object used to display the results of a query.
  */
final class ObsCatalogQueryTool(catalog: Catalog, display: QueryResultDisplay) extends CatalogQueryTool(catalog, display, false) {
  private val PREF_KEY: String = classOf[ObsCatalogQueryTool].getName
  private var remote: CheckBox = _
  
  /** Make and return the catalog query panel */
  protected override def makeCatalogQueryPanel(catalog: Catalog): CatalogQueryPanel =
    new ObsCatalogQueryPanel(catalog, 6)

  protected override def makeButtonPanel: JPanel = {
    remote = new CheckBox("Include Remote Programs") {
      tooltip = "Check to include programs in the remote database in query results."
      selected = Preferences.get(PREF_KEY + ".remote", true)

      reactions += {
        case ButtonClicked(_) =>
          Preferences.set(PREF_KEY + ".remote", selected)
      }
    }

    val panel = new MigPanel(LC().fill().insets(0)) {
      add(Component.wrap(makeSaveAsButton()), CC().alignX(RightAlign))
      add(remote, CC().alignX(RightAlign).pushX())
      add(Component.wrap(ObsCatalogQueryTool.super.makeButtonPanel()), CC().alignX(RightAlign))
    }
    panel.peer
  }

  def includeRemote: Boolean = remote.selected
}