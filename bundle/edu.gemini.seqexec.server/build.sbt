

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.server"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/squants_2.11-0.6.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-core_2.11-1.1.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-macro_2.11-1.1.0.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang" %  "scala-compiler"    % "2.11.7",
  "org.scalaz"     %% "scalaz-core"       % "7.1.6",
  "org.scalaz"     %% "scalaz-concurrent" % "7.1.6")


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.seqexec.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()
