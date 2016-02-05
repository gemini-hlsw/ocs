import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

ocsAppManifest := {
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "seqexec-server", 
    name = "Seqexec Server",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
        development(v),
        with_test_dbs(v),
          mac_test(v),
          linux64_test(v),
        with_production_dbs(v),
          mac(v),
          linux64(v)
      )
    )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx1024M"
  ),
  props = Map(
    "org.osgi.framework.storage.clean"                 -> "onFirstInit",
    "edu.gemini.spdb.mode"                             -> "api",
    "org.osgi.framework.startlevel.beginning"          -> "100",
    "edu.gemini.util.security.auth.ui.showDatabaseTab" -> "true",
          "org.osgi.framework.bootdelegation"          -> "*"
  ),
  log = Some("%a/log/seqexec.%u.%g.log"),
  bundles = List(
    BundleSpec("edu.gemini.ags",               version),
    BundleSpec("edu.gemini.osgi.main",         Version(4,  2, 1)),
    BundleSpec("edu.gemini.seqexec.server",    version),
    BundleSpec("edu.gemini.shared.gui",        version),
    BundleSpec("edu.gemini.spModel.io",        version),
    BundleSpec("edu.gemini.spModel.smartgcal", version),
    BundleSpec("edu.gemini.epics.acm",         version),
    BundleSpec("org.scala-lang.scala-reflect", Version(2, 10, 5)),
    BundleSpec("org.scala-lang.scala-swing",   Version(2, 0, 0)),
    BundleSpec("slf4j.api",                    Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                  Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",   Version(1, 1, 0)),
    BundleSpec("squants",                      Version(0, 5, 3))
  )
)

// WITH-TEST-DBS
def with_test_dbs(version: Version) = AppConfig(
  id = "with-test-dbs",
  props = Map(
    "edu.gemini.util.trpc.peer.GN" -> "gnodbtest2.gemini.edu:8443:Gemini North ODB (Test)",
    "edu.gemini.util.trpc.peer.GS" -> "gsodbtest2.gemini.edu:8443:Gemini South ODB (Test)"
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
  log = Some("%h/Library/Logs/edu.gemini.seqexec/seqexec.%u.%g.log")
) extending List(with_test_dbs(version))

// MAC
def mac(version: Version) = AppConfig(
  id = "mac",
  distribution = List(MacOS),
  log = Some("%h/Library/Logs/edu.gemini.seqexec/seqexec.%u.%g.log")
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
