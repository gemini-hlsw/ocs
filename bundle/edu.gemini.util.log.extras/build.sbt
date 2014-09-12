import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.log.extras"

// version set in ThisBuild

// unmanagedJars in Compile ++= Seq(
//   new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.util.logging.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.logging",
  "edu.gemini.util.logging.osgi")

        
OsgiKeys.privatePackage := Seq("edu.gemini.util.logging.*")