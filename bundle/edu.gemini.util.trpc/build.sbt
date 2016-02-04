import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.trpc"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix.http.jetty-2.2.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.eclipse.osgi.services_3.2.0.v20090520-1800.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.osgi.compendium-4.2.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.osgi.enterprise-5.0.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/scala-library-2.10.5.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/pax-web-jetty-bundle-1.1.13.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.util.trpc.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.trpc.client",
  "edu.gemini.util.trpc.auth")

        
OsgiKeys.additionalHeaders += 
  ("Import-Package" -> "!sun.misc,!sun.misc.*,*")

