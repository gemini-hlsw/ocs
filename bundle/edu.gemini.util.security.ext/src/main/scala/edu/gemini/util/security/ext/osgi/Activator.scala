package edu.gemini.util.security.osgi

import org.osgi.framework.{BundleContext, BundleActivator}
import java.security.{Principal, Policy}
import edu.gemini.util.osgi.Tracker._
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.pot.spdb.{ProgramEvent, ProgramEventListener, IDBDatabaseService}
import edu.gemini.util.security.ext.auth.ui.AuthDialog
import java.io.File
import edu.gemini.spModel.core.{Site, Version, OcsVersionUtil}
import scala.Some
import edu.gemini.util.security.auth.keychain.{KeyService, KeyMailer, KeyServer}
import edu.gemini.util.security.auth.keychain.Action._
import java.util.logging.Logger
import edu.gemini.util.osgi.ExternalStorage.getExternalDataFile
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.util.security.principal.StaffPrincipal
import edu.gemini.util.security.auth.ProgIdHash

class Activator extends BundleActivator {

  private val Log = Logger.getLogger(getClass.getName)

  val SHOW_DATABASE_TAB_PROP = "edu.gemini.util.security.auth.ui.showDatabaseTab"
  val START_SERVER_PROP      = "edu.gemini.util.security.auth.startServer"
  val PROGID_HASH_KEY        = "edu.gemini.spModel.gemini.obscomp.key"
  val COMMAND_SCOPE          = "osgi.command.scope"
  val COMMAND_FUNCTION       = "osgi.command.function"
  val PUBLISH_TRPC           = "trpc"
  val SMTP_PROP              = "cron.odbMail.SITE_SMTP_SERVER" // for now, sorry

  // we run as superuser
  private val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  private var ks: KeyServer = null
  private var dbTracker: Option[ServiceTracker[IDBDatabaseService, _]] = None

  def start(ctx: BundleContext) {

    // Configure the database tab
    AuthDialog.showDatabaseTab = ctx.getProperty(SHOW_DATABASE_TAB_PROP) != null

    // Start a server if we have been asked to do so
    if (ctx.getProperty(START_SERVER_PROP) != null) {

      // Get our database directory set up
      val dir = keyDir(ctx)
      dir.mkdirs()
      Log.info(s"KeyServer storage is at ${dir.getAbsolutePath}")

      // Construct our KeyServer and publish its Service (only)
      ks = KeyServer.apply(dir, keyMailer(ctx)).unsafePerformIO // YOLO
      ctx.registerService(classOf[KeyServer], ks, null)

      // Publish just the public service portion to TRPC
      val props = new java.util.Hashtable[String, Object]()
      props.put(PUBLISH_TRPC, "true")
      ctx.registerService(classOf[KeyService], ks.keyService, props)

      // ProgId hash, for creating program passwords
      val progIdHashKey = ctx.getProperty(PROGID_HASH_KEY)
      require (progIdHashKey != null, s"Property $PROGID_HASH_KEY was not specified.")
      val progIdHash = new ProgIdHash(progIdHashKey)

      // Key commands
      val dict = new java.util.Hashtable[String, Object]()
      dict.put(COMMAND_SCOPE, "key")
      dict.put(COMMAND_FUNCTION, Array("key"))
      ctx.registerService(classOf[KeyCommands], new KeyCommandsImpl(ks, ctx, user, progIdHash), dict)

      // Watch for new programs and add the default program key when created.
      dbTracker = Some(track[IDBDatabaseService,(IDBDatabaseService,ProgramEventListener[ISPProgram])](ctx) { db =>
        val listener = new ProgramKeyInitializer(new ProgramKeySetter(ks, ctx, user, progIdHash))
        db.addProgramEventListener(listener)
        (db,listener)
      } { case (db, listener) => db.removeProgramEventListener(listener) })
      dbTracker.foreach(_.open())
    }

  }

  // Construct a mailer. If there's no SMTP server in the config, we'll just use a test mailer.
  def keyMailer(ctx: BundleContext): KeyMailer =
    (Option(ctx.getProperty(SMTP_PROP)), Option(Site.currentSiteOrNull)) match {
      case (Some(host), Some(site)) =>
        Log.info(s"KeyServer using $host ad $site for mailing passwords.")
        KeyMailer(site, host)
      case (h, s) =>
        Log.warning(s"KeyServer using test mailer (site and host were $h and $s)")
        KeyMailer.forTesting(Site.GS)
    }

  def keyDir(ctx: BundleContext): File = {
    val BUNDLE_PROP_DIR = "edu.gemini.spdb.dir" // Same location as the SPDB
    val root:File = Option(ctx.getProperty(BUNDLE_PROP_DIR)).fold(getExternalDataFile(ctx, "spdb"))(new File(_))
    val dir:File = new File(OcsVersionUtil.getVersionDir(root, Version.current), "keyserver")
    dir
  }

  def stop(ctx: BundleContext) {

    // Untrack the DB
    dbTracker.foreach(_.close())
    dbTracker = None

    // Shut down keyserver
    Option(ks).foreach(_.backup(new File(keyDir(ctx), "keydb.backup")).unsafeRunAndThrow)
    ks = null

  }

  class ProgramKeyInitializer(pks: ProgramKeySetter) extends ProgramEventListener[ISPProgram] {

    def programAdded(evt: ProgramEvent[ISPProgram]): Unit =
      Option(evt.getNewProgram.getProgramID).foreach { pid =>
        Log.info(s"initial program key: ${pks.setKey(pid).run.unsafePerformIO().fold(identity, identity)}")
      }

    def programReplaced(evt: ProgramEvent[ISPProgram]): Unit = ()
    def programRemoved(evt: ProgramEvent[ISPProgram]): Unit = ()

  }

}

