import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.ca"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/com-cosylab-epics-caj_2.10-1.0.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.shared.ca",
  "edu.gemini.shared.ca.internal",
  "edu.gemini.shared.ca.weather",
  "edu.gemini.jca")

        
