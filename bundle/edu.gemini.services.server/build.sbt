import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.services.server"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/scala-library-2.10.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scala-swing-2.10.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.services.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.apache.avalon.*,!org.apache.commons.codec.binary.*,!org.apache.log.*,!org.apache.log4j.*,*")
        
