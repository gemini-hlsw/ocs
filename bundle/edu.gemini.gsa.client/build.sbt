import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.gsa.client"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/jsoup-1.6.1.jar"))

ocsBundleSettings // defined in top-level project/ folder

osgiSettings // from the sbt-osgi plugin

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.gsa.client.api",
  "edu.gemini.gsa.client.impl")

        
