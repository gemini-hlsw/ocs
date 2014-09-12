import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.horizons.server"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.horizons.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.horizons.server.backend"
)

        
