package jsky.app.ot.userprefs.observer

import edu.gemini.spModel.core.{Peer, Site}
import jsky.app.ot.OT
import edu.gemini.util.security.auth.keychain.Action._

/**
 * The "observing peer" is the peer associated with the selected "observing
 * site", if any.  See ObservingSite.
 */
object ObservingPeer {

  def get: Option[Peer] = peerFor(ObservingSite.get)
  def getOrNull: Peer = get.orNull

  def getOrPrompt: Option[Peer] = peerFor(ObservingSite.getOrPrompt)
  def getOrPromptOrNull: Peer = getOrPrompt.orNull

  /**
   * Gets any Peer with a Site, if any, but preferring the observing site if
   * set.  Used when both sites provide some service and it doesn't depend upon
   * the data in the database.
   */
  def anyWithSite: Option[Peer] =
    get orElse peerFor(Site.GN) orElse peerFor(Site.GS)

  def anyWithSiteOrNull: Peer = anyWithSite.orNull

  def peerFor(site: Site): Option[Peer] =
    OT.getKeyChain.peerForSite(site).unsafeRun.fold(_ => None, identity)

  def peerFor(site: Option[Site]): Option[Peer] = site.flatMap(peerFor)
  def peerOrNullFor(site: Site): Peer = peerFor(site).orNull
}
