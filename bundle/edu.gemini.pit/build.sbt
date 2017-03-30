import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.pit"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-xmlrpc_2.10-3.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"))

libraryDependencies ++= Seq(
  "org.scalaz"     %% "scalaz-core"       % ScalaZVersion,
  "org.scalaz"     %% "scalaz-concurrent" % ScalaZVersion,
  "org.scalaz"     %% "scalaz-effect"     % ScalaZVersion,
  "org.scala-lang" %  "scala-actors"      % "2.11.7",
  "org.scala-lang" %  "scala-compiler"    % "2.11.7" // required to pull the dependencies via edu.gemini.util.security
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.pit.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()
