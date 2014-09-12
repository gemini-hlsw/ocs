package edu.gemini.sp.vcs.server.osgi

import edu.gemini.sp.vcs.VcsServer
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.security.auth.keychain.KeyChain
import org.osgi.framework._
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.sp.vcs.tui.osgi._
import edu.gemini.util.osgi.SecureServiceFactory
import edu.gemini.sp.vcs.server.VcsServerImpl
import java.security.Principal
import scala.collection.JavaConverters._
import java.util.Dictionary

object Activator {
  val COMMAND_SCOPE    = "osgi.command.scope"
  val COMMAND_FUNCTION = "osgi.command.function"
}

class Activator extends BundleActivator {
  import Activator._
  
  private var tracker1: ServiceTracker[_,_] = null
  private var tracker2: ServiceTracker[_,_] = null

  override def start(ctx: BundleContext) {

    tracker1 = track[IDBDatabaseService, VcsLog, ServiceRegistration[_]](ctx) { (odb, log) =>
      val props = new java.util.Hashtable[String,String]
      props.put("trpc", "") // publish to trpc
      val factory = new SecureServiceFactory[VcsServer] {
        def getService(b: Bundle, reg: ServiceRegistration[VcsServer], ps: java.util.Set[Principal]): VcsServer =
          new VcsServerImpl(odb, log, ps.asScala.toSet)
      }
      ctx.registerService(classOf[VcsServer].getName, factory, props)
    } { _.unregister() }
    tracker1.open()

    tracker2 = track[IDBDatabaseService, VcsRegistrar, KeyChain, VcsLog, ServiceRegistration[Commands]](ctx) { (odb, reg, auth, log) =>
      val dict = new java.util.Hashtable[String, Object]()
      dict.put(COMMAND_SCOPE, "vcs")
      dict.put(COMMAND_FUNCTION, Array("vcs"))
      ctx.registerService(classOf[Commands], new CommandsImpl(odb, reg, auth, log), dict)
    } { _.unregister() }
    tracker2.open()

  }

  override def stop(ctx: BundleContext) {

    tracker1.close()
    tracker1 = null

    tracker2.close()
    tracker2 = null

  }
}

