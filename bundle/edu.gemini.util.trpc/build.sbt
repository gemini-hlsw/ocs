import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.trpc"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.11-7.0.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.11-7.0.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.util.trpc.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.trpc.client",
  "edu.gemini.util.trpc.auth")

        
OsgiKeys.additionalHeaders += 
  ("Import-Package" -> "!sun.misc,!sun.misc.*,*")

