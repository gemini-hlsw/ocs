
import sbt._
import Keys._

object OcsBuild extends Build
  with OcsBundle      // bundle project definitions
  with OcsBundleSettings // bundle project definitions
  with OcsApp         // application project definitions
  with OcsAppSettings // settings for app projects
  with OcsKey         // ocs-provided keys
{
  val ScalaZVersion = "7.2.13"

  override lazy val settings = super.settings ++
    Seq(
      (ocsBootTime          in ThisBuild) := System.currentTimeMillis,
      (ocsAllProjects       in ThisBuild) := (thisProject    in LocalRootProject).value.aggregate.toList,
      (ocsAllBundleProjects in ThisBuild) := (ocsAllProjects in LocalRootProject).value.filter(_.project.startsWith("bundle_")), // :-(
      (ocsLibraryBundles    in ThisBuild) := ((baseDirectory in LocalRootProject).value / "lib" / "bundle").listFiles.filter(_.getName.endsWith(".jar")).toList
    )

}
