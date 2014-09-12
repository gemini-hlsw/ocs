package edu.gemini.osgi.tools

import java.io.File
import FileUtils.jarFilter
import java.util.jar.JarFile

/** Provides access to library bundles in `libDir`, keyed by symbolic name and version. */
class LibraryBundles(val libDir: File) {

  val jars: Set[File] = 
    libDir.listFiles(jarFilter).toSet

  val bundleMap: Map[BundleKey, File] =
    jars.map(jar => BundleKey(new BundleManifest(new JarFile(jar).getManifest)) -> jar).toMap

}
