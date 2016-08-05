import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.catalog"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-logging_2.10-1.1.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core"       % ScalaZVersion,
  "org.scalaz" %% "scalaz-concurrent" % ScalaZVersion)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.catalog.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.privatePackage := Seq(
  "edu.gemini.catalog.*",
  "jsky.catalog.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.catalog.api",
  "edu.gemini.catalog.impl",
  "edu.gemini.catalog.image",
  "edu.gemini.catalog.votable",
  "edu.gemini.catalog.skycat",
  "jsky.catalog",
  "jsky.catalog.astrocat",
  "jsky.catalog.skycat",
  "jsky.catalog.util"
)

initialCommands := "import edu.gemini.spModel.core._,edu.gemini.catalog.api._,edu.gemini.catalog.votable._, scalaz._, Scalaz._"
