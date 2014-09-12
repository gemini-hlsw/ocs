import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.model.p1.pdf"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar")
)

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.model.p1.pdf")

        
