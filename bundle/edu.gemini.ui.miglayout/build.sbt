import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ui.miglayout"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/miglayout-core-4.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/miglayout-swing-4.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scala-swing_2.10-2.0.0-SNAPSHOT.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ui.miglayout")
