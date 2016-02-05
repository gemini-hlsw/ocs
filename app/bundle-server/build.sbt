
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

// Application project for Bundle Server
ocsAppManifest := {
  val v = ocsVersion.value.toBundleVersion
    Application(
    id = "bundle-server",
    name = "Bundle Server",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
      development(v),
      swalker(v)
    )
  )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx512M"
  ),
  props = Map(
    "org.osgi.framework.storage.clean" -> "onFirstInit",
    "org.osgi.framework.startlevel.beginning" -> "100",
    "org.osgi.framework.bootdelegation" -> "*",
    "org.osgi.service.http.port" -> "9999"
  ),
  bundles = List(
    BundleSpec("edu.gemini.osgi.main",               Version(4, 2, 1)),
    BundleSpec("org.scala-lang.scala-library",       Version(2, 10, 5)),
    BundleSpec("org.apache.felix.http.jetty",        Version(2, 2, 0)),
    BundleSpec("org.osgi.impl.bundle.repoindex.lib", Version(0, 0, 4)),
    BundleSpec("edu.gemini.util.osgi",               version),
    BundleSpec("edu.gemini.bundleserver",            version),
    BundleSpec("org.apache.felix.gogo.runtime",      Version(0, 10, 0)),
    BundleSpec("org.apache.felix.gogo.command",      Version(0, 12, 0))
  )
) extending List()

// DEVELOPMENT
def development(version: Version) = AppConfig(
  id = "development",
  bundles = List(
    BundleSpec(99, "org.apache.felix.gogo.shell", Version(0, 10, 0))
  )
) extending List(common(version))

// SWALKER
def swalker(version: Version) = AppConfig(
  id = "swalker",
  distribution = List(TestDistro),
  props = Map(
    "edu.gemini.bundleserver.roots" -> "/Users/swalker/dev/ocs1.5/osgi/bin/bundle /Users/swalker/dev/ocs1.5/osgi/lib"
  )
) extending List(development(version))

