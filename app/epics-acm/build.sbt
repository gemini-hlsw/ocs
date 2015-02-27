
import OcsKeys._
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

ocsAppManifest := {
  val bs = List(
    BundleSpec((sbt.Keys.name in bundle_edu_gemini_epics_acm).value, Version.parse((sbt.Keys.version in bundle_edu_gemini_epics_acm).value))
  )
  Application(
    id = "epics-acm",
    name = "EPICS ACM",
    label = None,
    version = ocsVersion.value.toString,
    configs = List(
      AppConfig(
        id = "common",
        distribution = Nil,
        args = Nil,
        vmargs = Nil,
        props = Map.empty,
        icon = None,
        log = None,
        script = None,
        bundles = bs
      ) extending Nil
    )
  )
}

resolvers ++= (resolvers in bundle_edu_gemini_epics_acm).value



