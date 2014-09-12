package edu.gemini.osgi.tools
package app

import scala.xml._

case class BundleSpec(
  name: String,
  version: edu.gemini.osgi.tools.Version)

object BundleSpec {

  // TODO: get rid of
  def apply(n: Int, s: String, v: Version): BundleSpec =
    apply(s, v)

  def apply(mf: java.util.jar.Manifest): BundleSpec = {
    val bmf = new BundleManifest(mf)
    BundleSpec(bmf.symbolicName, bmf.version)
  }

}

