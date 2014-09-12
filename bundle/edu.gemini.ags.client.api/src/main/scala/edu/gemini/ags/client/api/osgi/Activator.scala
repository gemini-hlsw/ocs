package edu.gemini.ags.client.api.osgi

import org.osgi.framework.{BundleActivator, BundleContext}

class Activator extends BundleActivator {
  def start(context: BundleContext) {
    println("edu.gemini.ags.client.api started.")
  }

  def stop(context: BundleContext) {
    println("edu.gemini.ags.client.api stopped.")
  }
}
