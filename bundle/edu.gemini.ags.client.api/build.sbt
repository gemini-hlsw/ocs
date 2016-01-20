import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ags.client.api"

version := pitVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.6")

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleActivator := Some("edu.gemini.ags.client.api.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ags.client.api")
