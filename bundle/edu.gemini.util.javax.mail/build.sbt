import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.javax.mail"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.mail.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq(
  "edu.gemini.mail.*"
)

OsgiKeys.exportPackage := Seq(
  "javax.mail",
  "javax.mail.event",
  "javax.mail.search",
  "javax.mail.internet",
  "javax.mail.util")



