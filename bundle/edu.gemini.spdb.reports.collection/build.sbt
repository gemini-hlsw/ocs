import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.spdb.reports.collection"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/com-cosylab-epics-caj_2.10-1.0.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-velocity_2.10-1.4.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % ScalaZVersion,
  "org.scalaz" %% "scalaz-effect" % ScalaZVersion)

osgiSettings

ocsBundleSettings

fork in Test := true

OsgiKeys.bundleActivator := Some("edu.gemini.spdb.cron.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spdb.reports.*" // needed by velocity
)

OsgiKeys.privatePackage := Seq(
  "edu.gemini.spdb.reports.*",
  "edu.gemini.spdb.cron.*",
  "edu.gemini.dbTools.*",
  "edu.gemini.weather.*",
  "edu.gemini.epics.*"
)
