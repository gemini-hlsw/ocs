package jsky.app.ot.testlauncher

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.qv.plugin.{QvTool, ShowQvToolAction}
import edu.gemini.sp.vcs2.{Vcs, VcsServer}
import jsky.app.ot.gemini.obscat.OTBrowserPresetsPersistence
import jsky.app.ot.vcs.VcsOtClient
import jsky.app.ot.viewer.plugin.PluginRegistry

import scalaz._
import Scalaz._
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.util.trpc.auth.TrpcKeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.ext.auth.ui.{AuthDialog, PasswordDialog}
import java.io.File
import java.security.{Provider, Security}
import java.util.logging.{Level, Logger}

import edu.gemini.catalog.image.ImageCacheWatcher
import edu.gemini.sp.vcs.reg.impl.VcsRegistrarImpl
import edu.gemini.pot.client.SPDB
import jsky.catalog.skycat.SkycatConfigFile
import jsky.app.ot.OT
import jsky.app.ot.viewer.ViewerService
import jsky.app.ot.visitlog.ShowVisitLogAction
import edu.gemini.spModel.core._

object TestLauncher extends App {

  // Store everything in /tmp; clean it up by hand as needed.
  val dir   = new File("/tmp/ot-testlauncher") <| (_.mkdirs)
  val peers = List(new Peer("localhost", 8443, Site.GS))
  val keys  = TrpcKeyChain(new File(dir, "keys.ser"), peers).unsafeRunAndThrow
  val reg   = new VcsRegistrarImpl(new File(dir, "vcs-reg.xml"))
  val odb   = DBLocalDatabase.create(new File(dir, "spdb")) <| SPDB.init

  // Initialize the image cache
  ImageCacheWatcher.run()

  // Irritation, then open the OT
  AuthDialog.showDatabaseTab = true
  SkycatConfigFile.setConfigFile(classOf[SkycatConfigFile].getResource("/jsky/catalog/osgi/skycat.cfg"))
  ViewerService.instance = Some(new ViewerService(odb, reg))

  val vcsServer   = new VcsServer(odb)
  val vcs         = Vcs(keys, vcsServer)
  VcsOtClient.ref = Some(VcsOtClient(vcs, reg))

  // Register Plugins
  PluginRegistry.add(new ShowVisitLogAction())
  PluginRegistry.add(new ShowQvToolAction())

  // Run!
  if (keys.isLocked.unsafeRunAndThrow) {
    PasswordDialog.unlock(keys, null)
    if (keys.isLocked.unsafeRunAndThrow) System.exit(0)
  }
  OT.open(keys, ProbeLimitsTable.loadOrThrow(), reg, new File(dir, "ot-storage"))

  // Initialize query browser history
  // NOTE must be de done after starting the OT
  OTBrowserPresetsPersistence.dir = Some(dir)
  OTBrowserPresetsPersistence.load()

  // You can pass an argument -program=PROGID to autolaunch the given program
  // Do simple parsing, errors on argument format or program id will be ignored
  val programArgRegex = "-program=(.*)".r
  args.foreach {
    case programArgRegex(programID) => ViewerService.instance.map(_.loadAndView(SPProgramID.toProgramID(programID)))
    case _                          =>
  }

}
