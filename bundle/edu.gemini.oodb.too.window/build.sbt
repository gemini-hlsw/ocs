import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.oodb.too.window"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.too.window.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.too.window",
  "edu.gemini.too.email")
        
OsgiKeys.privatePackage := Seq("edu.gemini.too.*")

