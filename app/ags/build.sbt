
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }
import OcsCredentials.Ags._

ocsAppSettings

// Application for AGS servlets
ocsAppManifest := {
  // check for link to keystore
  val f = baseDirectory.value / "conf" / "gemKeystore"
  if (!f.isFile)
    println(s"[${scala.Console.RED}error${scala.Console.RESET}] Keystore file ${f} was not found ... please link it!")
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "ags",
    name = "Automatic Guide Star Servlet",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
        ags(v),
        gn(v),
        gs(v)
    )
  )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Dcom.sun.management.jmxremote",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Dcom.sun.management.jmxremote.port=2408",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Djava.awt.headless=true",
    "-Djava.net.preferIPv4Stack=true",
    "-Dnetworkaddress.cache.ttl=60",
    "-Duser.language=en",
    "-Duser.country=US",
    "-Xmx3072M",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006",
    "-d64"
  ),
  props = Map(
    "edu.gemini.spdb.mode"                             -> "api",
    "edu.gemini.util.security.auth.startServer"        -> "false",
    "org.osgi.framework.bootdelegation"                -> "*",
    "org.osgi.framework.startlevel.beginning"          -> "99",
    "org.osgi.framework.storage.clean"                 -> "onFirstInit",
    "org.osgi.service.http.port"                       -> "8442",
    "org.osgi.service.http.port.secure"                -> "8443",
    "org.osgi.service.http.secure.enabled"             -> "true",
    "org.ops4j.pax.web.ssl.keystore"                   -> "conf/gemKeystore"
  ),
  log = Some("log/ags.%u.%g.log"),
  bundles = List(
    BundleSpec("edu.gemini.ags",                         version),
    BundleSpec("edu.gemini.ags.servlet",                 version),
    BundleSpec("edu.gemini.osgi.main",                   Version(4, 2, 1)),
    BundleSpec("edu.gemini.util.log.extras",             version),
    BundleSpec("org.apache.commons.io",                  Version(2, 0, 1)),
    BundleSpec("slf4j.api",                              Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                            Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",             Version(1, 1, 0)),
    BundleSpec("org.apache.felix.http.jetty",            Version(2, 2, 0)),
    BundleSpec("io.argonaut",                            Version(6, 2, 0)),
    BundleSpec("edu.gemini.pot",                         version),
    BundleSpec("org.ops4j.pax.web.pax-web-extender-war", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jetty-bundle", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jsp",          Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-spi",          Version(1, 1, 13))
  )
) extending List(common_credentials(version), with_gogo_credentials(version))


// WITH-GOGO
def with_gogo(version: Version) = AppConfig(
  id = "with-gogo",
  bundles = List(
    BundleSpec(10, "org.apache.felix.gogo.command", Version(0, 12, 0)),
    BundleSpec(10, "org.apache.felix.gogo.runtime", Version(0, 10, 0)),
    BundleSpec(10, "org.apache.felix.gogo.shell",   Version(0, 10, 0))
  )
) extending List(common(version), with_gogo_credentials(version))

// WITH-REMOTE-GOGO
def with_remote_gogo(version: Version) = AppConfig(
  id = "with-remote-gogo",
  props = Map(
    "gosh.args"                       -> "--nointeractive",
    "osgi.shell.telnet.port"          -> "8224",
    "osgi.shell.telnet.maxconn"       -> "4",
    "osgi.shell.telnet.socketTimeout" -> "30000"
  ),
  bundles = List(
    BundleSpec(10, "org.apache.felix.shell.remote", Version(1, 1, 2))
  )
) extending List(with_gogo(version), with_remote_gogo_credentials(version))

def ags(version: Version) = AppConfig(
  id = "ags",
  distribution = List(TestDistro),
  vmargs = List(
    "-Dedu.gemini.site=south"
  ),
  props = Map.empty
) extending List(with_gogo(version))

def gn(version: Version) = AppConfig(
  id = "gn",
  distribution = List(Linux64),
  vmargs = List(
    "-Dedu.gemini.site=north"
  ),
  props = Map.empty
) extending List(with_remote_gogo(version))

def gs(version: Version) = AppConfig(
  id = "gs",
  distribution = List(Linux64),
  vmargs = List(
    "-Dedu.gemini.site=south"
  ),
  props = Map.empty
) extending List(with_remote_gogo(version))

