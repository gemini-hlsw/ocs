import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.oodb.auth.servlet"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix-4.2.1.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.apache.felix.http.jetty-2.2.0.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  // new File(baseDirectory.value, "../../lib/bundle/pax-web-jetty-bundle-1.1.13.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.spdb.authServlet.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value
        
OsgiKeys.privatePackage := Seq("edu.gemini.spdb.authServlet.*")
