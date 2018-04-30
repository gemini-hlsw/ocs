package jsky.app.ot.viewer.open

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.shared.gui.SizePreference
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Newer}
import edu.gemini.shared.util.immutable.ApplyOp
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.sp.vcs2.TryVcs
import edu.gemini.sp.vcs2._
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.spModel.util.DBProgramInfo
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.keychain.{Key, KeyChain, Action => KAction}
import edu.gemini.util.security.ext.auth.ui.{AuthDialog, CloseOnEsc, Instructions}
import jsky.app.ot.OT
import jsky.app.ot.vcs.VcsOtClient
import jsky.app.ot.viewer.DBProgramChooserFilter
import java.awt
import java.awt.Color
import java.awt.event.{MouseAdapter, MouseEvent}
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing
import javax.swing._
import javax.swing.event.{ListSelectionEvent, ListSelectionListener, TableModelEvent}
import javax.swing.table.DefaultTableCellRenderer

import jsky.util.gui.Resources

import scala.actors.Futures.future
import scala.collection.JavaConverters._
import scala.swing.Swing._
import scala.swing.event.UIElementResized
import scala.swing.{Action, _}
import scalaz.Scalaz._
import scalaz._
import scalaz.effect.IO

object OpenDialog {

  lazy val Log = java.util.logging.Logger.getLogger(getClass.getName)
  def currentUser = OT.getKeyChain.subject.getPrincipals.asScala.toSet

  def open(db: IDBDatabaseService, auth: KeyChain, vcs: VcsRegistrar, parent: UIElement): List[ISPProgram] = {
    val d = new OpenDialog(db, auth, vcs)
    try d.open(parent) finally d.dispose()
  }

  def open(db: IDBDatabaseService, auth: KeyChain, vcs: VcsRegistrar, parent: JComponent): java.util.List[DBProgramInfo] =
    open(db, auth, vcs, Component.wrap(parent)).map(_.getProgInfo(db)).toList.asJava

  /** NULLABLE java style */
  def checkout(db: IDBDatabaseService, pid: SPProgramID, peer: Peer, parent: JComponent, vcs: VcsRegistrar): ISPProgram =
    checkout(db, pid, peer, Component.wrap(parent), vcs).orNull

  /** NULLABLE java style */
  def update(db: IDBDatabaseService, pid: SPProgramID, peer: Peer, parent: JComponent, vcs: VcsRegistrar): ISPProgram =
    update(db, pid, peer, Component.wrap(parent), vcs).orNull

  def checkout(db: IDBDatabaseService, pid: SPProgramID, peer: Peer, parent: Component, vcs: VcsRegistrar): Option[ISPProgram] =
    vcs2Op(db, pid, peer, parent, vcs, "checkout") { _.checkout(pid, peer, new AtomicBoolean(false)).unsafeRun }

  def update(db: IDBDatabaseService, pid: SPProgramID, peer: Peer, parent: Component, vcs: VcsRegistrar): Option[ISPProgram] =
    vcs2Op(db, pid, peer, parent, vcs, "pull") { _.pull(pid, new AtomicBoolean(false)).unsafeRun.as(db.lookupProgramByID(pid)) }

  private def vcs2Op(db: IDBDatabaseService, pid: SPProgramID, peer: Peer, parent: Component, vcs: VcsRegistrar, opName: String)(op: VcsOtClient => VcsFailure \/ ISPProgram): Option[ISPProgram] = {
    def success(p: ISPProgram): Option[ISPProgram] = {
      vcs.register(p.getProgramID, peer)
      Some(p)
    }

    def fail(f: VcsFailure): Option[ISPProgram] = {
      val msg = VcsFailure.explain(f, pid, opName, Some(peer))
      Dialog.showMessage(parent, msg, "Error", Dialog.Message.Error)
      None
    }

    for {
      client <- VcsOtClient.ref
      prog   <- op(client).fold(fail, success)
    } yield prog
  }
}

class OpenDialog private(db: IDBDatabaseService, auth: KeyChain, vcs: VcsRegistrar) extends Dialog with Publisher with CloseOnEsc {
  dialog =>

  // Entry point
  def open(parent: UIElement): List[ISPProgram] = {

    // Register our title updater with the keychain, retaining a token to unregister when the
    // window is closed. We need to do this to allow the dialog to be garbage-collected.
    val unreg = auth.addListener(titleUpdater).unsafeRunAndThrow

    try {
      modal = true
      Option(parent).foreach(setLocationRelativeTo)
      updateStorage()
      open()
      selection
    } finally {
      auth.removeListener(unreg).unsafeRunAndThrow
      Contents.ProgTable.table.storeSortPreference()
    }
  }

  // Members
  val keySelCombo = new KeySelectionCombo(auth, new ApplyOp[Option[(Peer, Key)]] {
    override def apply(t: Option[(Peer, Key)]): Unit = Contents.ProgTable.refresh(full = true)
  })

  var selection: List[ISPProgram] = Nil
  val filter = new DBProgramChooserFilter(DBProgramChooserFilter.Mode.localAndRemote)

  val MaxM = 50
  val MaxK = MaxM * 1000

  def usedBytes: Long =
    SPDB.get().getDBAdmin.getTotalStorage

  def usedK: Int =
    (usedBytes / 1000).round.toInt

  // Storage
  def updateStorage(): Unit = {
    val usedM = usedK / 1000.0
    val percent = usedK * 100 / MaxK
    Contents.Footer.storageProgressBar <| { b =>
      b.min   = 0
      b.max   = MaxK // kilobytes
      b.value = usedK
      b.peer.setString(f"$usedM%1.1fM of $MaxM%dM used")
      b.peer.setStringPainted(true)
      b.foreground = (if (percent < 75) Color.GREEN else if (percent < 90) Color.YELLOW else Color.RED).darker()
    }
  }

  // Run this action when the keychain changes. Registered/removed in open() above
  val titleUpdater: IO[Boolean] =
    (for {
      t <- auth.selection.map(_.fold("No Active Key")(p => p._2.get._1.getName + " (" + p._1.displayName + ")"))
      _ <- IO(title = "Open Program - " + t).liftIO[KAction]
      v <- IO(dialog.visible).liftIO[KAction]
    } yield v).run.map(_.fold(_ => false, identity))

  // Configuration
  contents = Contents
  pack()

  // Manage the window size preferences.
  SizePreference.getDimension(getClass).foreach { dim => this.size = dim }
  listenTo(this)
  reactions += {
    case e: UIElementResized =>
      SizePreference.setDimension(getClass, Some(this.size))
  }

  def isRemote(p: ISPProgram): Boolean =
    Option(p.getProgramID).exists(vcs.allRegistrations.isDefinedAt)

  // Our actions
  object Actions {

    // Init (soon, to avoid initialization issues ... this is awful)
    future {

      def updateEnabledStatus() {
        val tab = Contents.ProgTable

        RevertAction.enabled   = tab.selectedProgs.filter(tab.hasPendingChanges).nonEmpty
        DeleteAction.enabled   = tab.selectedProgs.nonEmpty
        OpenAction.enabled     = tab.selectedItems.nonEmpty
        CheckoutAction.enabled = tab.selectedRemotes.nonEmpty
      }

      Swing.onEDT {
        updateEnabledStatus()
        Contents.ProgTable.onSelectionChange(updateEnabledStatus())
      }
    }

    private def explainFailure(pid: SPProgramID, p: Peer, f: VcsFailure, op: String): String =
      s"${pid.stringValue}: ${VcsFailure.explain(f, pid, op, Some(p))}"

    // Explain the first n failures.  Show "..." if there are more than n failures.
    private def handleFailure(fs: List[String]): Unit =
      if (fs.nonEmpty) {
        val N = 5
        val firstN = fs.take(N)
        val msg    = (if (fs.drop(N).nonEmpty) firstN :+ "..." else firstN).mkString("\n")

        Dialog.showMessage(Contents, msg, "Error", Dialog.Message.Error)
      }

    lazy val ManageKeysAction = Action("Manage Keys...") {
      val detailText = "Database keys allow you to access programs and OT features."
      AuthDialog.openWithDetailText(auth, detailText, Contents)
      keySelCombo.refresh()
      Contents.ProgTable.refresh(full = true)
    }

    lazy val RefreshAction = Action("Refresh") {
      Contents.ProgTable.refresh(full = true)
    }

    lazy val OpenAction = Action("Open") {
      if (Contents.ProgTable.selectedRemotes.nonEmpty) CheckoutAction()
      selection = Contents.ProgTable.selectedProgs
      if (selection.nonEmpty) dialog.close()
    }

    lazy val CancelAction = Action("Cancel") {
      dialog.close()
    }

    lazy val RevertAction = Action("Revert") {
      val ps    = Contents.ProgTable.selectedProgs.filter(Contents.ProgTable.hasPendingChanges)
      val total = ps.size

      val warning: String =
        s"""This action will delete all local changes made
           |since the last sync and restore the program${if (total === 1) "" else "s"}
           |from the remote database.
           |
           |Continue?
         """.stripMargin

      def doRevert(client: VcsOtClient): Unit = {
        val ps0 = ps.flatMap { p =>
          (for {
            pid  <- Option(p.getProgramID)
            peer <- client.peer(pid)
          } yield (pid, peer, p)).toList
        }

        val result: Throwable \/ List[String \/ Unit] =
          ps0.traverseU { case (pid, peer, p) =>
            client.revert(pid, peer, new AtomicBoolean(false)).run
                  .map(_.bimap(explainFailure(pid, peer, _, "revert"), _ => ()))
          }.unsafePerformSyncAttempt

        result match {
          case -\/(_)   =>
            val msg = "There was an error reverting programs from the remote database."
            Dialog.showMessage(Contents, msg, "Error", Dialog.Message.Error)

          case \/-(lst) =>
            val (fs, ss) = lst.separate
            if (ss.nonEmpty) {
              Contents.ProgTable.refresh(full = false)
              updateStorage()
            }
            handleFailure(fs)
        }
      }

      if (total > 0) {
        val confirm = Dialog.showConfirmation(dialog.Contents, warning, "Confirm Revert", Dialog.Options.YesNo, Dialog.Message.Warning)
        if (confirm == Dialog.Result.Ok) {
          VcsOtClient.ref.foreach(doRevert)
        }
      }
    }

    lazy val DeleteAction = Action("Delete") {
      val ps    = Contents.ProgTable.selectedProgs
      val total = ps.size

      def localWarning(count: Int): String =
        count match {
          case 0 =>
            ""

          case 1 =>
            s"""${if (total === 1) "This" else "One"} program exists only in your local database.  If you have not made an XML
               |backup you will have no way to retrieve this program again.
               |
               |""".stripMargin

          case _ =>
            s"""$count programs exist only in your local database. If you have not made XML
               |backups you will have no way to retrieve these programs again.
               |
               |""".stripMargin
        }

      def remoteWarning(count: Int): String =
        count match {
          case 0 =>
            ""

          case 1 =>
            s"""${if (total === 1) "This" else "One"} program is also present in a remote database but contains changes
               |which will be lost if the program is deleted.  Make sure that you have
               |synchronized your version of the program if you want to keep those changes
               |before deleting it.
               |
               |""".stripMargin

          case _ =>
            s"""$count programs, while present in the remote database, have been modified locally.
               |Any changes will be lost if the programs are deleted. Make sure that you have
               |synchronized your copies if you want to keep those changes before deleting them.
               |
               |""".stripMargin
        }

      if (total > 0) {
        val localOnly     = ps.count(p => !isRemote(p))
        val updatedRemote = ps.count(Contents.ProgTable.hasPendingChanges)
        val icon          = if (localOnly + updatedRemote > 0) Dialog.Message.Warning
                            else Dialog.Message.Question

        val msg = localWarning(localOnly) ++ remoteWarning(updatedRemote) ++
                    s"Delete ${if (total === 1) "this program" else s"$total programs"} from your local database?"

        val confirm = Dialog.showConfirmation(dialog.Contents, msg, "Confirm Delete", Dialog.Options.YesNo, Dialog.Message.Warning)

        if (confirm == Dialog.Result.Ok) {
          ps.foreach(db.remove)
          Contents.ProgTable.refresh(full = false)
          updateStorage()
        }
      }
    }

    lazy val CheckoutAction = Action("Checkout") {

      // For all successes, if any, register the associatation of pid -> peer
      // and update the program table and storage meter.
      def handleSuccess(ss: List[(SPProgramID, Peer)]): Unit =
        if (ss.nonEmpty) {
          ss.foreach { case (pid, peer) => vcs.register(pid, peer) }

          Contents.ProgTable.refresh(full = false)
          updateStorage()
        }

      def doCheckout(client: VcsOtClient, remotes: List[(DBProgramInfo, Peer)]): Unit = {

        val checkoutResult: Throwable \/ List[String \/ (SPProgramID, Peer)] =
          remotes.traverseU { case (info, peer) =>
            client.checkout(info.programID, peer, new AtomicBoolean(false)).run
                  .map(_.bimap(explainFailure(info.programID, peer, _, "checkout"), p => (p.getProgramID, peer)))
          }.unsafePerformSyncAttempt

        checkoutResult match {
          case -\/(_)   =>
            val msg = "There was an error checking out programs from the remote database."
            Dialog.showMessage(Contents, msg, "Error", Dialog.Message.Error)

          case \/-(lst) =>
            val (fs, ss) = lst.separate
            handleSuccess(ss)
            handleFailure(fs)
        }
      }

      VcsOtClient.ref.foreach { client =>

        val remotes = Contents.ProgTable.selectedRemotes
        val sumBytes = remotes.map(_._1.size).sum

        if (sumBytes + usedBytes < MaxK * 1000)
          doCheckout(client, remotes)
        else {
          val msg =
            s"""Adding ${if (remotes.size === 1) "this program" else "these programs"} would exceed the storage limit.  Please delete
               |some previously obtained programs to make space and try again.
             """.stripMargin
          Dialog.showMessage(Contents, msg, "Warning", Dialog.Message.Warning)
        }
      }
    }

  }

  // Top-level GUI container
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    add(Header, BorderPanel.Position.North)
    add(ProgTable, BorderPanel.Position.Center)
    add(Footer, BorderPanel.Position.South)

    // Our header
    object Header extends Instructions {
      def icon = Resources.getIcon("program_large.png")

      def instructions =
        "The following science programs are available based on the active key.\n" +
          "Recently opened programs are highlighted, and are available for offline use. "
    }

    // Our footer
    object Footer extends BorderPanel {

      import Actions._

      val storageProgressBar = new ProgressBar

     object Filters extends GridBagPanel {
        border = BorderFactory.createEmptyBorder(11,11,0,11)

        layout(new Label("Key")) = new Constraints {
          gridx  = 0
          insets = new Insets(0,0,0,6)
        }

        layout(keySelCombo) = new Constraints {
          gridx  = 1
          insets = new Insets(0,0,0,6)
        }
        keySelCombo.refresh()

        layout(Component.wrap(filter.getFilterPanel)) = new Constraints {
          gridx   = 2
          fill    = GridBagPanel.Fill.Horizontal
          weightx = 1.0
        }
      }

      add(Filters, BorderPanel.Position.North)

      add(new FlowPanel {
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        contents += new NonFocusableButton(ManageKeysAction)
        contents += new NonFocusableButton(RefreshAction)
        contents += new NonFocusableButton(RevertAction)
        contents += new NonFocusableButton(DeleteAction)
      }, BorderPanel.Position.West)

      add(new BorderPanel {
        border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
        add(storageProgressBar, BorderPanel.Position.South)
      }, BorderPanel.Position.Center)

      add(new FlowPanel {
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        contents += new NonFocusableButton(OpenAction) {
          dialog.peer.getRootPane.setDefaultButton(peer)
        }
        contents += new Button(CancelAction)
      }, BorderPanel.Position.East)

      // Helpers

      class NonFocusableButton(a: Action) extends Button(a) {
        focusable = false
      }
    }

    object ProgTable extends ScrollPane {

      private val icon = Resources.getIcon("program_small.png")
      private val model = new ProgTableModel(filter, db, auth) //, vcs)

      // Contents, defined below
      contents = Component.wrap(table)

      // Set up our viewport
      val viewport = peer.getViewport
      val vSize = (table.getPreferredSize.width, table.getRowHeight * 10)
      viewport.setMinimumSize(vSize)
      viewport.setPreferredSize(vSize)
      viewport.setBackground(table.getBackground)

      private def selected[A](f: Int => Option[A]): List[A] =
        table.selectedModelRows.flatMap(f(_).toList)

      // Convenience method to get selected things
      def selectedItems: List[ProgTableModel.Elem] =
        selected(model.get)

      def selectedInfos: List[DBProgramInfo] =
        selected(model.getInfo)

      def selectedProgs: List[ISPProgram] =
        selected(model.getProg)

      def selectedRemotes: List[(DBProgramInfo, Peer)] =
        selected(model.getRemote)

      def hasPendingChanges(p: ISPProgram): Boolean =
        isRemote(p) &&
          model.getStatus(p.getProgramKey).forall(s => s === Newer || s === Conflicting)

      // Update methods
      def refresh(full: Boolean): Unit =
        model.refresh(full)

      // Allow clients to listen
      def onSelectionChange(f: => Unit) {
        table.getSelectionModel.addListSelectionListener(new ListSelectionListener {
          def valueChanged(e: ListSelectionEvent) = f
        })
      }

      // Our table
      object table extends JTable(model) {
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        setCellSelectionEnabled(false)
        setRowSelectionAllowed(true)
        setShowGrid(false)
        setShowVerticalLines(false)
        setIntercellSpacing((0, 1))
        setFocusable(false)
        model.cols.zipWithIndex.foreach {
          case ((_, w), n) => getColumnModel.getColumn(n).setPreferredWidth(w)
        }
        setDefaultRenderer(classOf[Object], Renderer)
        setDefaultRenderer(classOf[java.lang.Long], LongRenderer)

        setAutoCreateRowSorter(true)
        getRowSorter.setSortKeys(java.util.Collections.singletonList(SortPreference.get))

        def storeSortPreference(): Unit =
          Option(getRowSorter).foreach { rs =>
            Option(rs.getSortKeys).foreach { lst =>
              if (lst.size() > 0) SortPreference.set(lst.get(0))
            }
          }

        def selectedModelRows: List[Int] =
          getSelectedRows.toList.map(convertRowIndexToModel)

        // Ok in this hunk we do our best to maintain the selection when the list
        // is updated. We store the last non-empty selection and try to reset it
        // when the list changes, and then we scroll to it.
        private var sel: List[SPNodeKey] = Nil
        override def valueChanged(lse: ListSelectionEvent): Unit = {
          super.valueChanged(lse)
          sel = selectedModelRows.flatMap(model.get(_).toList).map(_.fold(_.getNodeKey, _.nodeKey))
        }
        override def tableChanged(e: TableModelEvent) {
          val copy = sel
          super.tableChanged(e)
          if (copy != null) { // @&^#%&^$% initialization bug
            val sm = getSelectionModel
            sm.clearSelection
            val trs = copy.flatMap(model.indexOf(_).toList).map(convertRowIndexToView)
            trs.foreach { tr => sm.addSelectionInterval(tr, tr) }
            trs.headOption.foreach { tr => scrollRectToVisible(getCellRect(tr, 0, true)) }
          }
        }
        // End of selection-maintenance hunk


        addMouseListener(new MouseAdapter {
          override def mouseClicked(e: MouseEvent) {
            if (e.getClickCount == 2) {
              if (Actions.OpenAction.enabled)
                Actions.OpenAction.apply()
            }
          }
        })

      }

      // A custom renderer
      class ColorRenderer extends DefaultTableCellRenderer {
        override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): awt.Component = {
          val r = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).asInstanceOf[DefaultTableCellRenderer]
          r.setIcon(if (column == 0) icon else null)
          r.setBorder(swing.BorderFactory.createEmptyBorder(1, 2, 1, 2))
          r.setForeground(model.get(table.convertRowIndexToModel(row)) match {
            case Some(Right(_)) => Color.GRAY
            case _ => Color.BLACK
          })
          r
        }
      }

      object Renderer extends ColorRenderer

      object LongRenderer extends ColorRenderer {
        override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): awt.Component = {
          val r = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).asInstanceOf[DefaultTableCellRenderer]
          val l = value.asInstanceOf[java.lang.Long]

          val (d,u) = if (l > 1000000) (l / 1000000.0, "M") else (l / 1000.0, "K")
          val s = f"$d%1.1f$u%s"

          r.setHorizontalAlignment(SwingConstants.RIGHT)
          r.setText(s)
          r
        }
      }

    }

  }


}
