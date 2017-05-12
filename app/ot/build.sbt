
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }
import OcsCredentials.Ot._

ocsAppSettings

// App manifest for the Observing Tool
ocsAppManifest := {
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "ot",
    name = "OT",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
        with_test_dbs(v),
          mac_test(v),
          windows_test(v),
          linux_test(v),
            linux32_test(v),
            linux64_test(v),
        with_production_dbs(v),
          mac(v),
          windows(v),
          linux(v),
            linux32(v),
            linux64(v),
            rpm64(v),
      development(v),
        localhost_is_gn(v),
        localhost_is_gs(v)
    )
  )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Duser.language=en",
    "-Duser.country=US"
  ),
  props = Map(
    "org.osgi.framework.storage.clean"        -> "onFirstInit",
    "org.osgi.framework.startlevel.beginning" -> "99",
    "org.osgi.framework.bootdelegation"       -> "*",
    "edu.gemini.spdb.mode"                    -> "local"
  ),
  log = Some("%a/log/ot.%u.%g.log"),
  bundles = List(
    BundleSpec("edu.gemini.osgi.main",         Version(4, 2, 1)),
    BundleSpec("edu.gemini.util.log.extras",   version),
    BundleSpec("edu.gemini.sp.vcs",            version),
    BundleSpec("edu.gemini.sp.vcs.log",        version),
    BundleSpec("edu.gemini.sp.vcs.reg",        version),
    BundleSpec("edu.gemini.qpt.shared",        version),
    BundleSpec("edu.gemini.itc.shared",        version),
    BundleSpec("edu.gemini.qv.plugin",         version),
    BundleSpec("edu.gemini.services.client",   version),
    BundleSpec("slf4j.api",                    Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                  Version(1, 6, 4)),
    BundleSpec("org.h2",                       Version(1, 3, 170)),
    BundleSpec("org.apache.commons.io",        Version(2, 0, 1)),
    BundleSpec("jsky.app.ot",                  version),
    BundleSpec("jsky.app.ot.visitlog",         version),
    BundleSpec("io.argonaut",                  Version(6, 2, 0)),
    BundleSpec("org.apache.commons.logging",   Version(1, 1, 0))
  ),
  spec = Some(file("app/ot/dist/RPM64/ot.spec.template"))
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
  vmargs = List(
    "-Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS"
  ),
  icon = Some(file("app/ot/dist/MacOS/OT.icns")), // TODO
  log = Some("%h/Library/Logs/edu.gemini.ot/ot.%u.%g.log")
) extending List(with_test_dbs(version))

// MAC
def mac(version: Version) = AppConfig(
  id = "mac",
  distribution = List(MacOS),
  vmargs = List(
    "-Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS"
  ),
  icon = Some(file("app/ot/dist/MacOS/OT.icns")), // TODO
  log = Some("%h/Library/Logs/edu.gemini.ot/ot.%u.%g.log")
) extending List(with_production_dbs(version))

// LINUX-TEST
def linux_test(version: Version) = AppConfig(
  id = "linux-test"
) extending List(with_test_dbs(version))

// LINUX
def linux(version: Version) = AppConfig(
  id = "linux"
) extending List(with_production_dbs(version))

// LINUX32-TEST
def linux32_test(version: Version) = AppConfig(
  id = "linux32-test",
  distribution = List(Linux32)
) extending List(linux_test(version))

// LINUX32
def linux32(version: Version) = AppConfig(
  id = "linux32",
  distribution = List(Linux32)
) extending List(linux(version))

// LINUX64-TEST
def linux64_test(version: Version) = AppConfig(
  id = "linux64-test",
  distribution = List(Linux64)
) extending List(linux_test(version))

// LINUX64
def linux64(version: Version) = AppConfig(
  id = "linux64",
  distribution = List(Linux64)
) extending List(linux(version))

// RPM64
def rpm64(version: Version) = AppConfig(
  id = "rpm64",
  distribution = List(RPM64)
) extending List(linux64(version))

// WINDOWS-TEST
def windows_test(version: Version) = AppConfig(
  id = "windows-test",
  distribution = List(Windows),
  props = Map(
    "felix.security.defaultpolicy" -> "true"
  ),
  icon = Some(file("app/ot/dist/Windows/OT.ico"))
) extending List(with_test_dbs(version))

// WINDOWS
def windows(version: Version) = AppConfig(
  id = "windows",
  distribution = List(Windows),
  props = Map(
    "felix.security.defaultpolicy" -> "true"
  ),
  icon = Some(file("app/ot/dist/Windows/OT.ico"))
) extending List(with_production_dbs(version))

// DEVELOPMENT
def development(version: Version) = AppConfig(
  id = "development",
  props = Map(
    "edu.gemini.util.security.auth.ui.showDatabaseTab" -> "",
    "obr.repository.url"                               -> "http://localhost:9999/bundle"
  ),
  bundles = List(
    BundleSpec(10, "org.apache.felix.bundlerepository", Version(1, 6, 6)),
    BundleSpec(10, "org.apache.felix.gogo.runtime",     Version(0, 10, 0)),
    BundleSpec(10, "org.apache.felix.gogo.command",     Version(0, 12, 0)),
    BundleSpec(10, "org.apache.felix.gogo.shell",       Version(0, 10, 0))
  )
) extending List(common(version))

// LOCALHOST_IS_GN
def localhost_is_gn(version: Version) = AppConfig(
  id = "localhost_is_gn",
  distribution = List(TestDistro),
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "localhost:8443:Gemini North ODB (Local)",
    "edu.gemini.util.trpc.peer.GS" -> ""
  )
) extending List(development(version))

// LOCALHOST_IS_GS
def localhost_is_gs(version: Version) = AppConfig(
  id = "localhost_is_gs",
  distribution = List(TestDistro),
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "",
    "edu.gemini.util.trpc.peer.GS" -> "localhost:8443:Gemini South ODB (Local)"
  )
) extending List(development(version))



