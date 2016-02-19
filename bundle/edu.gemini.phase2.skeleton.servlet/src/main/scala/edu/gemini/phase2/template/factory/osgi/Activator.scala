package edu.gemini.phase2.template.factory.osgi

import edu.gemini.phase2.template.factory.api.TemplateFactory
import edu.gemini.phase2.template.factory.impl.{TemplateFactoryImpl, TemplateDb}

import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import java.util.logging.Logger
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Activator {
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

  def start(ctx: BundleContext) {
    // Do this in the background
    Future.apply {
      TemplateDb.load(user) match {
        case Left(msg) =>
          Log.severe("Could not load the template database: " + msg)
        case Right(db) =>
          val fact = TemplateFactoryImpl(db)
          service = Some(ctx.registerService(classOf[TemplateFactory], fact, new java.util.Hashtable[String,Object]()))
          Log.info("Loaded template database.")
      }
    }

  }

  def stop(ctx: BundleContext) {
    service foreach { _.unregister() }
  }

}
