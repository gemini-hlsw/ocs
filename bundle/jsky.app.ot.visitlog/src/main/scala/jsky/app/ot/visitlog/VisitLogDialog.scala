package jsky.app.ot.visitlog

import edu.gemini.spModel.core.Peer

import java.awt.Dimension

import scala.swing.Dialog
import scala.swing.event.WindowClosing
import edu.gemini.skycalc.ObservingNight
import edu.gemini.util.security.auth.keychain.KeyChain

class VisitLogDialog(kc: KeyChain, p: Peer) extends Dialog {
  modal         = false
  resizable     = true
  preferredSize = new Dimension(650,500)
  minimumSize   = preferredSize

  VisitLogPanel.viewerService.map(_.registerView(this))
  val client = VisitLogClient(kc, p)

  listenTo(client)
  reactions += {
    case s: VisitLogClient.State =>
      title = s"Visits ${s.night.getSite.name} ${s.night.getNightString}"
    case WindowClosing(e) =>
      VisitLogPanel.viewerService.map(_.unregisterView(this)) // unregister frame when frame is closed

  }

  def load(night: ObservingNight): Unit = {
    contents = new VisitLogPanel(client, this)
    client.load(night)
  }
}