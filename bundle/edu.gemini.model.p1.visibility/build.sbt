import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.model.p1.visibility"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"))

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.model.p1.visibility")

        
