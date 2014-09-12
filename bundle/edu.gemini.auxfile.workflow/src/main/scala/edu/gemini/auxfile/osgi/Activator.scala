package edu.gemini.auxfile.osgi

import org.osgi.framework.{BundleContext, BundleActivator}

final class Activator extends BundleActivator {
  // These three Activators used to live in separate bundles.  We'll just
  // call them one by one from our bundle's official registered Activator.
  private val subAct = List(
    new edu.gemini.auxfile.copier.osgi.Activator,
    new edu.gemini.auxfile.server.osgi.Activator,
    new edu.gemini.auxfile.workflow.osgi.Activator)

  private def isServer(ctx: BundleContext): Boolean =
    Option(ctx.getProperty("edu.gemini.auxfile.server")).exists(_.toLowerCase == "true")

  def start(ctx: BundleContext): Unit = if (isServer(ctx)) { subAct.foreach(_.start(ctx)) }

  def stop(ctx: BundleContext): Unit  = if (isServer(ctx)) { subAct.reverse.foreach(_.stop(ctx)) }
}
