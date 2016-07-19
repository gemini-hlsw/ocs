package jsky.app.ot.gemini.obscat

import java.io.File
import javax.swing.{DefaultComboBoxModel, ImageIcon, JComponent, JTextField}

import edu.gemini.catalog.ui.PreferredSizeFrame
import edu.gemini.shared.gui.textComponent.TextRenderer
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import edu.gemini.shared.util.immutable.ScalaConverters._
import jsky.app.ot.userprefs.ui.{PreferenceDialog, PreferencePanel}
import jsky.app.ot.util.Resources
import jsky.catalog.{Catalog, FieldDescAdapter}
import jsky.util.Preferences
import jsky.util.gui.{DialogUtil, MultiSelectComboBox}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.event.{ButtonClicked, SelectionChanged}
import scalaz._
import Scalaz._

// NOTE Serialization is totally screwed, I need to make the types explicit for it to work
// TODO Port this to a sane system, e.g. Json serialization
case class OTCatalogSelection(selection: java.util.HashMap[String, java.io.Serializable]) extends Serializable

case class OTBrowserInstrumentSelection(selection: Option[java.util.HashMap[String, java.util.HashMap[String, java.io.Serializable]]]) extends Serializable

case class OTBrowserPreset(name: String, includeRemote: Boolean, catalog: OTCatalogSelection, instruments: OTBrowserInstrumentSelection) extends Serializable

case class OTBrowserConf(selected: String, presets: List[OTBrowserPreset]) extends Serializable

protected object OTBrowserPresetChoice {

  sealed trait ObsQueryPreset {
    def name: String
    def hasSettings: Boolean = false
  }

  case object SaveNewPreset extends ObsQueryPreset {
    val name = "Save New Preset..."
  }

  case class SavedPreset(preset: OTBrowserPreset) extends ObsQueryPreset {
    val name = preset.name
    override def hasSettings = true
  }

  case class DeletePreset(preset: SavedPreset) extends ObsQueryPreset {
    val name = s"""Delete Preset '${preset.name}'"""
  }

  case class SaveExistingPreset(preset: SavedPreset) extends ObsQueryPreset {
    val name = s"""Save Preset '${preset.name}'"""
  }

}

object ObsCatalogFrame extends Frame with PreferredSizeFrame {
  val instance = this

  def loadPresets(conf: OTBrowserConf):Unit = Swing.onEDT(cqt.loadPreset(conf))

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
    OTCatalogSelection(new java.util.HashMap[String, java.io.Serializable](s.toMap.asJava))
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
        (instrument, new java.util.HashMap[String, java.io.Serializable](p.toMap.asJava))
      }.toMap
    }
    // types need to be explicit
    OTBrowserInstrumentSelection(values.map(k => new java.util.HashMap[String, java.util.HashMap[String, java.io.Serializable]](k.asJava)))
  }

  def selectionPreset(name: String, includeRemote: Boolean): OTBrowserPreset = OTBrowserPreset(name, includeRemote, catalogSelection, instrumentSelection)

  def restorePreset(preset: OTBrowserPreset): Unit = {
    preset.catalog.selection.asScala.foreach(Function.tupled(setValue))

    preset.instruments.selection.foreach { m =>
      m.asScala.foreach {
        case (inst, s) =>
          s.asScala.foreach { v =>
            Option(getInstComponentForLabel(inst, v._1)).foreach { setValue(_, v._2) }
          }
      }
    }
  }

  def reset(): Unit = {
    val resetFunction:PartialFunction[JComponent, Unit] = {
      case m: MultiSelectComboBox[_] => m.setSelectedObjects(Array())
      case t: JTextField             => t.setText("")
      case _                         => // Do nothing
    }
    // Sometimes the values come null :S
    _panelComponents.toList.filter(_ != null).foreach(c => c.toList.filter(_ != null).foreach(resetFunction))
    _components.toList.filter(_ != null).foreach(resetFunction)
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
      case SelectionChanged(_)                                    =>
        selection.item match {
          case SaveNewPreset       =>
            val name = DialogUtil.input(ObsCatalogFrame.instance.peer, "Enter a name for this query")
            val previousModel = this.peer.getModel
            val existingPresets = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.hasSettings)
            val existingNames = existingPresets.map(_.name)

            Option(name).filter(_.nonEmpty).filterNot(existingNames.contains).foreach { n =>
              val preset = SavedPreset(queryPanel.selectionPreset(name, remote.selected))
              val model = new DefaultComboBoxModel[ObsQueryPreset]((preset :: existingPresets.toList ::: List(SaveNewPreset, SaveExistingPreset(preset), DeletePreset(preset))).toArray)
              model.setSelectedItem(preset)
              this.peer.setModel(model)
              // Optimistically assume the save works ok
              OTBrowserPresetsPersistence.saveAsync(presetsToSave(model))(implicitly)
            }
            existingNames.find(_ == name).foreach { n =>
              DialogUtil.error(s"Name '$n' already in used")
            }
          case DeletePreset(preset) =>
            val previousModel = this.peer.getModel
            val existingElements = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.hasSettings).filterNot(_.name == preset.name)
            val head = existingElements.headOption.collect {
              case s @ SavedPreset(_) => List(SaveExistingPreset(s), DeletePreset(s))
            }
            val model = new DefaultComboBoxModel[ObsQueryPreset]((existingElements.toList ::: (SaveNewPreset :: ~head)).toArray)
            existingElements.headOption.foreach(model.setSelectedItem)
            // Optimistically assume the save works ok
            OTBrowserPresetsPersistence.saveAsync(presetsToSave(model))(implicitly)
            this.peer.setModel(model)
          case SaveExistingPreset(preset) =>
            val updatedPreset = SavedPreset(queryPanel.selectionPreset(preset.name, remote.selected))
            val previousModel = this.peer.getModel
            val existingElements = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.hasSettings).collect {
              case s @ SavedPreset(p) if p.name == preset.name => updatedPreset
              case q: ObsQueryPreset                           => q
            }
            val model = new DefaultComboBoxModel[ObsQueryPreset]((existingElements.toList ::: List(SaveNewPreset, SaveExistingPreset(updatedPreset), DeletePreset(updatedPreset))).toArray)
            this.peer.setModel(model)
            this.selection.item = updatedPreset
            // Save the updated preset
            OTBrowserPresetsPersistence.saveAsync(presetsToSave(model))(implicitly)
          case s @ SavedPreset(preset) =>
            // Update the model
            val previousModel = this.peer.getModel
            val existingElements = (0 until previousModel.getSize).map(previousModel.getElementAt).filter(_.hasSettings)
            val model = new DefaultComboBoxModel[ObsQueryPreset]((existingElements.toList ::: List(SaveNewPreset, SaveExistingPreset(s), DeletePreset(s))).toArray)
            OTBrowserPresetsPersistence.saveAsync(presetsToSave(model))(implicitly)
            this.peer.setModel(model)
            this.selection.item = s
            remote.selected = preset.includeRemote
            queryPanel.restorePreset(preset)
            doQuery()
          case _                   => // Should not happen
        }
    }
  }

  def presetsToSave(model: DefaultComboBoxModel[ObsQueryPreset]): OTBrowserConf =
    OTBrowserConf(model.getSelectedItem.asInstanceOf[ObsQueryPreset].name, (0 until model.getSize).map(model.getElementAt).collect { case q: SavedPreset => q.preset}.toList)

  def loadPreset(conf: OTBrowserConf):Unit = {
    val selected = conf.presets.find(_.name == conf.selected).map(SavedPreset.apply)
    val extras = SaveNewPreset :: ~selected.map(p => List(SaveExistingPreset(p), DeletePreset(p)))
    val sp:List[ObsQueryPreset] = conf.presets.map(SavedPreset.apply)
    val model = new DefaultComboBoxModel[ObsQueryPreset]((sp ::: extras).toArray)
    selected.foreach { s =>
      model.setSelectedItem(s)
      queryPanel.restorePreset(s.preset)
      remote.selected = conf.presets.find(_.name == conf.selected).exists(_.includeRemote)
    }
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

  val resetButton = new Button("Reset") {
    tooltip = "Reset all the selected values"

    reactions += {
      case ButtonClicked(_) =>
        queryPanel.reset()
    }
  }

  val queryButton: Button = {
    new Button("Query") {
      tooltip = "Start the Query"
      reactions += {
        case ButtonClicked(_) =>
          doQuery()
      }
    }
  }

  protected def doQuery(): Unit = {
    ObsCatalogHelper.query(queryPanel.getQueryArgs, ObsCatalog.newConfigEntry(), remote.selected, r => Swing.onEDT(queryResults.setQueryResult(r)))
  }

  val buttonPanel: Component = new MigPanel(LC().fill().insets(0)) {
      add(toolsButton, CC().alignX(RightAlign))
      add(presetsCB, CC().alignX(RightAlign).growY())
      add(resetButton, CC().alignX(RightAlign).growY())
      add(remote, CC().alignX(RightAlign).pushX())
      add(queryButton, CC().alignX(RightAlign))
    }

}
