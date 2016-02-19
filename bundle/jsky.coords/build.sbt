import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.coords"

// version set in ThisBuild

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "jsky.coords",
  "jsky.coords.gui")
