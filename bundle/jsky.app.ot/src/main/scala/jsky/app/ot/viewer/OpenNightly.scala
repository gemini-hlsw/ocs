package jsky.app.ot.viewer

import jsky.app.ot.vcs.VcsOtClient

import javax.swing.AbstractAction
import jsky.util.gui.{DialogUtil, BusyWin}
import edu.gemini.spModel.core.{SPProgramID, Peer, Site}
import jsky.app.ot.userprefs.observer.ObservingSite
import jsky.app.ot.{OTOptions, OT}
import edu.gemini.pot.client.SPDB
import jsky.app.ot.shared.spModel.util.CreateOrVerifyFunctor
import jsky.app.ot.viewer.open.OpenDialog
import java.awt.event.ActionEvent
import scala.swing.Component
import edu.gemini.spModel.util.NightlyProgIdGenerator
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.util.security.auth.keychain.Action._

/** Actions that open a nightly plan or program either locally (if we already have it), or by creating it in the remote
  * db based on site affinity (if needed) and then checking out and opening locally. */
abstract class OpenNightly(name: String) extends AbstractAction(name) {

  def actionPerformed(evt: ActionEvent): Unit =
    ObservingSite.getOrPrompt.foreach { site =>
      BusyWin.showBusy()
      val pid = getProgramId(site)
      lookupLocal(pid).fold(openFromSite(site, pid))(openInViewer)
    }

  def openFromSite(site: Site, pid: SPProgramID): Unit =
    OT.getKeyChain.peerForSite(site).unsafeRunAndThrow.fold(errNoPeer())(checkout(pid, _).foreach(openInViewer))

  def errNoPeer(): Unit =
    DialogUtil.error("There is no peer configured for ${site.displayName}.\nYou can add one in the Key Manager.")

  def openInViewer(n: ISPProgram): Unit =
    ViewerManager.open(n)

  def checkout(pid: SPProgramID, peer: Peer): Option[ISPProgram] = {
    CreateOrVerifyFunctor.execute(OT.getKeyChain, pid, peer)
    OpenDialog.checkout(SPDB.get, pid, peer, null.asInstanceOf[Component], VcsOtClient.unsafeGetRegistrar)
  }

  // Subclasses need to implement
  def getProgramId(site: Site): SPProgramID
  def lookupLocal(pid: SPProgramID): Option[ISPProgram]
}

object OpenNightly {

  import NightlyProgIdGenerator._

  class OpenNightlyProgram(name: String, f: Site => SPProgramID) extends OpenNightly(name) {
    setEnabled(OTOptions.isStaffGlobally)
    def getProgramId(site: Site): SPProgramID = f(site)
    def lookupLocal(pid: SPProgramID): Option[ISPProgram] = Option(SPDB.get.lookupProgramByID(pid))
  }

  def Calibration = new OpenNightlyProgram("Open Calibration Program", getCalibrationID)
  def Engineering = new OpenNightlyProgram("Open Engineering Program", getEngineeringID)
}