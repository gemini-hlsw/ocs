
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

// Application project for Phase I Proposal Monitor
ocsAppManifest := {
  val ov = ocsVersion.value.toBundleVersion
  val pv = pitVersion.value.toBundleVersion
  Application(
    id = "p1-monitor",
    name = "Phase 1 Monitor Tool",
    version = pitVersion.value.toString,
    configs = List(
      common(ov, pv),
        dev(ov, pv),
        staging(ov, pv),
        v2021A(ov, pv),
        v2021B(ov, pv),
        v2022A(ov, pv)
    )
  )
}

// COMMON
def common(ov: Version, pv: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx512M",
    "-Djava.util.logging.config.file=conf/log.properties",
    "-Duser.language=en",
    "-Duser.country=US"
  ),
  props = Map(
    "org.osgi.framework.storage.clean"        -> "onFirstInit",
    "org.osgi.framework.startlevel.beginning" -> "100",
    "org.osgi.framework.bootdelegation"       -> "*"
  ),
  bundles = List(
    BundleSpec("edu.gemini.osgi.main",                   Version(4, 2, 1)),
    BundleSpec("edu.gemini.p1monitor",                   pv),
    BundleSpec("javax.servlet",                          Version(2, 5, 0)),
    BundleSpec("org.ops4j.pax.web.pax-web-jetty-bundle", Version(1, 1, 13)),
    BundleSpec("edu.gemini.util.javax.mail",             ov),
    BundleSpec("slf4j.api",                              Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                            Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",             Version(1, 1, 0))
  )
) extending List()

// DEV (formerly test)
def dev(ov: Version, pv: Version) = AppConfig(
  id = "dev",
  distribution = List(TestDistro),
  props = Map(
    "p1monitor.config"           -> "conf.test.xml",
    "org.osgi.service.http.port" -> "8000",
    "p1monitor.host"             -> "localhost"
  ),
  bundles = List(
    BundleSpec("org.apache.felix.gogo.runtime", Version(0, 10, 0)),
    BundleSpec("org.apache.felix.gogo.command", Version(0, 12, 0)),
    BundleSpec("org.apache.felix.gogo.shell",   Version(0, 10, 0))
  )
) extending List(common(ov, pv))

// STAGING
def staging(ov: Version, pv: Version) = AppConfig(
  id = "staging",
  props = Map(
    "org.osgi.framework.storage" -> "${user.home}/.edu.gemini.p1monitor.2014B/felix-cache",
    "p1monitor.config" -> "conf.staging.xml",
    "org.osgi.service.http.port" -> "8000",
    "p1monitor.host" -> "phase1dev.cl.gemini.edu"
  ),
  distribution = List(Linux32, Linux64)
) extending List(common(ov, pv))

// 2021A
def v2021A(ov: Version, pv: Version) = AppConfig(
  id = "2021A",
  props = Map(
    "org.osgi.framework.storage" -> "${user.home}/.edu.gemini.p1monitor.2021A/felix-cache",
    "p1monitor.config" -> "conf.production-2021A.xml",
    "org.osgi.service.http.port" -> "9006",
    "p1monitor.host" -> "phase1.gemini.edu"
  ),
  distribution = List(Linux32, Linux64)
) extending List(common(ov, pv))

// 2021B
def v2021B(ov: Version, pv: Version) = AppConfig(
  id = "2021B",
  props = Map(
    "org.osgi.framework.storage" -> "${user.home}/.edu.gemini.p1monitor.2021B/felix-cache",
    "p1monitor.config" -> "conf.production-2021B.xml",
    "org.osgi.service.http.port" -> "9007",
    "p1monitor.host" -> "phase1.gemini.edu"
  ),
  distribution = List(Linux32, Linux64)
) extending List(common(ov, pv))

// 2022A
def v2022A(ov: Version, pv: Version) = AppConfig(
  id = "2022A",
  props = Map(
    "org.osgi.framework.storage" -> "${user.home}/.edu.gemini.p1monitor.2022A/felix-cache",
    "p1monitor.config" -> "conf.production-2022A.xml",
    "org.osgi.service.http.port" -> "9006",
    "p1monitor.host" -> "phase1.gemini.edu"
  ),
  distribution = List(Linux32, Linux64)
) extending List(common(ov, pv))
