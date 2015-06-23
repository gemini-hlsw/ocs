package jsky.app.ot.vcs

import edu.gemini.spModel.core.{SPProgramID, Peer}
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.vcs2.VcsOtClient
import jsky.app.ot.viewer.SPViewer

import java.awt.{Point, Toolkit, Color}
import javax.swing.{UIManager, BorderFactory}

import scala.collection.JavaConverters._
import scala.swing._
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill._
import scala.swing.ListView.Renderer
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import scala.swing.event.SelectionChanged

import scalaz._
import Scalaz._

/**
 * A dialog that will prompt the user which peer to synchronize a given program
 * id with and, if confirmed, save the selection in the VcsRegistrar.
 */
object VcsPeerSelectionDialog {
  def formatterForPeers(allPeers: List[Peer]): (Peer => String) =
    if (allPeers.filter(_.site != null).map(_.site).toSet.size == allPeers.size) (p: Peer) => p.site.displayName
    else (p: Peer) => {
      val hostPort = s"${p.host}:${p.port}"
      Option(p.site).map(_.name).map { s => s"$s ($hostPort)" } | hostPort
    }

  private val separatorBorder =
    CompoundBorder(MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), EmptyBorder(10, 0 , 0, 0))

  def prompt(c: Option[Component], pid: SPProgramID, allPeers: List[Peer]): Option[Peer] = {
    val dialog = new VcsPeerSelectionDialog(c, pid, allPeers)
    dialog.open()
    dialog.result
  }

  def promptAndSet(c: Option[Component], pid: SPProgramID, allPeers: List[Peer]): Unit =
    for (client <- VcsOtClient.ref; p <- prompt(c, pid, allPeers)) {
      client.reg.register(pid, p)
    }
}

import VcsPeerSelectionDialog._

class VcsPeerSelectionDialog(c: Option[Component], pid: SPProgramID, allPeers: List[Peer]) extends Dialog {

  title     = "Select Remote Database"
  modal     = true
  resizable = false

  private def loc(comp: Option[java.awt.Component]): Point = {
    def safeLocation(c0: java.awt.Component): Option[Point] =
      \/.fromTryCatch(c0.getLocationOnScreen).toOption

    def viewer: Option[SPViewer] =
      SPViewer.instances().asScala.find { v =>
        Option(v.getProgram).exists(_.getProgramID == pid)
      }

    def center: Point =
      Toolkit.getDefaultToolkit.getScreenSize |> { dim =>
        new Point(dim.getWidth.toInt/2, dim.getHeight.toInt/2)
      }

    (comp.flatMap(safeLocation) orElse viewer.flatMap(safeLocation)) | center
  }

  location = loc(c.map(_.peer))

  private def peerForDefaultSite = Option(pid.site).flatMap { site =>
    allPeers.find(p => Option(p.site).exists(_ == site))
  }

  private var choice = peerForDefaultSite orElse ObservingPeer.get orElse allPeers.headOption

  var result = Option.empty[Peer]

  private val peerFormatter = formatterForPeers(allPeers)
  private val allSorted     = allPeers.sortBy(peerFormatter)

  private val ok     = new Button("Ok")
  private val cancel = new Button("Cancel")
  private val combo  = new ComboBox(allPeers) {
    renderer = Renderer(peerFormatter)
    choice.foreach { selection.item = _ }
  }

  trait Content {
    def message: String
    def panel: Panel
    def buttons: List[Button] = List(ok, cancel)
  }

  class ContentPanel(cnt: Content) extends GridBagPanel {
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    private def makeNote(msg: String): TextArea =
      new TextArea(2, 35) {
        editable = false
        opaque   = false
        lineWrap = true
        wordWrap = true
        text     = msg
        peer.setHighlighter(null)
      }

    protected def btnPanel(btns: List[Button]): Panel = new GridBagPanel {
      border = separatorBorder

      layout(HGlue) = new Constraints() {
        gridx   = 0
        weightx = 1.0
        fill    = Horizontal
      }

      layout(btns.head) = (1, 0)

      btns.tail.zipWithIndex.foreach { case (btn, index) =>
        layout(btn) = new Constraints() {
          gridx  = index + 2
          insets = new Insets(0, 5, 0, 0)
        }
      }
    }

    val warnIcon = new Label {
      text = ""
      icon = UIManager.getIcon("OptionPane.warningIcon")
    }
    layout(warnIcon) = new Constraints() {
      gridx      = 0
      gridy      = 0
      gridheight = 2
      insets     = new Insets(0, 0, 0, 10)
    }

    layout(makeNote(cnt.message)) = new Constraints() {
      gridx     = 1
      gridy     = 0
      fill      = Horizontal
      anchor    = West
      insets    = new Insets(0, 0, 5, 0)
    }

    layout(cnt.panel) = new Constraints() {
      gridx     = 1
      gridy     = 1
      anchor    = West
    }

    layout(btnPanel(cnt.buttons)) = new Constraints() {
      gridx     = 0
      gridy     = 2
      gridwidth = 2
      fill      = Horizontal
      insets    = new Insets(10,0,0,0)
    }
  }

  def noPeers = new Content {
    def message = "This program was not fetched from a remote database.  Add one using the menu item 'Edit->Manage Keys ...' and then try again."
    def panel   = new BorderPanel
    override def buttons = List(ok)
  }

  def onePeer(p: Peer) = new Content {
    def message = "This program was not fetched from a remote database."
    def panel   = new GridBagPanel {
      layout(new Label(s"Synchronize ${pid} with")) = new Constraints() {
        gridx  = 0
        insets = new Insets(0, 0, 0, 5)
      }

      layout(new Label(peerFormatter(p))) = (1,0)
      layout(new Label("?")) = (2, 0)
    }
  }

  def multiplePeers = new Content {
    def message = "This program was not fetched from a remote database.  Select the database with which to synchronize."

    def panel   = new GridBagPanel {
      layout(new Label(s"Synchronize ${pid} with")) = new Constraints() {
        gridx  = 0
        insets = new Insets(0, 0, 0, 5)
      }

      layout(combo) = (1, 0)
    }
  }

  contents = new ContentPanel(choice.fold[Content](noPeers) { c =>
    if (allPeers.size == 1) onePeer(c) else multiplePeers
  })

  def closeAndDispose(): Unit = {
      close()
      dispose()
  }

  listenTo(ok)
  listenTo(cancel)
  listenTo(combo.selection)

  reactions += {
    case ButtonClicked(`ok`) =>
      result = choice
      closeAndDispose()
    case ButtonClicked(`cancel`) =>
      closeAndDispose()
    case SelectionChanged(`combo`) =>
      choice = Some(combo.selection.item)
  }

  this.peer.getRootPane.setDefaultButton(ok.peer)
}
