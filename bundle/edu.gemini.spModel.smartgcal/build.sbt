import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.spModel.smartgcal"

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/au-com-bytecode-opencsv_2.10-2.1.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spModel.smartgcal",
  "edu.gemini.spModel.smartgcal.functors",
  "edu.gemini.spModel.smartgcal.provider",
  "edu.gemini.spModel.smartgcal.repository")

        
