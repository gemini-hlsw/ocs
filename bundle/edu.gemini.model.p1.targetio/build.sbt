import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.model.p1.targetio"

version := pitVersion.value.toOsgiVersion

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

fork in Test := true

OsgiKeys.exportPackage := Seq(
  "edu.gemini.model.p1.targetio.api",
  "edu.gemini.model.p1.targetio.impl")

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!javax.xml.rpc.encoding.*,!junit.*,!org.apache.axis.*,!sun.*,!uk.ac.starlink.*,*")
