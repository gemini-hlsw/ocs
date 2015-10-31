package jsky.app.ot.gemini.obscat

import java.io.File
import javax.swing.{DefaultComboBoxModel, ImageIcon}

import edu.gemini.catalog.ui.PreferredSizeFrame
import edu.gemini.shared.gui.textComponent.TextRenderer
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import edu.gemini.shared.util.immutable.ScalaConverters._
import jsky.app.ot.userprefs.ui.{PreferencePanel, PreferenceDialog}
import jsky.catalog.{FieldDescAdapter, Catalog}
import jsky.util.Preferences
import jsky.util.gui.DialogUtil

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.event.{SelectionChanged, ButtonClicked}

import scalaz._
import Scalaz._

case class OTCatalogSelection(selection: Map[String, java.io.Serializable]) extends Serializable

case class OTBrowserInstrumentSelection(selection: Option[Map[String, Map[String, java.io.Serializable]]]) extends Serializable

case class OTBrowserPreset(name: String, catalog: OTCatalogSelection, instruments: OTBrowserInstrumentSelection) extends Serializable

protected object OTBrowserPresetChoice {

  sealed trait ObsQueryPreset {
    def name: String
    def isPreset: Boolean = false
  }

  case object SaveNewPreset extends ObsQueryPreset {
    val name = "Save New Preset..."
  }

  case class SavedPreset(preset: OTBrowserPreset) extends ObsQueryPreset {
    val name = preset.name
    override def isPreset = true
  }

  case class DeletePreset(preset: SavedPreset) extends ObsQueryPreset {
    val name = s"""Delete Preset '${preset.name}'"""
  }

}

object ObsCatalogFrame extends Frame with PreferredSizeFrame {
  val instance = this

  def loadPresets(presets: List[OTBrowserPresetChoice.ObsQueryPreset]):Unit = Swing.onEDT(cqt.loadPreset(presets))

  title = "Gemini Science Program Database"

  lazy val cqt = new ObsCatalogQueryTool(ObsCatalog.INSTANCE)
  contents = new MigPanel(LC().insets(0).fill().minWidth(1200.px)) {
    add(Component.wrap(cqt.queryPanel), CC().alignY(TopAlign).growX())
    add(cqt.buttonPanel, CC().newline().growX().gap(10.px, 10.px, 10.px, 10.px))
    add(Component.wrap(cqt.queryResults), CC().newline().growX().growY().pushY())
  }
  adjustSize(true)
}


class OTBrowserQueryPanel(catalog: Catalog) extends ObsCatalogQueryPanel(catalog, 6) {
  private def catalogSelection: OTCatalogSelection = {
    val n = Math.min(_components.length, _catalog.getNumParams)
    val s = (0 until n).map(i => (i, Option(_components(i)))).map {
      case (i, c) =>
        (_catalog.getParamDesc(i).getName, Option(getValue(i)).getOrElse(""))
    }
    OTCatalogSelection(s.toMap)
  }

  private def instrumentSelection: OTBrowserInstrumentSelection = {
    val instIndexes = Option(_getInstIndexes).map(_.toList)
    val instruments = Option(_getInstruments).map(_.toList)
    val values = (instIndexes |@| instruments) { (idx, inst) =>
      inst.zipWithIndex.map { case (instrument, i) =>
        val params = ObsCatalog.getInstrumentParamDesc(instrument)
        val p = params.toList.zipWithIndex.map { case (param, j) =>
          (param.getName, getValue(param, _panelComponents(idx(i) + 1)(j)))
        }
        (instrument, p.toMap)
      }.toMap
    }
    OTBrowserInstrumentSelection(values)
  }

  def selectionPreset(name: String): OTBrowserPreset = OTBrowserPreset(name, catalogSelection, instrumentSelection)

  def restorePreset(preset: OTBrowserPreset): Unit = {
    preset.catalog.selection.foreach(Function.tupled(setValue))

    preset.instruments.selection.foreach { m =>
      m.foreach {
        case (inst, s) =>
          s.foreach { v =>
            Option(getInstComponentForLabel(inst, v._1)).foreach { setValue(_, v._2) }
          }
      }
    }
  }
}

/**
  * Defines the user interface for querying an ObsCatalog.
  * @param catalog the catalog, for which a user interface component is being generated
  */
final class ObsCatalogQueryTool(catalog: Catalog) {
  import OTBrowserPresetChoice._

  val PREF_KEY = classOf[ObsCatalogQueryTool].getName

  val queryPanel = new OTBrowserQueryPanel(catalog)
  val queryResults = new ObsCatalogQueryResultDisplay(new ObsCatalogQueryResult(ObsCatalog.INSTANCE.getConfigEntry, new java.util.Vector(), new java.util.Vector(), new java.util.ArrayList(), Array[FieldDescAdapter]()))
  val remote = new CheckBox("Include Remote Programs") {
        tooltip = "Check to include programs in the remote database in query results."
        selected = Preferences.get(PREF_KEY + ".remote", true)

        reactions += {
          case ButtonClicked(_) =>
            Preferences.set(PREF_KEY + ".remote", selected)
        }
      }

  val presetsCB = new ComboBox[ObsQueryPreset](List(SaveNewPreset)) with TextRenderer[ObsQueryPreset] {
    override def text(a: ObsQueryPreset): String = ~Option(a).map(_.name)

    listenTo(selection)
    reactions += {
      case SelectionChanged(_) if selection.item == SaveNewPreset =>
        val name = DialogUtil.input(ObsCatalogFrame.instance.peer, "Enter a name for this query")
        val previousModel = this.peer.getModel
        val existingPresets = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.isPreset)
        val existingNames = existingPresets.map(_.name)

        Option(name).filter(_.nonEmpty).filterNot(existingNames.contains).foreach { n =>
          val preset = SavedPreset(queryPanel.selectionPreset(name))
          val model = new DefaultComboBoxModel[ObsQueryPreset]((preset :: existingPresets.toList ::: List(SaveNewPreset, DeletePreset(preset))).toArray)
          model.setSelectedItem(preset)
          this.peer.setModel(model)
          // Optimistically assume the save worked ok
          OTBrowserPresets.saveAsync(presetsToSave(model))
        }
        existingNames.find(_ == name).foreach { n =>
          DialogUtil.error(s"Name '$n' already in used")
        }
      case SelectionChanged(_)                                    =>
        selection.item match {
          case DeletePreset(preset) =>
            val previousModel = this.peer.getModel
            val existingElements = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.isPreset).filterNot(_.name == preset.name)
            val head = existingElements.headOption.collect {
              case s @ SavedPreset(_) => DeletePreset(s)
            }
            val model = new DefaultComboBoxModel[ObsQueryPreset]((existingElements.toList ::: (SaveNewPreset :: head.toList)).toArray)
            head.foreach(model.setSelectedItem)
            this.peer.setModel(model)
            // Optimistically assume the save worked ok
            OTBrowserPresets.saveAsync(presetsToSave(model))
          case SavedPreset(preset) =>
            queryPanel.restorePreset(preset)
          case _                   => // Should not happen
        }
    }
  }

  def presetsToSave(model: DefaultComboBoxModel[ObsQueryPreset]): List[ObsQueryPreset] =
    (0 until model.getSize).map(model.getElementAt).filter(_.isPreset).toList

  def loadPreset(presets: List[ObsQueryPreset]):Unit = {
    val selected = presets.headOption.collect {
      case s: SavedPreset => s
    }
    val extras = SaveNewPreset :: ~selected.map(p => List(DeletePreset(p)))
    val model = new DefaultComboBoxModel[ObsQueryPreset]((presets ::: extras).toArray)
    selected.foreach(model.setSelectedItem)
    presetsCB.peer.setModel(model)
  }

  val toolsButton = new Button("") {
    tooltip = "Preferences..."
    icon = new ImageIcon(getClass.getResource("/resources/images/eclipse/engineering.gif"))

    reactions += {
      case ButtonClicked(_) =>
        val dialog = new PreferenceDialog(List[PreferencePanel](BrowserPreferencesPanel.instance).asImList)
        dialog.show(ObsCatalogFrame.instance.peer, BrowserPreferencesPanel.instance)
    }
  }

  val queryButton: Button = {
    new Button("Query") {
      tooltip = "Start the Query"
      reactions += {
        case ButtonClicked(_) =>
          Future.apply(ObsCatalogHelper.query(queryPanel.getQueryArgs, ObsCatalog.newConfigEntry(), remote.selected)).onSuccess {
            case r =>
              queryResults.setQueryResult(r)
          }
      }
    }
  }

  val buttonPanel: Component = new MigPanel(LC().fill().insets(0)) {
      add(toolsButton, CC().alignX(RightAlign))
      add(presetsCB, CC().alignX(RightAlign).growY())
      add(remote, CC().alignX(RightAlign).pushX())
      add(queryButton, CC().alignX(RightAlign))
    }

}
