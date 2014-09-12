package edu.gemini.osgi.tools.idea

import edu.gemini.osgi.tools.app.BundleSpec
import java.io.File
import edu.gemini.osgi.tools.{BundleVersion, Version}

/**
 * Combines a BundleSpec with the location of the Bundle (either a jar file
 * for a library bundle or a directory for a source bundle).
 */
case class BundleLoc(spec: BundleSpec, loc: File) {
  // def startLevel: Int = spec.startLevel
  def name: String = spec.name
  def version: Version = spec.version
  def toBundleVersion: BundleVersion = BundleVersion(loc)
}

object BundleLoc {
  def toBundleVersion(bl: BundleLoc): BundleVersion = BundleVersion(bl.loc)
}
