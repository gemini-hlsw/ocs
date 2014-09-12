package edu.gemini.osgi.tools

import java.io.{ IOException, FileInputStream, FileFilter, File }
import java.util.jar.Manifest
import org.eclipse.osgi.util.ManifestElement
import org.osgi.framework.BundleException
import scala.collection.immutable.TreeSet

object BundleManifest {
  val BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"
  val BUNDLE_VERSION = "Bundle-Version"
}

class BundleManifest(val mf: Manifest) {
  import BundleManifest._

  def this(file: File) = this(try {
    new Manifest(new FileInputStream(file))
  } catch {
    case e: Exception => throw new RuntimeException("Could not open manifest " + file, e)
  })

  private val attrs = mf.getMainAttributes

  lazy val version = Version.parse(attrs.getValue(BUNDLE_VERSION))
  lazy val symbolicName = attrs.getValue(BUNDLE_SYMBOLICNAME).split(";")(0)

  private def get(header: String): Array[ManifestElement] =
    Option(attrs.getValue(header)).map(ManifestElement.parseHeader(header, _)).getOrElse(Array())

}

case class BundleVersion(dir: File) extends Comparable[BundleVersion] {

  lazy val manifest = 
    new BundleManifest(new File(dir, "META-INF/MANIFEST.MF"))

  def compareTo(o: BundleVersion) = {
    var ret = manifest.symbolicName.compareTo(o.manifest.symbolicName)
    if (ret == 0) ret = manifest.version.compareTo(o.manifest.version)
    if (ret == 0) ret = dir.compareTo(o.dir)
    ret
  }

  override def toString =
    manifest.symbolicName + " " + manifest.version + " (" + dir + ")"

}



