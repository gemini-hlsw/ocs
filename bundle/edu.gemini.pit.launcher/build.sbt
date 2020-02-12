import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.pit.launcher"

version := pitVersion.value.toOsgiVersion

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()

sourceGenerators in Compile += Def.task {
  val pitVer = pitVersion.value
  val outDir = (sourceManaged in Compile).value / "edu" / "gemini" / "pit" / "launcher"
  val outFile = new File(outDir, pitVer.sourceFileName)
  outDir.mkdirs
  IO.write(outFile, pitVer.toClass("edu.gemini.pit.launcher")) // UTF-8 is default
  Seq(outFile)
}.taskValue

initialCommands := "import edu.gemini.pit.launcher._, scalaz._, Scalaz._"

scalacOptions in (Compile, doc) ++= Seq(
  "-groups",
  "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
  "-doc-source-url", "https://github.com/gemini-hlws/ocs/master€{FILE_PATH}.scala"
)

publishArtifact in (ThisBuild, packageSrc) := true

publishMavenStyle in ThisBuild := true


fork in run := true
