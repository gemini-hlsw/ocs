import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.mail"

// version set in ThisBuild


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.shared.mail")

        
