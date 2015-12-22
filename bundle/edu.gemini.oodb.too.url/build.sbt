import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.oodb.too.url"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.11-7.0.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.11-7.0.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.spdb.rapidtoo.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.privatePackage := Seq("edu.gemini.spdb.rapidtoo.*")

