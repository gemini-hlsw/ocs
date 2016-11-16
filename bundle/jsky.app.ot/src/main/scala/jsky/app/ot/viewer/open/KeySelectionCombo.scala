package jsky.app.ot.viewer.open

import javax.swing.DefaultComboBoxModel

import edu.gemini.spModel.core.Peer
import edu.gemini.util.security.auth.keychain._
import edu.gemini.util.security.auth.keychain.Action._

import scala.swing.ComboBox
import scala.swing.ListView.Renderer
import scala.swing.Reactions._
import scala.swing.event.SelectionEvent

class KeySelectionCombo(auth: KeyChain, keyChangedOp: edu.gemini.shared.util.immutable.ApplyOp[Option[(Peer, Key)]]) extends ComboBox[Option[(Peer, Key)]](List.empty[Option[(Peer, Key)]]) {
  private object Model extends DefaultComboBoxModel[Option[(Peer, Key)]] {
    def allKeys: List[(Peer, Key)] =
      auth.keys.unsafeRun.fold(_ => List.empty[(Peer, Key)], _.toList.flatMap {
        case (p, s) => Stream.continually(p).zip(s).toList
      })

    def keyDisplay(tup: (Peer, Key)): String = {
      val (p,k) = tup
      val principal = k.get._1
      s"${principal.getName} (${p.displayName})"
    }

    def selectKeyFromComboBox(): Unit = {
      val newSel = Option(getSelectedItem.asInstanceOf[Option[(Peer, Key)]]).flatten
      auth.select(newSel).unsafeRun
      keyChangedOp.apply(newSel)
    }

    def refresh(): Unit = {
      removeAllElements()

      val ks = allKeys.sortBy(k => keyDisplay(k).toLowerCase).map(Some(_))
      (Option.empty[(Peer, Key)] :: ks).foreach { x: Option[(Peer, Key)] => addElement(x) }

      val sel = auth.selection.unsafeRun.fold(_ => None, identity)
      setSelectedItem(sel)
    }
  }

  peer.setModel(Model)
  renderer = Renderer { _.map(Model.keyDisplay).getOrElse("None") }

  val selectionReaction: Reaction = {
    case _: SelectionEvent => Model.selectKeyFromComboBox()
  }

  def refresh(): Unit = {
    selection.reactions -= selectionReaction
    Model.refresh()
    enabled = !auth.isLocked.unsafeRun.getOrElse(false)
    selection.reactions += selectionReaction
  }
}
