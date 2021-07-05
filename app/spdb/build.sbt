
import OcsKeys._
import edu.gemini.osgi.tools.Version
import edu.gemini.osgi.tools.app.{ Configuration => AppConfig, _ }
import edu.gemini.osgi.tools.app.Configuration.Distribution.{ Test => TestDistro, _ }
import OcsCredentials.Spdb._

ocsAppSettings

// Application project for Science Program Database
ocsAppManifest := {
  // check for link to keystore
  val f = baseDirectory.value / "conf" / "gemKeystore"
  if (!f.isFile)
    println(s"[${scala.Console.RED}error${scala.Console.RESET}] Keystore file ${f} was not found ... please link it!")
  // ok, now define the app
  val v = ocsVersion.value.toBundleVersion
  Application(
    id = "spdb",
    name = "Science Program Database",
    version = ocsVersion.value.toString,
    configs = List(
      common(v), // note, each config extends xxx_credentials, defined in $TOP/project/OcsCredentials.scala
        with_gogo(v),
          sraaphorst(v),
          with_remote_gogo(v)
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
    "-Djava.net.preferIPv4Stack=true",
    "-Dnetworkaddress.cache.ttl=60",
    "-Duser.language=en",
    "-Duser.country=US"
  ),
  props = Map(
    "cron.odbMail.mailer.type"                         -> "development",
    "edu.gemini.auxfile.chunkSize"                     -> "32768",
    "edu.gemini.auxfile.fits.dest"                     -> "/please/specify/in/host/specific/property/file",
    "edu.gemini.auxfile.other.dest"                    -> "/please/specify/in/host/specific/property/file",
    "edu.gemini.auxfile.other.host"                    -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.server"                        -> "true",
    "edu.gemini.dataman.gsa.archive.host"              -> "archive.gemini.edu",
    "edu.gemini.dataman.poll.obsRefresh"               -> "PT2M",
    "edu.gemini.dataman.poll.archive.tonight"          -> "PT1M",
    "edu.gemini.dataman.poll.archive.thisWeek"         -> "PT15M",
    "edu.gemini.dataman.poll.archive.allPrograms"      -> "P1D",
    "edu.gemini.dataman.poll.summit.tonight"           -> "PT15S",
    "edu.gemini.dataman.poll.summit.thisWeek"          -> "PT15M",
    "edu.gemini.dataman.poll.summit.allPrograms"       -> "P1D",
    "edu.gemini.dbTools.maskcheck.nagdelay"            -> "P7D",
    "edu.gemini.filefilter.excludes"                   -> "tmp.*",
    "edu.gemini.filefilter.excludes.2"                 -> ".*b\\.fits",
    "edu.gemini.filefilter.includes"                   -> ".*\\.fits",
    "edu.gemini.oodb.mail.senderAddr"                  -> "no_reply@gemini.edu",
    "edu.gemini.oodb.mail.senderName"                  -> "Gemini ODB",
    "edu.gemini.oodb.mail.smtpHost"                    -> "smtp.cl.gemini.edu",
    "edu.gemini.services.telescope.schedule.id.north"  -> "m882dsr2asoddjcte5250lvn6c@group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.url.north" -> "https://www.google.com/calendar/embed?src=m882dsr2asoddjcte5250lvn6c%40group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.id.south"  -> "aiv4b2forl2gaovsu8na39vs5s@group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.url.south" -> "https://www.google.com/calendar/embed?src=aiv4b2forl2gaovsu8na39vs5s%40group.calendar.google.com",
    "edu.gemini.smartgcal.host"                        -> "gsodbtest",
    "edu.gemini.smartgcal.svnRootUrl"                  -> "http://source.gemini.edu/gcal/branches/development/calibrations",
    "edu.gemini.smartgcal.updateInterval"              -> "7200",
    "edu.gemini.spdb.mode"                             -> "local",
    "edu.gemini.too.event.mode"                        -> "service",
    "edu.gemini.util.security.auth.startServer"        -> "true",
    "org.ops4j.pax.web.ssl.keystore"                   -> "conf/gemKeystore",
    "org.osgi.framework.bootdelegation"                -> "*",
    "org.osgi.framework.startlevel.beginning"          -> "99",
    "org.osgi.framework.storage.clean"                 -> "onFirstInit",
    "org.osgi.service.http.port"                       -> "8442",
    "org.osgi.service.http.port.secure"                -> "8443",
    "org.osgi.service.http.secure.enabled"             -> "true"
  ),
  log = Some("log/spdb.%u.%g.log"),
  bundles = List(
    BundleSpec("com.jgoodies.looks",                     Version(2, 4, 1)),
    BundleSpec("com.mchange.c3p0",                       Version(0, 9, 5)),
    BundleSpec("com.mysql.jdbc",                         Version(5, 1, 46)),
    BundleSpec("edu.gemini.ags",                         version),
    BundleSpec("edu.gemini.ags.servlet",                 version),
    BundleSpec("edu.gemini.dataman.app",                 version),
    BundleSpec("edu.gemini.ictd",                        version),
    BundleSpec("edu.gemini.itc",                         version),
    BundleSpec("edu.gemini.itc.shared",                  version),
    BundleSpec("edu.gemini.itc.web",                     version),
    BundleSpec("edu.gemini.lchquery.servlet",            version),
    BundleSpec("edu.gemini.obslog",                      version),
    BundleSpec("edu.gemini.oodb.auth.servlet",           version),
    BundleSpec("edu.gemini.oodb.too.url",                version),
    BundleSpec("edu.gemini.oodb.too.window",             version),
    BundleSpec("edu.gemini.osgi.main",                   Version(4, 2, 1)),
    BundleSpec("edu.gemini.p2checker",                   version),
    BundleSpec("edu.gemini.programexport.servlet",       version),
    BundleSpec("edu.gemini.phase2.skeleton.servlet",     version),
    BundleSpec("edu.gemini.qpt.shared",                  version),
    BundleSpec("edu.gemini.services.server",             version),
    BundleSpec("edu.gemini.seqexec.odb",                 version),
    BundleSpec("edu.gemini.smartgcal.odbinit",           version),
    BundleSpec("edu.gemini.smartgcal.servlet",           version),
    BundleSpec("edu.gemini.sp.vcs",                      version),
    BundleSpec("edu.gemini.sp.vcs.log",                  version),
    BundleSpec("edu.gemini.sp.vcs.reg",                  version),
    BundleSpec("edu.gemini.spdb.rollover.servlet",       version),
    BundleSpec("edu.gemini.spdb.shell",                  version),
    BundleSpec("edu.gemini.util.log.extras",             version),
    BundleSpec("edu.gemini.wdba.session.client",         version),
    BundleSpec("edu.gemini.wdba.xmlrpc.server",          version),
    BundleSpec("jsky.app.ot.shared",                     version),
    BundleSpec("org.apache.commons.io",                  Version(2, 0, 1)),
    BundleSpec("org.h2",                                 Version(1, 3, 170)),
    BundleSpec("org.ops4j.pax.web.pax-web-extender-war", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jetty-bundle", Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-jsp",          Version(1, 1, 13)),
    BundleSpec("org.ops4j.pax.web.pax-web-spi",          Version(1, 1, 13)),
    BundleSpec("slf4j.api",                              Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                            Version(1, 6, 4)),
    BundleSpec("org.apache.commons.logging",             Version(1, 1, 0)),
    BundleSpec("com.cosylab.epics.caj",                  Version(1, 0, 2)),
    BundleSpec("io.argonaut",                            Version(6, 2, 0)),
    BundleSpec("monocle.core",                           Version(1, 2, 1)),
    BundleSpec("monocle.macro",                          Version(1, 2, 1)),
    BundleSpec("edu.gemini.shared.ca",                   version),
    BundleSpec("edu.gemini.spdb.reports.collection",     version)
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

// SRAAPHORST
def sraaphorst(version: Version) = AppConfig(
  id = "sraaphorst",
  distribution = List(TestDistro),
   vmargs = List(
     "-Xmx1024M",
     "-Dedu.gemini.site=south",
     "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
     "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/sraaphorst/.spdb/sftp",
     "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
     "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/sraaphorst/cron",
     "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
     "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/sraaphorst/cron"
  ),
  props = Map(
    "edu.gemini.smartgcal.host"            -> "localhost",
    "edu.gemini.spdb.dir"                  -> "/Users/sraaphorst/.spdb/",
    "edu.gemini.auxfile.root"              -> "/Users/sraaphorst/.auxfile",
    "edu.gemini.util.trpc.name"            -> "Sebastian's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gsconfig.gemini.edu",
    "edu.gemini.dataman.gsa.summit.host"   -> "cpofits-lv1new.cl.gemini.edu",
    "edu.gemini.dbTools.archive.directory" -> "/Users/sraaphorst/tmp/archiver",
    "osgi.shell.telnet.ip"                 -> "172.16.77.244"
    //"osgi.shell.telnet.ip"                -> "192.168.1.5"
  )
) extending List(with_gogo(version), sraaphorst_credentials(version))
