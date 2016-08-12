package edu.gemini.util.security.auth.ui

import edu.gemini.util.security.auth.keychain.Key
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._

import scalaz.effect.IO
import java.awt
import java.awt.event.MouseAdapter
import java.awt.{Color, Font}
import javax.swing

import scala.swing._
import Swing._
import swing.event.{ListSelectionEvent, ListSelectionListener}
import swing._
import swing.Icon
import swing.table.{AbstractTableModel, DefaultTableCellRenderer}

import edu.gemini.util.security.principal._

import scala.swing.TabbedPane.Page
import scala.swing.Action
import scala.swing.event.ButtonClicked
import edu.gemini.spModel.core.Peer

object AuthDialog {
  val instance = this

  // This is set by the activator
  var showDatabaseTab = false

  def open(ac: KeyChain, parent: UIElement):Unit = new AuthDialog(ac).open(parent)
  def open(ac: KeyChain, parent: JComponent):Unit = new AuthDialog(ac).open(Component.wrap(parent))

  def openWithDetailText(ac: KeyChain, detailText: String, parent: UIElement):Unit = new AuthDialog(ac, detailText).open(parent)
  def createWithDetailText(ac: KeyChain, detailText: String): AuthDialog = new AuthDialog(ac, detailText)
}

class AuthDialog(ac: KeyChain, detailText: String =  "Database keys allow you to access data and features in OCS tools.") extends Dialog with CloseOnEsc {  dialog =>

  // Config
  title = "Keychain Manager"
  contents = Contents
  pack()

  // Icon loader
  def getIcon(name:String) = new ImageIcon(AuthDialog.getClass.getResource(name))

  // Open relative to parent
  def open(parent: UIElement) {
    modal = true
    Option(parent).foreach(setLocationRelativeTo)
    open()
  }

  object AddKeyAction extends Action("Add Key") {
    icon = getIcon("icons/add.gif")
    def apply = AddKeyDialog.open(ac, dialog)
  }

  object DeleteKeyAction extends Action("Remove Key") {
    icon = getIcon("icons/delete.gif")
    enabled = false
    def confirmDelete = Dialog.showConfirmation(dialog.Contents,
      "Deleting this key will remove associated permissions.\n" +
        "This operation cannot be undone.\n\n" +
        "Do you wish to continue?", "Confirm Deletion", Dialog.Options.YesNo,
      Dialog.Message.Warning, null) == Dialog.Result.Ok
    def apply {
      if (confirmDelete) KeyTab.Table.selectedItem.foreach { kp =>
        ac.removeKey(kp._1).unsafeRunAndThrow
      }
    }
  }

  KeyTab.Table.table.getSelectionModel.addListSelectionListener(new ListSelectionListener {
    def valueChanged(e: ListSelectionEvent) {
      DeleteKeyAction.enabled = KeyTab.Table.selectedItem.isDefined
    }
  })

  KeyTab.Table.table.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: java.awt.event.MouseEvent) {
      if (e.getClickCount == 2) {
        val sel = KeyTab.Table.selectedItem.map(_.swap) // oops
        ac.select(sel).unsafeRunAndThrow
      }
    }
  })

  object AddDbAction extends Action("Add Database") {
    icon = getIcon("icons/add.gif")
    def apply {
      AddDbDialog.open(ac, dialog)
    }
  }

  object DeleteDbAction extends Action("Remove Database") {
    icon = getIcon("icons/delete.gif")
    enabled = false
    def confirmDelete = Dialog.showConfirmation(dialog.Contents,
      "There are keys associated with this database, which will also be removed.\n" +
        "This operation cannot be undone.\n\n" +
        "Do you wish to continue?", "Confirm Deletion", Dialog.Options.YesNo,
      Dialog.Message.Warning, null) == Dialog.Result.Ok
    def apply {
      DatabaseTab.Table.selectedItem.foreach { db =>
        val keysExist: Boolean =
          ac.keys.map(!_.get(db).forall(_.isEmpty)).unsafeRun.fold(throw _, identity)
        if (!keysExist || confirmDelete) {
          ac.removePeer(db).unsafeRunAndThrow
        }
      }
    }
  }

  DatabaseTab.Table.table.getSelectionModel.addListSelectionListener(new ListSelectionListener {
    def valueChanged(e: ListSelectionEvent) {
      DeleteDbAction.enabled = DatabaseTab.Table.selectedItem.exists { p =>
        Option(p.site).isEmpty
      }
    }
  })

  object ToggleLockAction extends Action("Lock") {

    val iconL = getIcon("icons/lock_closed.png")
    val iconU = getIcon("icons/lock_open.png")

    ac.addListener(IO{
      update()
      dialog.peer.isVisible
    }).unsafeRunAndThrow

    def update() {
      setIcon()
      setText()
    }

    def setIcon() {
      icon = if (ac.isLocked.unsafeRunAndThrow) iconL else iconU
    }

    def setText() {
      title = if (ac.isLocked.unsafeRunAndThrow) "Click to unlock the keychain." else "Click to lock the keychain."
    }

    def confirmLock = Dialog.showConfirmation(dialog.Contents,
      "To protect your keychain from unauthorized usage, you will provide a password that will\n" +
      "be required each time you start this application. You can remove this password at a later\n" +
      "time if you choose, however if you forget the password while your keychain is locked, your\n" +
      "keys will be lost and you will need to acquire them again.\n\n" +
      "Do you wish to continue?", "Confirm Lock", Dialog.Options.YesNo,
      Dialog.Message.Warning, null) == Dialog.Result.Ok


    def apply {
      if (ac.isLocked.unsafeRunAndThrow) {
        PasswordDialog.unlock(ac, dialog)
      } else if (ac.hasLock.unsafeRunAndThrow) {
        ac.lock.unsafeRunAndThrow
      } else {
        if (confirmLock) {
          ChangePasswordDialog.open(ac, dialog)
          if (ac.hasLock.unsafeRunAndThrow)
            ac.lock.unsafeRunAndThrow
        }
      }
    }

  }

  ToggleLockAction.update() // init order issues :-/


  object SetOrChangePasswordAction extends Action("") {

    def apply() {
      ChangePasswordDialog.open(ac, dialog.Contents)
    }

    def update() {
      title = if (ac.hasLock.unsafeRunAndThrow) "Change Password..." else "Set Password..."
    }

    ac.addListener(IO{
      update()
      dialog.visible
    }).unsafeRunAndThrow

    update()

  }

  object RemovePasswordAction extends Action("Remove Password...") {

    def apply() {
      PasswordDialog.removePassword(ac, dialog.Contents)
    }

    def update() {
      enabled = ac.hasLock.unsafeRunAndThrow
    }

    ac.addListener(IO{
      update()
      dialog.visible
    }).unsafeRunAndThrow

    update()

  }


  // Our main content object
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    add(Header, BorderPanel.Position.North)
    add(Footer, BorderPanel.Position.South)

    // ug
    def updateMainEnabledState() {
      if (ac.isLocked.unsafeRunAndThrow) {
        add(LockedPanel, BorderPanel.Position.Center)
        repaint()
      } else {
        add(Tabs, BorderPanel.Position.Center)
        repaint()
      }
    }
    ac.addListener(IO{
      updateMainEnabledState()
      dialog.peer.isVisible
    }).unsafeRunAndThrow

    updateMainEnabledState()

    // Disabled panal
    object LockedPanel extends Label("Keychain is Locked") {
      opaque = true
      background = new Color(64, 64, 64)
      foreground = Color.WHITE
      font = font.deriveFont(font.getSize * 2f)
      preferredSize = Tabs.preferredSize
    }

    // Tab panel, which can be disabled all at once
    object Tabs extends TabbedPane {
      val disabledGray = new Color(0,0,0,0.5f)
      border = null
      focusable = false
      pages += new Page("Keys", KeyTab)
      if (AuthDialog.showDatabaseTab)
        pages += new Page("Databases", DatabaseTab)
      pages += new Page("Options", Options)
    }

  }

  // Our header
  object Header extends Instructions {
    def icon = getIcon("icons/database_key.png")
    def instructions =
      "This tool allows you to manage the database keys in your keychain.\n" + detailText
  }


  // key tab
  object KeyTab extends TablePanel[(Key, Peer), AuthTableModel] (
    new AuthTableModel(ac, dialog.peer.isVisible),
    AddKeyAction,
    DeleteKeyAction,
    getIcon("icons/key_small.png"),
    { case (k, p) => ac.selection.unsafeRun.fold(_ => false, _.contains((p, k))) },
    Seq(125, 125, 225),
    Some("Double-click to change the current active key."))

  // Database tab
  object DatabaseTab extends TablePanel[Peer, DatabaseTableModel] (
    new DatabaseTableModel(ac, dialog.peer.isVisible),
    AddDbAction,
    DeleteDbAction,
    getIcon("icons/db_small.png"),
    _ => false,
    Seq(225, 100, 50))


  object Options extends BorderPanel {

    add(new GridPanel(3,1) {

      preferredSize = (200, preferredSize.height)

      contents += new Button(SetOrChangePasswordAction)
      contents += new Button(RemovePasswordAction)
      contents += Button("Reset Keychain...") {
        Reset.reset(ac, dialog.Contents)
      }

    }, BorderPanel.Position.Center)
  }



  // Our footer
  object Footer extends BorderPanel {

    object Locked extends BorderPanel {
      add(new Button(ToggleLockAction) {
        // FFS
        text = null
        opaque = false
        border = null
        margin = new Insets(0,0,0,0 )
        focusable = false
        peer.setContentAreaFilled(false)
        peer.setBorderPainted(false)
        icon = getIcon("icons/lock_closed.png")
        foreground = Color.DARK_GRAY
      }, BorderPanel.Position.West)
    }

    def lock() {
      add(Locked, BorderPanel.Position.West)
    }

    lock()

    add(new FlowPanel {
      contents += new Button("Close") {
        reactions += {
          case ButtonClicked(_) => dialog.close()
        }
      }
    }, BorderPanel.Position.East)
  }

}



///
/// HELPER CLASSES
///



abstract class MyTableModel[A] extends AbstractTableModel {
  def get(i:Int):Option[A]
}

class AuthTableModel(ac: KeyChain, alive: => Boolean) extends MyTableModel[(Key, Peer)]  {

  // Our principals are expensive to calculate, so we cache them
  var principals: List[(Key, Peer)] = Nil
  def recomputePrincipals(): Unit =
    principals =
      ac.keys.unsafeRun.fold(_ => Map(), identity).toList.flatMap {
        case (peer, keys) => keys.map((_, peer))
      }.sortBy(_.toString)

  // Watch the keychain for updates (will get called once on register)
  ac.addListener(IO{
    recomputePrincipals()
    Swing.onEDT(fireTableDataChanged())
    alive
  }).unsafeRunAndThrow

  def get(i: Int): Option[(Key,Peer)] = principals.lift(i)
  def getRowCount: Int = principals.size
  def getColumnCount: Int = 3

  def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = get(rowIndex).map {
    case (key, peer) =>
      columnIndex match {
        case 0 => key.get._1 match {
          case ProgramPrincipal(_)   => "Program Key"
          case AffiliatePrincipal(_) => "Partner Key"
          case StaffPrincipal(_)     => "Staff Key"
          case UserPrincipal(_)      => "User Key"
          case VisitorPrincipal(_)   => "Visitor Key"
        }
        case 1 => key.get._1.getName
        case 2 => peer.displayName
      }
  }.orNull

  override def getColumnName(column: Int): String = column match {
    case 0 => "Key Type"
    case 1 => "Key Name"
    case 2 => "Database"
  }

}


class DatabaseTableModel(ac: KeyChain, alive: => Boolean) extends MyTableModel[Peer] {

  ac.addListener(IO{
    Swing.onEDT(fireTableDataChanged())
    alive
  }).unsafeRunAndThrow

  def get(i: Int) = {

    val ps = ac.peers.map(_.toList).unsafeRun.fold(_ => Nil, identity)
    if (ps.isDefinedAt(i)) Some(ps(i)) else None
  }

  def getRowCount: Int = ac.peers.unsafeRun.map(_.size).fold(_ => 0, identity)
  def getColumnCount: Int = 4

  def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = get(rowIndex).map {
    db =>
      columnIndex match {
        case 0 => db.displayName
        case 1 => db.host
        case 2 => db.port.toString
        case 3 => Option(db.site).map(_.abbreviation).orNull
      }
  }.orNull

  override def getColumnName(column: Int): String = column match {
    case 0 => "Database"
    case 1 => "Host"
    case 2 => "Port"
    case 3 => "Site"
  }

}

// A panel with a table and add/remove commands
class TablePanel[A, M <: MyTableModel[A]](val model:M, addAction:Action, deleteAction:Action, icon: Icon, bold: A => Boolean, cols:Seq[Int], footerText: Option[String] = None) extends BorderPanel {

  // Our content, defined below
  add(Table,   BorderPanel.Position.Center)
  add(Buttons, BorderPanel.Position.South)

  // Our table
  object Table extends ScrollPane {

    // Contents, defined below
    contents = Component.wrap(table)

    // Table model and table
    lazy val table = new JTable(model) {
      setCellSelectionEnabled(false)
      setRowSelectionAllowed(true)
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      setShowGrid(false)
      setShowVerticalLines(false)
      setIntercellSpacing((0, 1))
      setFocusable(false)
      cols.zipWithIndex.foreach {
        case (w, n) => getColumnModel.getColumn(n).setPreferredWidth(w)
      }
      setDefaultRenderer(classOf[Object], Renderer)
    }

    // Set up our viewport
    val viewport = peer.getViewport
    val vSize = (table.getPreferredSize.width, table.getRowHeight * 10)
    viewport.setMinimumSize(vSize)
    viewport.setPreferredSize(vSize)
    viewport.setBackground(table.getBackground)

    // Convenience method to get selected things
    def selectedItem:Option[A] = model.get(table.getSelectedRow)

    object Fonts {
      val normal: awt.Font = peer.getFont
      val bold: awt.Font = normal.deriveFont(Font.BOLD)
    }

    // A custom renderer
    object Renderer extends DefaultTableCellRenderer {
      import scalaz.syntax.std.boolean._
      override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): awt.Component = {

        // Ok we better be on EDT
        if (!SwingUtilities.isEventDispatchThread)
          throw new Error

        val r = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).asInstanceOf[DefaultTableCellRenderer]
        r.setIcon(if (column == 0) icon else null)
        r.setBorder(swing.BorderFactory.createEmptyBorder(1, 2, 1, 2))
        r.setFont(model.get(row).exists(bold).fold(Fonts.bold, Fonts.normal))
        r
      }
    }



  }

  object Buttons extends BorderPanel {
    add(new FlowPanel {
      contents += new ImageButton(addAction)
      contents += new ImageButton(deleteAction)
    }, BorderPanel.Position.West)
    footerText.map(new Label(_)).foreach(add(_, BorderPanel.Position.Center))
  }


}

// Image button class, duh
class ImageButton(a:Action) extends Button(a) {
  text = ""
  focusable = false
}

