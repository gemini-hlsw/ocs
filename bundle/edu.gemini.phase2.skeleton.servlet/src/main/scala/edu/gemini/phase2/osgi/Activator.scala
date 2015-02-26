package edu.gemini.phase2.osgi

import org.osgi.framework.{BundleContext, BundleActivator}

/**
 * Delegates start/stop to all of this bundle's contained Activators.  Several
 * bundles were merged into this one as part of the migration to sbt and
 * general code cleanup.  This simply makes sure that all the activator tasks
 * are still taken care of in the combined bundle.
 */
class Activator extends BundleActivator {
  val AllActivators = List(
    new edu.gemini.phase2.skeleton.servlet.osgi.Activator,
    new edu.gemini.phase2.template.factory.osgi.Activator,
    new edu.gemini.phase2.template.servlet.osgi.Activator
  )

  def start(ctx: BundleContext): Unit = AllActivators.foreach(_.start(ctx))
  def stop(ctx: BundleContext): Unit  = AllActivators.foreach(_.stop(ctx))
}
