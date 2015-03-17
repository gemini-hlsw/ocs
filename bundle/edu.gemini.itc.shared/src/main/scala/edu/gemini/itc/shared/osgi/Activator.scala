package edu.gemini.itc.shared.osgi

import org.osgi.framework.{BundleActivator, BundleContext}

class Activator extends BundleActivator {
  def start(context: BundleContext) {
    println("edu.gemini.itc.shared started.")
  }

  def stop(context: BundleContext) {
    println("edu.gemini.itc.shared stopped.")
  }
}
