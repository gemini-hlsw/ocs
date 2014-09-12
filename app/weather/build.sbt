
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }

ocsAppSettings

ocsAppManifest := {
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "weather",
    name = "Weather Station",
    label = None,
    version = ocsVersion.value.toString,
    configs = List(
      common(v)
    )
  )
}

def common(version: Version) = AppConfig(
  id = "common",
  distribution = List(TestDistro),
  args = Nil,
  vmargs = List(
    "-Xmx1024M",
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255"
  ),
  props = Map(
    "org.osgi.framework.storage.clean" -> "onFirstInit",
    "org.osgi.framework.startlevel.beginning" -> "100",
    "org.osgi.framework.bootdelegation" -> "*"
  ),
  icon = None,
  log = None,
  script = None,
  bundles = List(
    BundleSpec("org.scala-lang.scala-reflect",       Version(2,10, 1)),
    BundleSpec("edu.gemini.osgi.main",               Version(4, 2, 1)),
    BundleSpec("edu.gemini.osgi.main",               Version(4, 2, 1)),
    BundleSpec("edu.gemini.shared.ca",               version),
    BundleSpec("edu.gemini.spdb.reports.collection", version),
    BundleSpec("org.apache.velocity",                Version(1, 4, 0)),
    BundleSpec("javax.servlet",                      Version(2, 5, 0)),
    BundleSpec("org.dom4j",                          Version(1, 5, 1)),
    BundleSpec("slf4j.api",                          Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                        Version(1, 6, 4))
  )
) extending Nil

