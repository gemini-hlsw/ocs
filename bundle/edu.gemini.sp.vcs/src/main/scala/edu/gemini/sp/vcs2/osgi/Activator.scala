package edu.gemini.sp.vcs2.osgi

import java.security.Principal
import java.util

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs2.{Vcs, VcsServer, VcsService}
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.util.osgi.SecureServiceFactory
import edu.gemini.util.osgi.SecureServiceFactory._
import edu.gemini.util.security.auth.keychain.KeyChain
import org.osgi.framework.{Bundle, ServiceRegistration, BundleContext, BundleActivator}

import edu.gemini.util.osgi.Tracker._
import org.osgi.util.tracker.ServiceTracker

import scala.collection.JavaConverters._

object Activator {
  val COMMAND_SCOPE    = "osgi.command.scope"
  val COMMAND_FUNCTION = "osgi.command.function"
  val PUBLISH_TRPC     = "trpc"
}

class Activator extends BundleActivator{

  private var trackers: List[ServiceTracker[_,_]] = Nil

  override def start(ctx: BundleContext): Unit = {
    import Activator._

    trackers = List(
      track[IDBDatabaseService, KeyChain, VcsLog, List[ServiceRegistration[_]]](ctx) { (odb, auth, log) =>
        // The vcs backend/server implementation itself.
        val vcsServer = new VcsServer(odb)

        // The public service.
        val factory = new SecureServiceFactory[VcsService] {
          def getService(ps: Set[Principal]): VcsService =
            new vcsServer.SecureVcsService(ps, log)
        }

        // Register the Vcs client and a secure service factory for making the
        // VcsService implementation.
        List(
          ctx.registerService(classOf[Vcs], Vcs(auth, vcsServer), null),
          ctx.registerSecureService(factory, Map(PUBLISH_TRPC -> ""))
        )
      } { _.foreach(_.unregister()) },

      // Shell commands.
      track[IDBDatabaseService, KeyChain, Vcs, VcsRegistrar, ServiceRegistration[Commands]](ctx) { (odb, auth, vcs, reg) =>
        val dict = new util.Hashtable[String, Object]()
        dict.put(COMMAND_SCOPE, "vcs2")
        dict.put(COMMAND_FUNCTION, Array("vcs2"))
        ctx.registerService(classOf[Commands], Commands.apply(odb, auth, vcs, reg), dict)
      } { _.unregister() }
    )
    trackers.foreach(_.open())
  }

  override def stop(ctx: BundleContext): Unit = {
    trackers.foreach(_.close())
    trackers = Nil
  }
}
