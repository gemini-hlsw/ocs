package jsky.app.ot.vcs

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Newer, Older, Same}
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.sp.vcs2.{ProgramLocationSet, TryVcs, VcsFailure}
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.sp.vcs2.VcsFailure.{HasConflict, VcsException}
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.spModel.rich.pot.spdb._
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.ui.CloseOnEsc
import jsky.app.ot.OT
import jsky.app.ot.shared.vcs.VersionMapFunctor.VmUpdate
import jsky.app.ot.userprefs.general.GeneralPreferences
import jsky.app.ot.util.OtColor
import jsky.app.ot.vcs.vm.{VmStore, VmUpdater}
import jsky.app.ot.viewer.ViewerManager
import java.awt.{Color, Component, Font}
import java.awt.event.{MouseAdapter, MouseEvent}
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.{BorderFactory, JTable, ListSelectionModel}
import javax.swing.table.{DefaultTableCellRenderer, TableColumn}

import jsky.util.gui.Resources

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill._
import scala.swing.Swing._
import scala.swing.event.ActionEvent
import scalaz._
import Scalaz._


object SyncAllDialog {
  private def selectedPeer(auth: KeyChain): Option[Peer] =
    auth.selection.unsafeRun.toOption.flatten.map(_._1)

  private def canSync(selectedPeer: Peer, vcs: VcsRegistrar)(p: ISPProgram): Boolean =
    Option(p.getProgramID).flatMap(vcs.registration).exists(_ == selectedPeer)

  def shouldClose(comp: Component, auth: KeyChain, vcs: VcsRegistrar, db: IDBDatabaseService, progs: java.util.List[ISPProgram]): Boolean =
    shouldClose(comp, auth, vcs, db, progs.asScala.toVector)

  def shouldClose(comp: Component, auth: KeyChain, vcs: VcsRegistrar, db: IDBDatabaseService, progs: Vector[ISPProgram]): Boolean = {
    def isModified(p: ISPProgram): Boolean =
      Option(p.getProgramID).exists { pid =>
        VmStore.get(pid).exists { rvm =>
          VersionMap.compare(p.getVersions, rvm) match {
            case Newer | Conflicting => true
            case _                   => false
          }
        }
      }

    if (!GeneralPreferences.fetch().warnUnsavedChanges()) true
    else selectedPeer(auth).fold(true) { sp =>
      progs.filter(canSync(sp, vcs)).filter(isModified) match {
        case Vector()  => true
        case syncProgs =>
          val dialog = new SyncAllDialog(SyncMode.ClosePrompt, sp, syncProgs, db)
          dialog.peer.setLocationRelativeTo(comp)
          dialog.visible = true

          if (dialog.Contents.Buttons.dontAsk.selected) {
            GeneralPreferences.fetch().withWarnUnsavedChanges(false).store()
          }
          dialog.closeAnyway
      }
    }
  }

  def syncAll(comp: Component, auth: KeyChain, vcs: VcsRegistrar, db: IDBDatabaseService): Unit =
    selectedPeer(auth).foreach { sp =>
      val dialog = new SyncAllDialog(SyncMode.All, sp, db.allPrograms(OT.getUser).filter(canSync(sp, vcs)), db)
      dialog.peer.setLocationRelativeTo(comp)
      dialog.visible = true
    }
}


class SyncAllDialog private(mode: SyncMode, selectedPeer: Peer, programs: Vector[ISPProgram], db: IDBDatabaseService) extends Dialog with CloseOnEsc { dialog =>
  title = if (mode == SyncMode.ClosePrompt) "Confirm Close" else "Sync Programs"
  modal = true

  val cancelled   = new AtomicBoolean(java.lang.Boolean.FALSE)
  var closeAnyway = false

  var model = SyncAllModel.init(programs)

  private def updateModel(f: SyncAllModel => SyncAllModel): Unit =
    Swing.onEDT { updateModelOnCurrentThread(f) }

  private def updateModelOnCurrentThread(f: SyncAllModel => SyncAllModel): Unit = {
    model = f(model)
    updateUi()
  }

  // Kick off a program status update for all the programs.
  private def updateStatus(): Unit = {
    import SyncAllModel.State.ProgramStatusUpdating
    val pids = model.programs.filter(_.state == ProgramStatusUpdating).map(_.pid)

    def markSyncFailed(ex: Exception): Unit =
      pids.foreach { pid => updateModel(_.markSyncFailed(pid, some(VcsException(ex)))) }

    def updateRemoteVersions(ups: List[VmUpdate]): Unit = {
      // have to poke the state machine even if there is no actual update to the
      // version map for some (or all) pids.  create dummy "updates" for these
      val unUpdatedPids = pids.toSet -- ups.map(_.pid)
      val noUps = pids.map { pid => (pid, VmStore.get(pid)) }.collect {
        case (pid, Some(vm)) => VmUpdate(pid, vm)
      }
      (noUps ++ ups).foreach { up => updateModel(_.updateRemoteVersion(up.pid, up.vm))}
    }

    VmUpdater.updateAll(selectedPeer, pids).onComplete {
      case scala.util.Failure(ex: Exception) => markSyncFailed(ex)
      case scala.util.Failure(t)             => markSyncFailed(new RuntimeException(t))
      case scala.util.Success(ups)           => updateRemoteVersions(ups)
    }
  }
  updateStatus()

  val SyncAction = Action("Sync All") {
    def doSync(p: ISPProgram): Unit = {
      val pid = p.getProgramID

      val handleResult: TryVcs[(ProgramLocationSet, VersionMap)] => Unit = {
        case \/-(a)           => updateModel(_.markSuccess(pid))
        case -\/(HasConflict) => updateModel(_.markSyncConflict(pid))
        case -\/(failure)     => updateModel(_.markSyncFailed(pid, some(failure)))
      }

      VcsOtClient.ref.foreach { c =>
        c.sync(pid, cancelled).forkAsync(r => Swing.onEDT(handleResult(r)))
      }
    }

    updateModelOnCurrentThread(_.markSyncInProgress)
    val syncList = model.programs.filter { ps =>
      ps.state match {
        case SyncAllModel.State.SyncInProgress(_) => true
        case _                                    => false
      }
    }.map(_.program)
    syncList.foreach(doSync)
  }

  private def quit(closeProgram: Boolean): Unit = {
    closeAnyway = closeProgram
    cancelled.set(java.lang.Boolean.TRUE)
    close()
    dispose()
  }

  object Contents extends GridBagPanel {
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    object Instructions extends BorderPanel {
      add(new Label() {
        icon = VcsIcon.UpToDate
      }, BorderPanel.Position.West)

      val textArea = new TextArea("") {
        border   = BorderFactory.createEmptyBorder(0,10,0, 0)
        opaque   = false
        peer.setDisabledTextColor(Color.DARK_GRAY)
        enabled  = false
        wordWrap = true
        lineWrap = true
      }

      add(textArea, BorderPanel.Position.Center)
    }

    object ProgTable extends ScrollPane {
      val tableModel = new SyncAllTableModel

      val viewPort = peer.getViewport
      val vSize    = (table.getPreferredSize.width, table.getRowHeight * 10)
      viewPort.setMinimumSize(vSize)
      viewPort.setPreferredSize(vSize)
      viewPort.setBackground(table.getBackground)

      object table extends JTable(tableModel) {
        def tc(col: SyncAllTableModel.Column): TableColumn =
          getColumnModel.getColumn(SyncAllTableModel.ColumnIndex(col))

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        setCellSelectionEnabled(false)
        setRowSelectionAllowed(true)
        setShowGrid(false)
        setShowVerticalLines(false)
        setIntercellSpacing((0, 1))
        setFocusable(false)
        SyncAllTableModel.Columns.zipWithIndex.foreach { case (col, n) =>
          getColumnModel.getColumn(n).setPreferredWidth(col.width)
        }
        tc(SyncAllTableModel.StateColumn).setCellRenderer(LightBulbRenderer)
        tc(SyncAllTableModel.DetailColumn).setCellRenderer(DetailRenderer)
        setDefaultRenderer(classOf[Object], DefaultRenderer)

        addMouseListener(new MouseAdapter {
          override def mouseClicked(e: MouseEvent): Unit =
            e.getClickCount match {
              case 1 => updateProgramDetail()
              case 2 => selectedProgram.foreach { ps => ViewerManager.open(ps.pid) }
              case _ => // do nothing
            }
        })

        def selectedProgram: Option[SyncAllModel.ProgramSync] =
          getSelectedRow match {
            case i if i >= 0 => some(tableModel.program(i))
            case _           => none[SyncAllModel.ProgramSync]
          }
      }

      class BorderRenderer extends DefaultTableCellRenderer {
        override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): java.awt.Component = {
          val r = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).asInstanceOf[DefaultTableCellRenderer]
          r.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2))
          r
        }
      }

      object DefaultRenderer extends BorderRenderer

      abstract class StateRenderer extends BorderRenderer {
        override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): java.awt.Component = {
          val r = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).asInstanceOf[DefaultTableCellRenderer]
          val s = value.asInstanceOf[SyncAllModel.State]
          initRenderer(r, s)
          r
        }

        protected def initRenderer(r: DefaultTableCellRenderer, s: SyncAllModel.State): Unit
      }

      object LightBulbRenderer extends StateRenderer {
        override def initRenderer(r: DefaultTableCellRenderer, s: SyncAllModel.State): Unit = {
          import SyncAllModel.State._
          val color = s match {
                        case ProgramStatusUpdating => "grey"
                        case PendingSync(_)        => "orange"
                        case SyncInProgress(_)     => "grey"
                        case SyncFailed(_)         => "red"
                        case InSync                => "green"
                        case Conflicts             => "red"
                      }
          r.setIcon(Resources.getIcon(s"bullet/bullet_$color.png"))
          r.setText("")
        }
      }

      object DetailRenderer extends StateRenderer {
        override def initRenderer(r: DefaultTableCellRenderer, s: SyncAllModel.State): Unit = {
          import SyncAllModel.State._

          val iconName = s match {
            case Conflicts     => some("vcs/vcs_up_conflict.png")
            case SyncFailed(_) => some("eclipse/error.gif")
            case _             => none[String]
          }
          r.setIcon(iconName.map(n => Resources.getIcon(n)).orNull)

          def programStatusText(vc: VersionComparison): String = vc match {
            case Conflicting => "Incoming, Outgoing"
            case Older       => "Incoming"
            case Newer       => "Outgoing"
            case Same        => "In Sync"
          }

          val text = s match {
            case ProgramStatusUpdating => "Pending ..."
            case PendingSync(vc)       => programStatusText(vc)
            case SyncInProgress(vc)    => programStatusText(vc)
            case SyncFailed(_)         => "Sync Problem"
            case InSync                => programStatusText(Same)
            case Conflicts             => "Conflicts"
          }
          r.setText(text)

          val (style, color) = s match {
            case ProgramStatusUpdating => (Font.ITALIC, Color.gray)
            case _                     => (Font.PLAIN,  Color.black)
          }
          r.setFont(getFont.deriveFont(style))
          r.setForeground(color)
        }
      }

      contents = scala.swing.Component.wrap(table)
    }

    val programDetail = new Label("Double-click to open in a program editor.") {
      border = BorderFactory.createEmptyBorder(2,2,2,2)
      opaque = true
      background = OtColor.BANANA
      foreground = Color.BLACK
      horizontalAlignment = Alignment.Left
    }

    val feedback = new Label(" ") {
      horizontalAlignment = Alignment.Left
    }

    object Buttons extends GridBagPanel {
      border = CompoundBorder(MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), EmptyBorder(10, 0 , 0, 0))

      class quitButton(close: Boolean, title: String) extends Button(title) {
        reactions += {
          case ActionEvent(_) => quit(close)
        }
      }

      object dontAsk extends CheckBox("Don't warn me about unsynchronized changes again.") {
        font       = font.deriveFont(font.getSize - 2)
        foreground = Color.darkGray
      }

      if (mode == SyncMode.ClosePrompt) {
        // Allow the user to opt-out of pestering.
        layout(dontAsk) = new Constraints() {
          gridx     = 0
          anchor    = West
          insets    = new Insets(0,0,0,5)
        }
      }

      layout(HGlue) = new Constraints() {
        gridx   = 1
        weightx = 1.0
        fill    = Horizontal
      }


      layout(new Button(SyncAction) { focusable = false }) = new Constraints() {
        gridx  = 2
        insets = new Insets(0, 0, 0, 5)
      }

      val closeWithoutSync = new quitButton(true, "Close Without Sync") {
        focusable = false
      }
      if (mode == SyncMode.ClosePrompt) {
        layout(closeWithoutSync) = new Constraints() {
          gridx = 3
          insets = new Insets(0, 0, 0, 5)
        }
      }

      val cancel = new quitButton(false, "Cancel")
      layout(cancel) = new Constraints() {
        gridx = 4
      }
      defaultButton = cancel
    }

    def hConstraints(y: Int, topGap: Int, bottomGap: Int): Constraints = new Constraints() {
      gridy   = y
      fill    = Horizontal
      weightx = 1.0
      insets  = new Insets(topGap, 0, bottomGap, 0)
      anchor  = West
    }

    layout(Instructions)  = hConstraints(0, 0, 10)
    layout(ProgTable)     = new Constraints() {
      gridy   = 1
      fill    = Both
      weightx = 1.0
      weighty = 1.0
    }
    layout(programDetail) = hConstraints(2, 2, 10)
    layout(feedback)      = hConstraints(3, 0, 10)
    layout(Buttons)       = hConstraints(4, 0,  0)
  }


  def updateProgramDetail(): Unit = {
    import SyncAllModel.State._
    val (color, text) =
      Contents.ProgTable.table.selectedProgram.fold((OtColor.BANANA, "Select row to see details, double click to open in an editor.")) { ps =>
        val pid  = ps.pid
        val site = selectedPeer.displayName
        ps.state match {
          case ProgramStatusUpdating =>
            (OtColor.BANANA, s"Looking up version information for $pid ...")
          case PendingSync(_)        =>
            (OtColor.BANANA, s"$pid is out of sync with $site.")
          case SyncInProgress(_)     =>
            (OtColor.BANANA, s"$pid is being synchronized with $site.")
          case SyncFailed(f)         =>
            (OtColor.LIGHT_SALMON, f.fold("Sync failed, check your connection or try again later.")(v => VcsFailure.explain(v, ps.pid, "sync", Some(selectedPeer))))
          case InSync                =>
            (OtColor.HONEY_DEW, s"$pid is in sync with $site.")
          case Conflicts             =>
            (OtColor.LIGHT_SALMON, s"$pid has conflicts which must be resolved before syncing.")
        }
      }
    Contents.programDetail.text       = text
    Contents.programDetail.background = color
  }

  private def updateUi(): Unit = {
    lazy val isTerminal = model.programs.forall(_.state.isTerminal)
    lazy val isSuccess  = isTerminal && model.forallState {
      case SyncAllModel.State.InSync => true
    }
    lazy val syncInProgress = model.existsState {
      case SyncAllModel.State.SyncInProgress(_) => true
    }
    lazy val programStatusUpdateInProgress = model.existsState {
      case SyncAllModel.State.ProgramStatusUpdating => true
    }

    val peerName = selectedPeer.displayName

    def updateInstructions(): Unit = {
      val singleProgramId = model.programs match {
        case Vector(ps) => some(ps.pid)
        case _          => none[SPProgramID]
      }

      Contents.Instructions.textArea.text = (mode, isTerminal, isSuccess) match {
          case (SyncMode.All, false, _)            =>
              s"Programs marked with an orange circle in the table below are out of sync with the $peerName database.  Click 'Sync All' to synchronize them now, or 'Cancel' to return to the editor."
          case (SyncMode.All, true, false)         =>
            s"Some programs could not be synchronized with $peerName at this time.  Click 'Ok' to return to the editor."
          case (SyncMode.All, true, true)          =>
            s"All programs have been successfully synchronized with $peerName."
          case (SyncMode.ClosePrompt, false, _)    =>
            singleProgramId.fold {
              s"The indicated programs are open in an editor, but changes haven't been stored at $peerName.  Click 'Sync All' to synchronize them now, 'Close Anyway' to close without saving, or 'Cancel' to return to the editor."
            } { pid =>
              s"$pid is open in an editor, but changes to it haven't been stored at $peerName.  Click 'Sync All' to synchronize it now, 'Close Anyway' to close without saving, or 'Cancel' to return to the editor."
            }
          case (SyncMode.ClosePrompt, true, false) =>
            singleProgramId.fold {
              s"Some programs could not be synchronized with $peerName at this time.  Click 'Close Anyway' to close them or 'Cancel' to return to the editor."
            } { pid =>
              s"$pid could not be synchronized with $peerName at this time.  Click 'Close Anyway' to close it or 'Cancel' to return to the editor."
            }
          case (SyncMode.ClosePrompt, true, true)  =>
            singleProgramId.fold {
              s"All programs have been successfully synchronized with $peerName.  Click 'Close' to close them or 'Cancel' to leave them open and return to the editor."
            } { pid =>
              s"$pid has been successfully synchronized with $peerName.  Click 'Close' to close it or 'Cancel' to leave it open and return to the editor."
            }
        }
    }

    def updateFeedback(): Unit = {
      val text = if (syncInProgress) some("Synchronizing changes ...")
                 else if (programStatusUpdateInProgress) some("Updating program status ... ")
                 else none[String]
      val icon = text.map(_ => Resources.getIcon("spinner16.gif"))
      Contents.feedback.icon = icon.orNull
      Contents.feedback.text = text.getOrElse(" ")
    }

    updateInstructions()
    Contents.ProgTable.tableModel.update(model)
    updateProgramDetail()
    updateFeedback()

    SyncAction.enabled = model.syncEnabled

    if (mode == SyncMode.ClosePrompt) {
      Contents.Buttons.closeWithoutSync.text    = if (isSuccess) "Close" else "Close Anyway"
      Contents.Buttons.closeWithoutSync.enabled = !syncInProgress
    }

    Contents.Buttons.cancel.text =
      if ((mode == SyncMode.All) && isTerminal) "Ok" else "Cancel"
  }

  contents = Contents

  updateUi()

  Contents.Buttons.cancel.peer.grabFocus()
}
