import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ags.client.api"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/scala-actors-2.10.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"))

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleActivator := Some("edu.gemini.ags.client.api.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ags.client.api")

        
