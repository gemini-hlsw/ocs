import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.spModel.pio"

// version set in ThisBuild
unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spModel.pio",
  "edu.gemini.spModel.pio.xml")

        
