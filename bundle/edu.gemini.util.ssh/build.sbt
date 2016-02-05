import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.ssh"

// version set in ThisBuild

// unmanagedJars in Compile ++= Seq(
//   new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
//   new File(baseDirectory.value, "../../lib/bundle/scala-library-2.10.5.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.ssh")

OsgiKeys.additionalHeaders += 
  ("Import-Package" -> "!com.jcraft.jzlib,!keypairgen,!signature,!userauth,*")
        
