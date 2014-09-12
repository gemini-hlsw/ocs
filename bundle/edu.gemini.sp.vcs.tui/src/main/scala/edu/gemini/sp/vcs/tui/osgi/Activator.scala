// package edu.gemini.sp.vcs.tui.osgi

// import edu.gemini.pot.spdb.IDBDatabaseService
// import edu.gemini.sp.vcs.reg.VcsRegistrar
// import edu.gemini.util.osgi.Tracker._
// import edu.gemini.util.security.auth.keychain.KeyChain

// import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
// import org.osgi.util.tracker.ServiceTracker
// import edu.gemini.sp.vcs.log.VcsLog


// object Activator {
//   val COMMAND_SCOPE    = "osgi.command.scope"
//   val COMMAND_FUNCTION = "osgi.command.function"
// }

// import Activator._

// class Activator extends BundleActivator {

//   private var tracker: ServiceTracker[_,_] = null

//   override def start(ctx: BundleContext) {
//     tracker = track[IDBDatabaseService, VcsRegistrar, KeyChain, VcsLog, ServiceRegistration[Commands]](ctx) { (odb, reg, auth, log) =>
//       val dict = new java.util.Hashtable[String, Object]()
//       dict.put(COMMAND_SCOPE, "vcs")
//       dict.put(COMMAND_FUNCTION, Array("vcs"))
//       ctx.registerService(classOf[Commands], new CommandsImpl(odb, reg, auth, log), dict)
//     } {
//       srv => srv.unregister()
//     }

//     tracker.open()
//   }

//   override def stop(ctx: BundleContext) {
//     tracker.close()
//     tracker = null
//   }
// }
