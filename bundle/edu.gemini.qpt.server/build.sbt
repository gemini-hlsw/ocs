import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.qpt.server"

// version set in ThisBuild


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.qpt.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  )

        
