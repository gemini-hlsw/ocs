import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.model.p1"

version := pitVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scalaz"             %% "scalaz-core" % ScalaZVersion
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.model.p1.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.model.p1.*"
)

sourceGenerators in Compile += Def.task {
  import scala.sys.process._
  val pkg = "edu.gemini.model.p1.mutable"
  val log = state.value.log
  val gen = (sourceManaged in Compile).value
  val out = (gen /: pkg.split("\\."))(_ / _)
  val xjb = sourceDirectory.value / "main" / "xjb"
  val xsd = sourceDirectory.value / "main" / "xsd" / "Proposal.xsd"
  val cmd = List("xjc",
    "-d", gen.getAbsolutePath,
    "-p", pkg,
    "-b", xjb.getAbsolutePath,
    xsd.getAbsolutePath)
  val mod = (xjb.listFiles ++ xsd.getParentFile.listFiles).map(_.lastModified).max
  val cur = if (out.exists && out.listFiles.nonEmpty) out.listFiles.map(_.lastModified).min else Int.MaxValue
  if (mod > cur) {
    out.mkdirs
    val err = cmd.run(ProcessLogger(log.info(_), log.error(_))).exitValue
    if (err != 0) sys.error("xjc failed")
  }
  out.listFiles.toSeq
}.taskValue

unmanagedResourceDirectories in Compile +=
  sourceDirectory.value / "main" / "xsd"

// > modelDist
commands += {
  import scala.sys.process._
  Command.command("modelDist") { state =>
    val version = s"${pitVersion.value.semester}.${pitVersion.value.xmlCompatibility}.${pitVersion.value.serialCompatibility}"
    val schemaName = s"p1-schema-$version"
    println(s"packaging model $schemaName")
    val main = sourceDirectory.value / "main"
    s"tar cvfz $schemaName.tar.gz -s /xsd/$schemaName/ -C $main xsd".!
    state
  }
}

publishArtifact in (ThisBuild, packageSrc) := true

publishMavenStyle in ThisBuild := true
