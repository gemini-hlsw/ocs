import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.pit"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-xmlrpc_2.10-3.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

osgiSettings 

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.pit.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()


        
