import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.file.filter"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq(
  "edu.gemini.file.*",
  "edu.gemini.filefilter.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.filefilter",
  "edu.gemini.filefilter.osgi",
  "edu.gemini.file.util",
  "edu.gemini.file.util.osgi")
