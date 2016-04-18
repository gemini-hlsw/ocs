

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.server"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/squants_2.11-0.6.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-core_2.11-1.2.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-macro_2.11-1.2.1.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang" %  "scala-compiler"    % "2.11.7",
  "org.scodec"     %% "scodec-bits"       % "1.0.9",
  "org.scalaz"     %% "scalaz-core"       % ScalaZVersion,
  "org.scalaz"     %% "scalaz-concurrent" % ScalaZVersion)


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.seqexec.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()
