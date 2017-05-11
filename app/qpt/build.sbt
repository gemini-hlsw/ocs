
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }
import OcsCredentials.Qpt._

ocsAppSettings

// Application project for Queue Planning Tool
ocsAppManifest := {
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "qpt",
    name = "QPT",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
        development(v),
        with_test_dbs(v),
          mac_test(v),
          linux64_test(v),
          windows_test(v),
        with_production_dbs(v),
          mac(v),
          linux64(v),
          windows(v),
          rpm64(v)
      )
    )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx1024M",
    "-Duser.language=en",
    "-Duser.country=US"
  ),
  props = Map(
    "org.osgi.framework.storage.clean"                 -> "onFirstInit",
    "edu.gemini.spdb.mode"                             -> "api",
    "edu.gemini.qpt.ltts.services.south.url"           -> "http://gsltts.cl.gemini.edu:8080/ltts/services",
    "org.osgi.framework.startlevel.beginning"          -> "100",
    "edu.gemini.util.security.auth.ui.showDatabaseTab" -> "true",
    "edu.gemini.qpt.ltts.services.north.url"           -> "http://gnltts.hi.gemini.edu:8080/ltts/services",
    "org.osgi.framework.bootdelegation"                -> "*"
  ),
  log = Some("%a/log/qpt.%u.%g.log"),
  bundles = List(
    BundleSpec("edu.gemini.ags",               version),
    BundleSpec("edu.gemini.osgi.main",         Version(4, 2, 1)),
    BundleSpec("edu.gemini.qpt.client",        version),
    BundleSpec("edu.gemini.shared.gui",        version),
    BundleSpec("edu.gemini.spModel.io",        version),
    BundleSpec("edu.gemini.spModel.smartgcal", version),
    BundleSpec("slf4j.api",                    Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                  Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",   Version(1, 1, 0))
  ),
  spec = Some(file("app/qpt/dist/RPM64/qpt.spec.template"))
) extending List(common_credentials(version))

// WITH-TEST-DBS
def with_test_dbs(version: Version) = AppConfig(
  id = "with-test-dbs",
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "gnodbtest.gemini.edu:8443:Gemini North ODB (Test)",
    "edu.gemini.util.trpc.peer.GS" -> "gsodbtest.gemini.edu:8443:Gemini South ODB (Test)"
  )
) extending List(common(version))

// WITH-PRODUCTION-DBS
def with_production_dbs(version: Version) = AppConfig(
  id = "with-production-dbs",
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "gnodb.gemini.edu:8443:Gemini North ODB (Test)",
    "edu.gemini.util.trpc.peer.GS" -> "gsodb.gemini.edu:8443:Gemini South ODB (Test)"
  )
) extending List(common(version))

// MAC-TEST
def mac_test(version: Version) = AppConfig(
  id = "mac-test",
  distribution = List(MacOS),
  icon = Some(file("app/qpt/dist/MacOS/QPT.icns")),
  log = Some("%h/Library/Logs/edu.gemini.qpt/qpt.%u.%g.log")
) extending List(with_test_dbs(version))

// MAC
def mac(version: Version) = AppConfig(
  id = "mac",
  distribution = List(MacOS),
  icon = Some(file("app/qpt/dist/MacOS/QPT.icns")),
  log = Some("%h/Library/Logs/edu.gemini.qpt/qpt.%u.%g.log")
) extending List(with_production_dbs(version))

// LINUX64-TEST
def linux64_test(version: Version) = AppConfig(
  id = "linux64-test",
  distribution = List(Linux64)
) extending List(with_test_dbs(version))

// LINUX64
def linux64(version: Version) = AppConfig(
  id = "linux64",
  distribution = List(Linux64)
) extending List(with_production_dbs(version))

// RPM64
def rpm64(version: Version) = AppConfig(
  id = "rpm64",
  distribution = List(RPM64),
  spec = Some(file("app/qpt/dist/RPM64/qpt.spec.template"))
) extending List(linux64(version))

// WINDOWS-TEST
def windows_test(version: Version) = AppConfig(
  id = "windows-test",
  distribution = List(Windows),
  icon = Some(file("app/qpt/dist/Windows/QPT.ico"))
) extending List(with_test_dbs(version))

// WINDOWS
def windows(version: Version) = AppConfig(
  id = "windows",
  distribution = List(Windows),
  icon = Some(file("app/qpt/dist/Windows/QPT.ico"))
) extending List(with_production_dbs(version))

// DEVELOPMENT
def development(version: Version) = AppConfig(
  id = "development",
  distribution = List(TestDistro),
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "localhost:8443:Gemini North ODB (Local)",
    "edu.gemini.util.trpc.peer.GS" -> "localhost:8443:Gemini South ODB (Local)"
  ),
  bundles = List(
    BundleSpec(99, "org.apache.felix.gogo.runtime", Version(0, 10, 0)),
    BundleSpec(99, "org.apache.felix.gogo.command", Version(0, 12, 0)),
    BundleSpec(99, "org.apache.felix.gogo.shell",   Version(0, 10, 0))
  )
) extending List(common(version))


