package edu.gemini.itc.web.osgi

import org.osgi.framework.{BundleActivator, BundleContext}

class Activator extends BundleActivator {
  def start(context: BundleContext) {
    println("edu.gemini.itc.web started.")
  }

  def stop(context: BundleContext) {
    println("edu.gemini.itc.web stopped.")
  }
}
