package jsky.app.ot.scilib

import scalaz._
import Scalaz._
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.spModel.core._
import jsky.app.ot.OT
import edu.gemini.pot.sp.ISPProgram
import jsky.app.ot.vcs.VcsGui
import edu.gemini.pot.client.SPDB
import jsky.app.ot.viewer.open.OpenDialog
import javax.swing.JComponent

object ScienceLibraryHelper {

  /** Return peer for the specified site, OR NULL !! */
  def peerForSite(s:Site):Peer =
    OT.getKeyChain.peerForSite(s).unsafeRun.fold(_ => null, _.orNull)

  def checkout(peer:Peer, pid:SPProgramID, checkedOut: Boolean):ISPProgram = {

    val reg = VcsGui.registrar.get
    val db = SPDB.get()
    val auth = OT.getKeyChain

    {
      if (checkedOut) OpenDialog.update(db, pid, peer, null : JComponent, reg)
      else OpenDialog.checkout(db, pid, peer, null : JComponent, reg)
    }.asInstanceOf[ISPProgram]

  }

}
