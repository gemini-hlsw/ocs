
import OcsKeys._
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

ocsAppManifest := {
  Application(
    id = "epics-acm", 
    name = "EPICS ACM",
    label = None,
    version = ocsVersion.value.toString,
    configs = List(
      AppConfig(
        id = "common",
        distribution = List(TestDistro),
        args = Nil,
        vmargs = List(
          "-Xmx1024M"
        ),
        props = Map(
          "org.osgi.framework.storage.clean"        -> "onFirstInit",
          "org.osgi.framework.startlevel.beginning" -> "100",
          "org.osgi.framework.bootdelegation"       -> "*"
        ),
        icon = None,
        log = None,
        script = None,
        bundles = List(
          BundleSpec("edu.gemini.epics.acm",          ocsVersion.value.toBundleVersion),
          BundleSpec("edu.gemini.osgi.main",          Version(4,  2, 1)),
          BundleSpec("org.apache.felix.configadmin",  Version(1,  6, 0)),
          BundleSpec("org.apache.felix.gogo.runtime", Version(0, 10, 0)),
          BundleSpec("org.apache.felix.gogo.command", Version(0, 12, 0)),
          BundleSpec("org.apache.felix.gogo.shell",   Version(0, 10, 0))
        )
      ) extending Nil
    )
  )
}

resolvers ++= (resolvers in bundle_edu_gemini_epics_acm).value



