package edu.gemini.osgi.tools

/** Combination of a bundle symbolic name and version that uniquely identifies a bundle */
case class BundleKey(name: String, version: Version)

object BundleKey {
  def apply(man: BundleManifest): BundleKey = 
    new BundleKey(man.symbolicName, man.version)
}

