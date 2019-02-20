import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.auxfile.workflow"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.auxfile.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq(
  "edu.gemini.auxfile.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.auxfile.api",
  "edu.gemini.auxfile.client",
  "edu.gemini.auxfile.server",
  "edu.gemini.auxfile.copier")

parallelExecution in Test := false
