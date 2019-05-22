
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }
import OcsCredentials.Spdb._

ocsAppSettings

// Application project for Science Program Database
ocsAppManifest := {
  // ok, now define the app
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "itc",
    name = "Integrated Time Calculator",
    version = ocsVersion.value.toString,
    configs = List(
      common(v),
        with_gogo(v),
          itc(v),
          with_remote_gogo(v),
            itctest_32(v),
            itctest_32(v),
            itcproduction_64(v),
            itcproduction_64(v)
    )
  )
}

// COMMON
def common(version: Version) = AppConfig(
  id = "common",
  vmargs = List(
    "-Dcom.sun.management.jmxremote",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Dcom.sun.management.jmxremote.port=2407",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Djava.awt.headless=true",
    "-Dnetworkaddress.cache.ttl=60",
    "-Duser.language=en",
    "-Duser.country=US",
    "-Xmx2048M"
  ),
  props = Map(
    "org.osgi.service.http.port"                 -> "9080",
    "edu.gemini.spdb.mode"                       -> "local",
    "edu.gemini.util.security.auth.startServer"  -> "true",
    "org.osgi.framework.bootdelegation"          -> "*",
    "org.osgi.framework.startlevel.beginning"    -> "99",
    "org.osgi.framework.storage.clean"           -> "onFirstInit",
    "org.osgi.service.http.secure.enabled"       -> "false" // testing only, don't turn on secure port
  ),
  log = Some("log/itc.%u.%g.log"),
  bundles = List(
    BundleSpec("com.jgoodies.looks",                     Version(2, 4, 1)),
    BundleSpec("com.mchange.c3p0",                       Version(0, 9, 5)),
    BundleSpec("edu.gemini.util.osgi",                   version),
    BundleSpec("edu.gemini.osgi.main",                   Version(4, 2, 1)),
    BundleSpec("edu.gemini.util.log.extras",             version),
    BundleSpec("edu.gemini.itc",                         version),
    BundleSpec("edu.gemini.itc.shared",                  version),
    BundleSpec("edu.gemini.itc.web",                     version),
    BundleSpec("org.apache.commons.io",                  Version(2, 0, 1)),
    BundleSpec("org.h2",                                 Version(1, 3, 170)),
    BundleSpec("org.ops4j.pax.web.pax-web-extender-war", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jetty-bundle", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jsp",          Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-spi",          Version(1, 1, 13)),
    BundleSpec("slf4j.api",                              Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                            Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",             Version(1, 1, 0))
  )
) extending List(common_credentials(version))

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

// ITC
def itc(version: Version) = AppConfig(
  id = "itc",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-Dedu.gemini.site=north"
  ),
  props = Map.empty
) extending List(with_gogo(version))

// ITC test
def itctest_32(version: Version) = AppConfig(
  id = "itctest32",
  distribution = List(Linux32)
) extending List(with_remote_gogo(version))

// ITC production
def itcproduction_32(version: Version) = AppConfig(
  id = "itcproduction32",
  distribution = List(Linux32)
) extending List(with_remote_gogo(version))

// ITC test
def itctest_64(version: Version) = AppConfig(
  id = "itctest64",
  distribution = List(Linux64)
) extending List(with_remote_gogo(version))

// ITC production
def itcproduction_64(version: Version) = AppConfig(
  id = "itcproduction64",
  distribution = List(Linux64)
) extending List(with_remote_gogo(version))

