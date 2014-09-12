import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot.shared"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.0.3.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.osgi.core-4.3.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("jsky.app.ot.shared.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "jsky.app.ot.shared.gemini.obscat",
  "jsky.app.ot.shared.progstate",
  "jsky.app.ot.shared.spModel.util",
  "jsky.app.ot.shared.vcs")

        
