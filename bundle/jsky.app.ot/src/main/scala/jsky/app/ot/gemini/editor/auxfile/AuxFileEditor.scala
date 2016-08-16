package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.gemini.editor.ProgramForm
import jsky.app.ot.vcs.VcsOtClient
import java.text.SimpleDateFormat
import java.util.{Collections, Date, TimeZone}

import jsky.util.gui.Resources

import scala.collection.JavaConverters._
import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.event.{MouseClicked, TableRowsSelected}


object AuxFileEditor {
  private class LabelCellRenderer[T](conf: (Label, T) => Unit) extends Table.AbstractRenderer[T, Label](new Label) {
    override def configure(t: Table, sel: Boolean, foc: Boolean, value: T, row: Int, col: Int) {
      conf(component, value)
    }
  }

  private val SizeCellRenderer = new LabelCellRenderer[Integer]((lab, size) => {
      lab.horizontalAlignment = Alignment.Right
      lab.text = size.toString
  })

  private val Format = new SimpleDateFormat("MM/dd/yy HH:mm:ss") {
    setTimeZone(TimeZone.getTimeZone("UTC"))
  }

  private val DateCellRenderer = new LabelCellRenderer[Date]((lab, date) => {
    lab.horizontalAlignment = Alignment.Center
    lab.text = Format.format(date)
  })

  private val CheckIcon = Resources.getIcon("eclipse/check.gif")

  private val CheckCellRenderer = new LabelCellRenderer[java.lang.Boolean]((lab, checked) => {
    lab.text = ""
    lab.icon = if (checked) CheckIcon else null
  })

  // Action requires a title and button displays the action title.  This is
  // a button with an action but no title.
  private class ImageButton(a: Action) extends Button(a) {
    text      = null
    focusable = false
  }
}

import AuxFileEditor._

class AuxFileEditor(form: ProgramForm) extends BorderPanel {
  val model = new AuxFileModel(VcsOtClient.unsafeGetRegistrar)

  private val addAction    = new AddAction(this, model)
  private val removeAction = new RemoveAction(this, model)
  private val fetchAction  = new FetchAction(this, model)
  private val openAction   = new OpenAction(this, model)
  private val updateAction = new UpdateAction(this, model)

  private val descriptionTextField = new TextField()
  private val describeAction = new DescribeAction(this, model, descriptionTextField)
  descriptionTextField.action = describeAction

  private val checkAction  = new CheckAction(this, model)

  form.tabbedPane.insertTab("File Attachment", null, peer, "View/Edit file attachments", 0)
  form.tabbedPane.setSelectedIndex(0)

  def update(pid: SPProgramID) {
    model.init(pid)

    // this is essentially updateAction.apply() w/o any popups on error
    AuxFileAction.silentUpdate(model)
  }

  layoutManager.setVgap(5)

  val attachmentTable = new Table() {
    model = new AttachmentTableModel(Collections.emptyList())
    autoResizeMode = Table.AutoResizeMode.LastColumn
    peer.getTableHeader.setReorderingAllowed(false)

    override def rendererComponent(sel: Boolean, foc: Boolean, row: Int, col: Int): Component =
      (col, model.getValueAt(row, col)) match {
        case (c, size: Integer) if c == AttachmentTableModel.Col.SIZE.ordinal =>
          SizeCellRenderer.componentFor(this, sel, foc, size, row, col)
        case (c, date: Date) if c == AttachmentTableModel.Col.LAST_MOD.ordinal =>
          DateCellRenderer.componentFor(this, sel, foc, date, row, col)
        case (c, check: java.lang.Boolean) if c == AttachmentTableModel.Col.CHECKED.ordinal =>
          CheckCellRenderer.componentFor(this, sel, foc, check, row, col)
        case _ => super.rendererComponent(sel, foc, row, col)
      }
  }

  layout(new ScrollPane(attachmentTable)) = Center

  class ControlPanel extends BorderPanel {
    layoutManager.setHgap(10)

    layout(new GridPanel(1, 0) {
      hGap = 5
      contents += new ImageButton(addAction)
      contents += new ImageButton(removeAction)
      contents += new ImageButton(fetchAction)
      contents += new ImageButton(openAction)
      contents += new ImageButton(updateAction)
    }) = West

    layout(new BorderPanel() {
      layoutManager.setHgap(5)
      layout(descriptionTextField) = Center
      layout(new Button(describeAction)) = East
    }) = Center

    layout(new Button(checkAction)) = East
  }

  layout(new ControlPanel) = South

  // Gets the list of AuxFiles corresponding to selected table rows.
  private def selectedFiles(files: List[AuxFile]): List[AuxFile] =
    files.zipWithIndex collect { case (f, i) if attachmentTable.selection.rows.contains(i) => f }

  // Gets the set of tables row indices corresponding to selected aux files.
  private def selectedIndices(all: List[AuxFile], sel: List[AuxFile]): Set[Int] =
    (Set.empty[Int]/:sel) { (s, f) => s + all.indexOf(f) }

  // If all given files contain the same description, return it otherwise ""
  private def commonDescription(files: List[AuxFile]): String =
    files.map(f => Option(f.getDescription).getOrElse("")).distinct match {
      case List(d) => d
      case _       => ""
    }

  // Double click a table row fetches and opens the corresponding file.
  listenTo(attachmentTable)
  reactions += {
    case evt: MouseClicked if evt.clicks == 2 && openAction.enabled => openAction()
  }

  // Watch the model in order to reset the table model and selection.
  listenTo(model)
  reactions += {
    case AuxFileStateEvent(evt) =>
      val (files, sel) = evt.map(s => (s.files, s.selection)).getOrElse((Nil, Nil))
      deafTo(attachmentTable.selection)
      attachmentTable.model = new AttachmentTableModel(files.asJava)
      attachmentTable.selection.rows.clear()
      attachmentTable.selection.rows ++= selectedIndices(files, sel)
      if (sel.isEmpty) descriptionTextField.text = ""
      listenTo(attachmentTable.selection)
  }

  // Selecting table rows updates the model to record the selection and let the
  // actions update their enabled state.
  listenTo(attachmentTable.selection)
  reactions += {
    case TableRowsSelected(_, _, false) =>
      for (pid <- model.currentPid; files <- model.currentFiles) {
        deafTo(model)
        val sel = selectedFiles(files)
        model.select(pid, sel)
        descriptionTextField.text = commonDescription(sel)
        listenTo(model)
      }
  }
}
