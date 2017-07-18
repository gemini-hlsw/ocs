import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.tools.p1pdfmaker"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"))

ocsBundleSettings // defined in top-level project/ folder

osgiSettings // from the sbt-osgi plugin

OsgiKeys.bundleActivator := Some("edu.gemini.tools.p1pdfmaker.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  )

fork in run := true
