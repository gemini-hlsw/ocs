import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot.testlauncher"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/com-jgoodies-looks_2.10-2.4.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/nom-tam-fits_2.10-0.99.3.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jfree_2.10-1.0.14.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % ScalaZVersion,
  "org.scalaz" %% "scalaz-effect" % ScalaZVersion)

osgiSettings

ocsBundleSettings

OsgiKeys.privatePackage := Seq(
  "jsky.app.ot.testlauncher"
)

// OsgiKeys.bundleActivator := Some("jsky.app.ot.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.additionalHeaders += 
  ("Import-Package" -> "!Acme.JPM.Encoders,!com.sun.*.jpeg,!sun.*,*")

OsgiKeys.dynamicImportPackage := Seq("*") // hmm

OsgiKeys.exportPackage := Seq()

fork := true
