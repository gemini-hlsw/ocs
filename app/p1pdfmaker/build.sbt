
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

// Application project for Phase I PDF Maker Tool
ocsAppManifest := {
  val pv = pitVersion.value.toBundleVersion
  Application(
    id = "p1pdfmaker",
    name = "Phase I PDF Maker Tool",
    version = pitVersion.value.toString,
    configs = List(
      common(pv),
        dev(pv),
        linux(pv)
    )
  )
}

// COMMON
def common(pv: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Xmx512M",
    "-Dedu.gemini.ui.workspace.impl.Workspace.fonts.shrunk=true",
    "-Dfile.encoding=UTF-8",
    "-Duser.language=en",
    "-Duser.country=US"
  ),
  props = Map(
    "org.osgi.framework.storage.clean"        -> "onFirstInit",
    "org.osgi.framework.startlevel.beginning" -> "100",
    "org.osgi.framework.bootdelegation"       -> "*",
    "edu.gemini.ags.host"                     -> "gnodb.gemini.edu",
    "edu.gemini.ags.port"                     -> "8080",
    "edu.gemini.horizons.host"                -> "gnodb.gemini.edu",
    "edu.gemini.horizons.port"                -> "8443"
  ),
  // log = Some("%a/log/qpt.%u.%g.log"),
  bundles = List(
    BundleSpec("edu.gemini.osgi.main",         Version(4, 2, 1)),
    BundleSpec("edu.gemini.tools.p1pdfmaker",  pv),
    BundleSpec("javax.servlet",                Version(2, 5, 0)),
    // BundleSpec("slf4j.jdk14",                  Version(1, 6, 4)),
    // BundleSpec("org.scala-lang.scala-actors",  Version(2, 10, 5))),
    // BundleSpec("org.scala-lang.scala-reflect", Version(2, 10, 5))),
    // BundleSpec("org.scala-lang.scala-swing",   Version(2, 10, 5))),
    BundleSpec("org.apache.commons.logging",   Version(1, 1, 0))
  )
) extending List()


def dev(pv: Version) = AppConfig(
  id = "test",
  distribution = List(TestDistro),
  props = Map(
    "edu.gemini.model.p1.validate" -> "true"
  ),
  bundles = List(
    BundleSpec("org.apache.felix.gogo.runtime", Version(0, 10, 0)),
    BundleSpec("org.apache.felix.gogo.command", Version(0, 12, 0)),
    BundleSpec("org.apache.felix.gogo.shell",   Version(0, 10, 0))
  )
) extending List(common(pv))

def linux(pv: Version) = AppConfig(
  id = "linux",
  distribution = List(Linux32, Linux64),
  script = Some(file("app/p1pdfmaker/dist/Linux/p1pdfmaker"))
) extending List(common(pv))
