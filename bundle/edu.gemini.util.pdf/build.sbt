import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.pdf"

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "org.apache.fop.*",
  "edu.gemini.util.pdf"
)

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!javax.media.jai.*,!com.apple.*,!com.sun.*,!org.apache.*,!org.bouncycastle.*,!org.mozilla.*,!org.w3c.dom.*,org.apache.commons.logging,*")
