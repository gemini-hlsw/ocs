import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ags.servlet"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.2.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.ags.servlet.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ags.estimate")


