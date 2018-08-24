package edu.gemini.qpt

import java.io.File

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.ictd.IctdDatabase
;
import edu.gemini.qpt.ui.ShellAdvisor
import edu.gemini.qpt.ui.action.PublishAction
import edu.gemini.spModel.core.{Peer, Site, Version}
import edu.gemini.ui.workspace.impl.Workspace
import edu.gemini.util.trpc.auth.TrpcKeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.ext.auth.ui.AuthDialog

/** Standalone launcher for running QPT inside an IDE. */
object QptTestLauncher {

  def main(args: Array[String]): Unit = {

    // Show the database tab in the key manager
    AuthDialog.showDatabaseTab = true

    // We need a KeyChain to find peers.
    val keys = {
      val peers = List(new Peer("localhost", 8443, Site.GS))      // GS will point to Localhost
      val dir   = new File("/tmp/qpt-testlauncher"); dir.mkdirs() // Keychain storage in tempdir
      TrpcKeyChain(new File(dir, "keys.ser"), peers).unsafeRunAndThrow
    }

    // This needs to be somewhere else … doesn't work. Just used for help text so meh.
    val root = new File(System.getProperty("user.dir")).toURI().toString();

    // These need to be refined if we want to test publishing. See the QPT activator for examples.
    val internal = new PublishAction.Destination("host", "user", "fs-root", "url-root")
    val pachon   = new PublishAction.Destination("host", "user", "fs-root", "url-root")

    // Window title, so we won't get confused
    val title = "QPT Test Launcher"

    // The QPT UI handler
    val adv = new ShellAdvisor(title, Version.current.toString(), root, keys, internal, pachon, ProbeLimitsTable.loadOrThrow())

    // Workspace to manage windows
    val ws = new Workspace(null)
    ws.open()

    // Add a shell. When the last one closes everything shuts down.
    ws.createShell(adv).open()

  }

}
