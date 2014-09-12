package edu.gemini.util.osgi

import org.osgi.framework._
import org.osgi.framework.launch.Framework
import scala.collection.JavaConverters._

// some helpers for testing osgi stuff
object TestFramework {

  /** Empty properties. */
  val emptyDict: java.util.Dictionary[String,Object] = 
    new java.util.Hashtable

  /** Runs the specified function in a new, empty OSGi framework. */
  def withFramework[A](a: BundleContext => A): A = {
    val storage = {
      val f = java.io.File.createTempFile("ocs-felix-test", "")
      f.delete; f.mkdir; f
    }
    val f = new org.apache.felix.framework.Felix(Map[String,String](
      Constants.FRAMEWORK_STORAGE -> storage.getAbsolutePath
    ).asJava)
    try {
      f.start
      a(f.getBundleContext)
    } finally {
      f.stop
    }
  }

}

