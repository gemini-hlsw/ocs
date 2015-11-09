import OcsKeys._

name := "edu.gemini.spModel.core"

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/squants_2.10-0.5.3.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spModel.core",
  "edu.gemini.spModel.core.osgi")
        
sourceGenerators in Compile += Def.task {
  val ocsVer = ocsVersion.value
  val outDir = (sourceManaged in Compile).value / "edu" / "gemini" / "spModel" / "core"
  val outFile = new File(outDir, ocsVer.sourceFileName)
  outDir.mkdirs
  IO.write(outFile, ocsVer.toClass) // UTF-8 is default
  Seq(outFile)
}.taskValue

initialCommands := "import edu.gemini.spModel.core._, scalaz._, Scalaz._"

scalacOptions in (Compile, doc) ++= Seq(
  "-groups",
  "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath, 
  "-doc-source-url", "https://github.com/gemini-hlws/ocs/master€{FILE_PATH}.scala" 
)

publishArtifact in (ThisBuild, packageSrc) := true

publishMavenStyle in ThisBuild := true

// Add your credentials to the artifactory repository
publishTo := {
    val repo = if (isSnapshot.value) {
      "libs-snapshot-local"
    } else {
      "libs-release-local"
    }
    Some("Gemini Artifactory" at s"http://sbfosxdev-mp1.cl.gemini.edu:8081/artifactory/$repo")
  }
