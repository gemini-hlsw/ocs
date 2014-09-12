package jsky.app.ot.too

import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.too.TooType
import edu.gemini.too.event.api.{TooEvent, TooSubscriber}
import edu.gemini.too.event.client.TooClient

import jsky.app.ot.{StaffBean, OTOptions, OT}
import jsky.app.ot.userprefs.observer.{ObservingPeer, ObserverPreferences}
import jsky.app.ot.userprefs.model.{PreferencesChangeEvent, PreferencesChangeListener}

import javax.swing.SwingUtilities
import java.util.logging.Logger
import java.beans.{PropertyChangeEvent, PropertyChangeListener}

object TooPoll {
  val LOG = Logger.getLogger(classOf[TooPoll].getName)

  var poll: Option[TooPoll] = None

  private def stopPolling() {
    poll foreach { p =>
      LOG.info("Stop polling %s for ToO events.".format(p.peer))
      p.stop()
    }
    poll = None
  }

  private def startPolling(p: Peer, pollPeriodMs: Long) {
    LOG.info("Start polling %s for ToO events".format(p))
    poll = Some(TooPoll(p, pollPeriodMs))
    poll foreach { _.start() }
  }

  def init(pollMs: Long) {
    // Watch for changes to the selected Site
    ObserverPreferences.addChangeListener(new PreferencesChangeListener[ObserverPreferences] {
      def preferencesChanged(evt: PreferencesChangeEvent[ObserverPreferences]) {
        val oldSite = Option(evt.getOldValue.getOrNull).map(_.observingSite)
        val newSite = Option(evt.getNewValue.observingSite)
        if (oldSite != newSite) update(pollMs)
      }
    })

    // Watch for changes to the "isStaff" property
    StaffBean.addPropertyChangeListener(new PropertyChangeListener {
      def propertyChange(evt: PropertyChangeEvent) { update(pollMs) }
    })

    update(pollMs)
  }

  private def update(pollPeriodMs: Long) {
    synchronized {
      if (!OTOptions.isStaffGlobally) stopPolling()
      else (poll.map(_.peer), ObservingPeer.get) match {
            case (None, Some(p))      => startPolling(p, pollPeriodMs)
            case (Some(p0), Some(p1)) => if (p0 != p1) {
                                           stopPolling()
                                           startPolling(p1, pollPeriodMs)
                                         }
            case _                    => stopPolling()
          }
    }
  }

  def isPolling = synchronized { poll.isDefined }
}

case class TooPoll(peer: Peer, pollPeriodMs: Long) {
  private val client = new TooClient(OT.getKeyChain, peer.host, peer.port, pollPeriodMs)
  private val sub = new TooSubscriber {
    def tooObservationReady(event: TooEvent) {
      SwingUtilities.invokeLater(new TooHandler(event, peer, null))
    }
  }

  def start() {
    client.addTooSubscriber(TooType.rapid, sub)
    client.start()
  }

  def stop() {
    client.stop()
    client.removeTooSubscriber(TooType.rapid, sub)
  }
}
