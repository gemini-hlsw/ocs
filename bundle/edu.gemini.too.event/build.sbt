import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.too.event"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.6")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.too.event.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.too.event.api",
  "edu.gemini.too.event.client")
