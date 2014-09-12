import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.spdb.reports.collection"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix.http.jetty-2.2.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/com-cosylab-epics-caj_2.10-1.0.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.0.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-velocity_2.10-1.4.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.spdb.reports.collection.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spdb.reports.collection.table",
  "edu.gemini.spdb.reports.collection.util",
  "edu.gemini.spdb.reports",
  "edu.gemini.spdb.reports.util",
  "edu.gemini.spdb.reports.impl.www",
  "edu.gemini.spdb.reports.impl.www.vm",
  "edu.gemini.spdb.cron",
  "edu.gemini.spdb.cron.util",
  "edu.gemini.weather",
  "edu.gemini.epics")

        
