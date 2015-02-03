package edu.gemini.model.p1.osgi

import edu.gemini.model.p1.immutable.Semester
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import java.util.Dictionary
import java.util.Enumeration

class Activator extends BundleActivator {
  def start(context: BundleContext) {
    val bundleRegex = """(\d\d\d\d)\d\d(\d)\.(\d)\.\d""".r
    if (context.getProperty("edu.gemini.model.p1.year") != null) {
      System.setProperty("edu.gemini.model.p1.year", context.getProperty("edu.gemini.model.p1.year"))
    }
    if (context.getProperty("edu.gemini.model.p1.semester") != null) {
      System.setProperty("edu.gemini.model.p1.semester", context.getProperty("edu.gemini.model.p1.semester"))
    }
    val headersEnum: Enumeration[String] = context.getBundle.getHeaders.keys
    while (headersEnum.hasMoreElements) {
      val key = headersEnum.nextElement
      if (key.startsWith("Bundle-Version")) {
        val schemaVersion = context.getBundle.getHeaders.get(key) match {
          case bundleRegex(y, m, n) => s"$y.$m.$n"
          case x                    => x
        }
        System.setProperty("edu.gemini.model.p1.schemaVersion", schemaVersion)
      }
    }
    System.out.println("edu.gemini.model.p1 started on " + Semester.current + " with version " + System.getProperty("edu.gemini.model.p1.schemaVersion"))
  }

  def stop(context: BundleContext) {
    System.out.println("edu.gemini.model.p1 stopped.")
  }
}