package edu.gemini.osgi.tools

import language._
import java.io._
import java.util.jar.Manifest
import scala.xml.Elem
import scala.collection.JavaConverters.asScalaIteratorConverter

package object app {

  type Version = edu.gemini.osgi.tools.Version
  val Version = edu.gemini.osgi.tools.Version

  type BetterProperties = edu.gemini.osgi.tools.BetterProperties

  implicit class RichnElem(e: Elem) {

    /** Returns the specified attribute value as an Option[String] */
    def \?(attr: String) = (e \ ("@" + attr)).map(_.text).headOption

    /** Returns the specified attribute value as an String, or throw. */
    def \!(attr: String) = \?(attr).getOrElse(sys.error("Required attribute %s not present in %s.".format(attr, e)))

  }

  def rm(f: File): Unit = if (f.isFile()) {
    f.delete() || sys.error("Couldn't delete " + f)
  } else if (f.isDirectory) {
    f.listFiles.foreach(rm)
    f.delete() || sys.error("Couldn't delete " + f)
  }

  // create and return the specified directory
  def mkdir(parent: File, name: String): File = {
    val f = new File(parent, name)
    if (f.exists)
      require(f.isDirectory, f + " is not a directory.")
    else {
      require(f.mkdir(), "Could not create directory " + f)
      //    println("Created " + f)
    }
    f
  }

  def copy(src: File, dest: File) {
    require(dest.isDirectory, dest + " does not exist or isn't a directory.")
    require(src.exists(), src + " does not exist.")
    if (src.isFile) {
      read(src) { is =>
        val f = new File(dest, src.getName)
        write(f) { os =>
          stream(is, os)
        }
        if (src.canExecute) {
          // Make it executable for owner, group, and world.
          // executable = true, ownerOnly = false
          f.setExecutable(true, false)
        }
      }
    } else {
      val dir = mkdir(dest, src.getName)
      src.listFiles foreach { copy(_, dir) }
    }
  }

  def stream(is: InputStream, os: OutputStream, buf: Array[Byte] = new Array(1024 * 64)) {
    val len = is.read(buf)
    if (len >= 0) {
      os.write(buf, 0, len)
      stream(is, os, buf)
    }
  }

  def write(dest: File)(f: OutputStream => Unit) {
    //  println("Writing " + dest)
    closing(new FileOutputStream(dest)) { f(_) }
  }

  def read(src: File)(f: InputStream => Unit) = closing(new FileInputStream(src)) { f(_) }

  def closing[A <: { def close(): Unit }](a: A)(f: A => Unit) = try { f(a) } finally { a.close() }

  def isFragment(mf: Manifest) = mf.getMainAttributes.getValue("Fragment-Host") != null

}
