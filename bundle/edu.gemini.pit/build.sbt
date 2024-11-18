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
  "org.scala-lang" %  "scala-actors"      % scalaVersion.value
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.pit.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()

sourceGenerators in Compile += Def.task {
  val pitVer = pitVersion.value
  val outDir = (sourceManaged in Compile).value / "edu" / "gemini" / "pit" / "model"
  val outFile = new File(outDir, pitVer.sourceFileName)
  outDir.mkdirs
  IO.write(outFile, pitVer.toClass("edu.gemini.pit.model")) // UTF-8 is default
  Seq(outFile)
}.taskValue

publishArtifact in (ThisBuild, packageSrc) := true

publishMavenStyle in ThisBuild := true
