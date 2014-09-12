package edu.gemini.phase2.template.factory.osgi

import edu.gemini.phase2.template.factory.api.TemplateFactory
import edu.gemini.phase2.template.factory.impl.{TemplateFactoryImpl, TemplateDb}

import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import java.util.Hashtable
import java.util.logging.Logger
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

object Activator {
//  val TEMPLATE_DIR_PROP = "edu.gemini.phase2.template.db"
  val Log = Logger.getLogger(classOf[Activator].getName)
}
import Activator._

/**
 * Activator that registers a TemplateGroupFactory service upon startup.
 */
class Activator extends BundleActivator {

  private var service: Option[ServiceRegistration[TemplateFactory]] = None

  // we run as superuser
  val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

//  private def templateDir(ctx: BundleContext): File =
//    Option(ctx.getProperty(Activator.TEMPLATE_DIR_PROP)) map { p => new File(p) } getOrElse {
//      sys.error("Missing parameter '%s'".format(Activator.TEMPLATE_DIR_PROP))
//    }

//  private def storageDir(ctx: BundleContext): File = {
//    val dir = new File(ctx.getDataFile(""), "odb")
//    if (dir.exists()) {
//      if (!dir.isDirectory) {
//        sys.error("Storage dir is not a directory: " + dir)
//      }
//      clear(dir)
//    } else {
//      dir.mkdirs()
//    }
//    dir
//  }

//  private def clear(d: File) {
//    d.listFiles() foreach { f =>
//      if (f.isDirectory) clear(f)
//      f.delete()
//    }
//  }

  def start(ctx: BundleContext) {
//    val dir = templateDir(ctx)
//    if (!dir.exists || !dir.canRead) {
//      sys.error("Template program directory missing or unreadable: %s".format(dir.getPath))
//    }

    // Do this in the background
    scala.actors.Actor.actor {
      TemplateDb.load(user) match {
        case Left(msg) =>
          Log.severe("Could not load the template database: " + msg)
        case Right(db) =>
          val fact = TemplateFactoryImpl(db)
          service = Some(ctx.registerService(classOf[TemplateFactory], fact, new Hashtable[String,Object]()))
          Log.info("Loaded template database.")
      }
    }

  }

  def stop(ctx: BundleContext) {
    service foreach { _.unregister() }
  }

}
