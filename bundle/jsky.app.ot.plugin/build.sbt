import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot.plugin"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.0.6")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "jsky.app.ot.plugin")
