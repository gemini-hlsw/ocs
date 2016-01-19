import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.p1monitor"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-actors_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

ocsBundleSettings // defined in top-level project/ folder

osgiSettings // from the sbt-osgi plugin

OsgiKeys.bundleActivator := Some("edu.gemini.p1monitor.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()

        
