
import sbt._
import Keys._

object OcsBuild extends Build
  with OcsBundle      // bundle project definitions
  with OcsBundleSettings // bundle project definitions
  with OcsApp         // application project definitions
  with OcsAppSettings // settings for app projects
  with OcsKey         // ocs-provided keys
{
  val ScalaZVersion = "7.2.30"

  override lazy val settings = super.settings ++
    Seq(
      (ocsBootTime          in ThisBuild) := System.currentTimeMillis,
      (ocsAllProjects       in ThisBuild) := (thisProject    in LocalRootProject).value.aggregate.toList,
      (ocsAllBundleProjects in ThisBuild) := (ocsAllProjects in LocalRootProject).value.filter(_.project.startsWith("bundle_")), // :-(
      (ocsLibraryBundles    in ThisBuild) := ((baseDirectory in LocalRootProject).value / "lib" / "bundle").listFiles.filter(_.getName.endsWith(".jar")).toList
    )

  // When running quiet builds it's important for there to be *some* output, otherwise Travis will
  // time us out. By defining a system property we can turn on a periodic ping.
  if (sys.props.isDefinedAt("edu.gemini.ocs.build.ping")) {
    println("*** Keep-alive ping requested. The build will say PING! every minute or so.")
    import java.util.{ Timer, TimerTask }
    val t = new Timer("pinger", true)
    t.scheduleAtFixedRate(new TimerTask {
      var count = 0
      def run() {
        count += 1
        println(s"*** PING! Minutes elapsed: rougly $count")
      }
    }, 1000 * 60, 1000 * 60)
  }

}
