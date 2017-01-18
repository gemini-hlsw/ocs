package edu.gemini.util.trpc.osgi

import edu.gemini.spModel.core.{Version, OcsVersionUtil, Site, Peer}
import edu.gemini.util.osgi.ExternalStorage
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.osgi.SecureServiceFactory.BundleContextOps
import edu.gemini.util.security.auth.keychain._
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.trpc.auth._
import edu.gemini.util.trpc.server.TrpcServlet
import java.io.File
import java.security.Principal
import java.util
import org.osgi.framework.{ServiceReference, BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.{ServiceTrackerCustomizer, ServiceTracker}

// Our strategy is that we will register the server if there's an HTTP Service available, even on the client. The only
// cost is that there will be a servlet on /trpc that will return nothing but errors. I think this is probably ok, but
// if it turns out to be a problem we can wait for an HTTP Service *and* at least one other service with a "trpc" attr.

class Activator extends BundleActivator with ExternalStorage {

  // These can be null sometimes, but we'll pretend it's impossible for them to be referenced
  // when they're null because the servlet should never be registered if this is the case.
  private var context: BundleContext = null
  private var httpTracker: ServiceTracker[HttpService, HttpService] = null
  private var keyChain: KeyChain = null


  private var tracker: ServiceTracker[_, _] = null

  // The generic TrpcServlet doesn't know how to resolve a class name into a service, so we have to implement that bit.
  // The reasoning is that we have to know about OSGi over here, but the TrpcServlet doesn't.
  class Servlet(ks: KeyService) extends TrpcServlet(ks) {

    // We just look for a service of the requested class, with an extra attribute specified by the filter
    def withService[B](clazz: String, ps: Set[Principal])(f: Any => B): B =
      withNormalService(clazz, f)     orElse
      withSecureService(clazz, ps, f) getOrElse {
        throw new util.NoSuchElementException("No service of type %s available.".format(clazz))
      }

    def withSecureService[B](clazz: String, ps: Set[Principal], f: Any => B): Option[B] =
      context.withSecureServiceByName(clazz, ps, Filter)(f)

    def withNormalService[B](clazz: String, f: Any => B): Option[B] =
      Option(context.getServiceReferences(clazz, Filter.toString)).flatMap(_.headOption).map { ref =>
        try f(context.getService(ref)) finally context.ungetService(ref)
      }

  }

  def start(context: BundleContext) {
    this.context = context

    // Keychain is in permanent storage, except for SPDB in which case it lives with the other
    // data files. It's not entirely clear that this is what we want; special SPDB handling is
    // problematic generally and should be revisited. Migration steps are not provided; this will
    // be handled internally in KeyChain should the need arise.
    val file: File = {
      val spdbProp = "edu.gemini.spdb.dir" // Same as the SPDB
      val fileName = "keychain.ser"
      Option(context.getProperty(spdbProp)).fold(
        getPermanentDataFile(context, Version.current.isTest, fileName, Nil))(root =>
        new File(OcsVersionUtil.getVersionDir(new File(root), Version.current), fileName))
    }
    file.getParentFile.mkdirs()
    Log.info(s"TRPC storage is at ${file.getAbsolutePath}")

    // Load our initial peer info from bundle props.
    val initialPeers =
      for {
        site <- Site.values.toList
        desc <- Option(context.getProperty(s"edu.gemini.util.trpc.peer.${site.name}"))
        info <- toInitialPeer(desc, site)
      } yield info._1

    // What is our name (as see by other peers)?
    val oname = Option(context.getProperty("edu.gemini.util.trpc.name"))
    oname.fold(Log.info("Using default peer name.")) { n =>
      Log.info(s"Using configured peer name '${n}'")
    }

    // KeyChain
    keyChain = TrpcKeyChain.apply(file, initialPeers).unsafeRunAndThrow
    context.registerService(classOf[KeyChain], keyChain, null) // no props

    // TRPC Service
    tracker = track[KeyService, HttpService, HttpService](context) { (ks, http) =>
      http.registerServlet(Alias, new Servlet(ks), new util.Hashtable[Any, Any], null)
      Log.info("Registered TRPC service at %s".format(Alias))
      http
    } { _.unregister(Alias) }
    tracker.open()

  }

  def toInitialPeer(s: String, site:Site): Option[(Peer, String)] =
    try {
      val Array(h, p, n) = s.split(":")
      val info = (new Peer(h, p.toInt, site), n)
      Log.info("Initial peer: " + info)
      Some(info)
    } catch {
      case e: Exception =>
        Log.warning("Could not parse initial peer description: " + s)
        None
    }

  def stop(context: BundleContext) {
    tracker.close()
    tracker = null
  }

  // Get a service, do something and unget it
  def withRef[A, B](f: A => B)(ref: ServiceReference[A]): B = try {
    f(context.getService(ref))
  } finally {
    context.ungetService(ref)
  }

}
