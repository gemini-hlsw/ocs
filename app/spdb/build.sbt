
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
          rnorris(v),
          swalker(v),
          dnavarro(v),
          jluhrs(v),
          cquiroz(v),
          sraaphorst(v),
          anunez(v),
          astephens(v),
          with_remote_gogo(v),
            odbtest(v),
              gsodbtest(v),
              gnodbtest(v),
            odbproduction(v),
              gsodb(v),
              gnodb(v),
	      gnagsodb(v)
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
    "-Duser.country=US"
  ),
  props = Map(
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
    BundleSpec("edu.gemini.ags",                         version),
    BundleSpec("edu.gemini.ags.servlet",                 version),
    BundleSpec("edu.gemini.dataman.app",                 version),
    BundleSpec("edu.gemini.itc",                         version),
    BundleSpec("edu.gemini.itc.shared",                  version),
    BundleSpec("edu.gemini.itc.web",                     version),
    BundleSpec("edu.gemini.lchquery.servlet",            version),
    BundleSpec("edu.gemini.obslog",                      version),
    BundleSpec("edu.gemini.oodb.auth.servlet",           version),
    BundleSpec("edu.gemini.oodb.too.url",                version),
    BundleSpec("edu.gemini.oodb.too.window",             version),
    BundleSpec("edu.gemini.osgi.main",                   Version(4, 2, 1)),
    BundleSpec("edu.gemini.p2checker",                   version), // ?
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

// RNORRIS
def rnorris(version: Version) = AppConfig(
  id = "rnorris",
  distribution = List(TestDistro),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/gemsoft/var/data/www/public/reports/obsStatus/test",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=gnconfig.gemini.edu",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/home/dataproc/OT_XML/test",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsags.cl.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.cl.gemini.edu",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=gnconfig.gemini.edu",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/gemsoft/var/data/www/public/reports/test",
    "-Dedu.gemini.site=south",
    "-Xmx1024M",
    "-XX:MaxPermSize=196M"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.root"                    -> "/Users/rnorris/.auxfile",
    "edu.gemini.dataman.gsa.summit.host"         -> "mkofits-lv1new.hi.gemini.edu",
    "edu.gemini.spdb.dir"                        -> "/Users/rnorris/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Rob's ODB (Test)"
  )
) extending List(with_gogo(version), rnorris_credentials(version))

// SWALKER
def swalker(version: Version) = AppConfig(
  id = "swalker",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dedu.gemini.site=south",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/swalker/sftp",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/swalker/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/swalker/cron"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"               -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"              -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"                    -> "/Users/swalker/.auxfile",
    "edu.gemini.dataman.gsa.summit.host"         -> "cpofits-lv1new.cl.gemini.edu",
    "edu.gemini.dbTools.tcs.ephemeris.directory" -> "/Users/swalker/.ephemeris",
    "edu.gemini.services.server.start"           -> "false",
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/Users/swalker/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Shane's ODB (Test)"
  )
) extending List(with_gogo(version), swalker_credentials(version))

// DNAVARRO
def dnavarro(version: Version) = AppConfig(
  id = "dnavarro",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dedu.gemini.site=south",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/jnavarro/sftp",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/jnavarro/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/jnavarro/cron"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"               -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"              -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"                    -> "/Users/jnavarro/.auxfile",
    "edu.gemini.dataman.gsa.summit.host"         -> "cpofits-lv1new.cl.gemini.edu",
    "edu.gemini.dbTools.tcs.ephemeris.directory" -> "/Users/jnavarro/.ephemeris",
    "edu.gemini.services.server.start"           -> "false",
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/Users/jnavarro/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Dannys's ODB (Test)"
  )
) extending List(with_gogo(version), dnavarro_credentials(version))

// FNUSSBER
def fnussber(version: Version) = AppConfig(
  id = "fnussber",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx2000M",
    "-XX:MaxPermSize=196M",
    "-Dedu.gemini.site=north",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/osmirnov/.spdb/sftp",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/osmirnov/.spdb/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/osmirnov/.spdb/cron"
  ),
  props = Map(
    "edu.gemini.smartgcal.host"          -> "localhost",
    "edu.gemini.spdb.dir"                -> "/Users/osmirnov/.spdb/",
    "edu.gemini.auxfile.root"            -> "/Users/osmirnov/.auxfile",
    "edu.gemini.dataman.gsa.summit.host" -> "mkofits-lv1new.hi.gemini.edu",
    "edu.gemini.util.trpc.name"          -> "Florian's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"       -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"      -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"       -> "gsconfig.gemini.edu"
  )
) extending List(with_gogo(version), fnussber_credentials(version))

// SRAAPHORST
def sraaphorst(version: Version) = AppConfig(
  id = "sraaphorst",
  distribution = List(TestDistro),
   vmargs = List(
     "-Xmx1024M",
     "-XX:MaxPermSize=196M",
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
  )
) extending List(with_gogo(version), sraaphorst_credentials(version))

// CQUIROZ
def cquiroz(version: Version) = AppConfig(
  id = "cquiroz",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Dedu.gemini.site=south"
  ),
  props = Map(
    "edu.gemini.spdb.dir"                  -> "/Users/cquiroz/.spdb/",
    "edu.gemini.auxfile.root"              -> "/Users/cquiroz/.auxfile",
    "edu.gemini.dataman.gsa.summit.host"   -> "cpofits-lv1new.cl.gemini.edu",
    "edu.gemini.services.server.start"     -> "false",
    "edu.gemini.util.trpc.name"            -> "Carlos's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gsconfig.gemini.edu",
    "edu.gemini.smartgcal.host"            -> "localhost"
  )
) extending List(with_gogo(version), cquiroz_credentials(version))

// JLUHRS
def jluhrs(version: Version) = AppConfig(
  id = "jluhrs",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Dedu.gemini.site=south"
  ),
  props = Map(
    "edu.gemini.spdb.dir"                -> "/home1/jluhrs/.spdb/",
    "edu.gemini.auxfile.root"            -> "/home1/jluhrs/.auxfile",
    "edu.gemini.dataman.gsa.summit.host" -> "cpofits-lv1new.cl.gemini.edu",
    "edu.gemini.util.trpc.name"          -> "Javier's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"       -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"      -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"       -> "gsconfig.gemini.edu"
  )
) extending List(with_gogo(version), jluhrs_credentials(version))

// ANUNEZ
def anunez(version: Version) = AppConfig(
  id = "anunez",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx1024M",
    "-XX:MaxPermSize=196M",
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dedu.gemini.site=south",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
  ),
  props = Map(
    "edu.gemini.auxfile.root"                    -> "/Users/anunez/.auxfile",
    "edu.gemini.auxfile.fits.dest"               -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"              -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu",
    "edu.gemini.services.server.start"           -> "false",
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/Users/anunez/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Art's ODB (Test)"
  )
) extending List(with_gogo(version), anunez_credentials(version))

// ASTEPHENS
def astephens(version: Version) = AppConfig(
  id = "astephens",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx2048M",
    "-XX:MaxPermSize=196M",
    "-Dedu.gemini.site=north",
    "-Djava.util.logging.config.file=/home/astephens/ocs/logging.properties",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/home/astephens/ocs/sftp",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/home/astephens/ocs/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/home/astephens/ocs/cron"
  ),
  props = Map(
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/home/astephens/ocs/spdb/",
    "edu.gemini.auxfile.root"                    -> "/home/astephens/ocs/auxfile",
    "edu.gemini.dbTools.tcs.ephemeris.directory" -> "/home/astephens/ocs/ephemeris",
    "edu.gemini.dataman.gsa.summit.host"         -> "mkofits-lv1new.hi.gemini.edu",
    "edu.gemini.util.trpc.name"                  -> "Andy's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"               -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"              -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu"
  )
) extending List(with_gogo(version), fnussber_credentials(version))

// ODBTEST
def odbtest(version: Version) = AppConfig(
  id = "odbtest",
  vmargs = List(
    "-d64",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/gemsoft/var/data/www/public/reports/obsStatus/test",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=gnconfig.gemini.edu",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/home/dataproc/OT_XML/test",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=TODO",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=gnconfig.gemini.edu",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/gemsoft/var/data/www/public/reports/test",
    "-Xms6G",
    "-Xmx14G",
    "-XX:+UseConcMarkSweepGC",
    "-XX:MaxPermSize=512M"
  ),
  props = Map(
    "edu.gemini.auxfile.root"                    -> "/home/software/ugemini/auxfile",
    "edu.gemini.dbTools.archive.directory"       -> "/home/software/ugemini/spdb/spdb.archive",
    "edu.gemini.dbTools.tcs.ephemeris.directory" -> "/home/software/ugemini/ephemerides",
    "edu.gemini.smartgcal.svnRootUrl"            -> "http://source.gemini.edu/gcal/branches/development/calibrations",
    "edu.gemini.spdb.dir"                        -> "/home/software/ugemini/spdb/spdb.active",
    "edu.gemini.util.trpc.name"                  -> "Gemini ODB (Test)"
  )
) extending List(with_remote_gogo(version), odbtest_credentials(version))

// ODBPRODUCTION
def odbproduction(version: Version) = AppConfig(
  id = "odbproduction",
  vmargs = List(
    "-d64",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/gemsoft/var/data/www/public/reports/obsStatus",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=gnconfig.gemini.edu",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/home/dataproc/OT_XML",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=TODO",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=gnconfig.gemini.edu",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/gemsoft/var/data/www/public/reports",
    "-Xms6G",
    "-Xmx14G",
    "-XX:+UseConcMarkSweepGC",
    "-XX:MaxPermSize=512M"
  ),
  props = Map(
    "edu.gemini.dbTools.tcs.ephemeris.directory"       -> "/gemsoft/var/ephemerides",
    "edu.gemini.smartgcal.host"                        -> "gsodb",
    "edu.gemini.smartgcal.svnRootUrl"                  -> "http://source.gemini.edu/gcal/trunk/calibrations",
    "edu.gemini.services.telescope.schedule.id.north"  -> "00h6i49qldh5qrteote4nfhldo@group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.id.south"  -> "c4br8ehtv4i8741jfe4ef5saq8@group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.url.north" -> "https://www.google.com/calendar/embed?src=00h6i49qldh5qrteote4nfhldo%40group.calendar.google.com",
    "edu.gemini.services.telescope.schedule.url.south" -> "https://www.google.com/calendar/embed?src=c4br8ehtv4i8741jfe4ef5saq8%40group.calendar.google.com"
  )
) extending List(with_remote_gogo(version), odbproduction_credentials(version))

// GNODBTEST
def gnodbtest(version: Version) = AppConfig(
  id = "gnodbtest",
  distribution = List(Linux64),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=10.2.2.255",
    "-Dedu.gemini.site=north",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsagn.hi.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.hi.gemini.edu"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"       -> "/gemsoft/var/data/ictd/test/GN@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"       -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"      -> "/gemsoft/var/data/finder/GNqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.dataman.gsa.summit.host" -> "mkofits-lv1new.hi.gemini.edu",
    "edu.gemini.oodb.mail.smtpHost"      -> "smtp.hi.gemini.edu",
    "edu.gemini.util.trpc.name"          -> "Gemini North ODB (Test)",
    "osgi.shell.telnet.ip"               -> "10.1.5.36"
  )
) extending List(odbtest(version), gnodbtest_credentials(version))

// GNODB
def gnodb(version: Version) = AppConfig(
  id = "gnodb",
  distribution = List(Linux64),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=10.2.2.255",
    "-Dedu.gemini.site=north",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsagn.hi.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.hi.gemini.edu"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/GN@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GNqueue/Finders/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"              -> "/mount/wikiwiki/odbhome/ugemini/auxfile",
    "edu.gemini.dataman.gsa.summit.host"   -> "fits.hi.gemini.edu",
    "edu.gemini.dbTools.archive.directory" -> "/mount/wikiwiki/odbhome/ugemini/spdb/spdb.archive",
    "edu.gemini.oodb.mail.smtpHost"        -> "smtp.hi.gemini.edu",
    "edu.gemini.spdb.dir"                  -> "/mount/wikiwiki/odbhome/ugemini/spdb/spdb.active",
    "edu.gemini.util.trpc.name"            -> "Gemini North ODB",
    "osgi.shell.telnet.ip"                 -> "10.2.4.77"
  )
) extending List(odbproduction(version), gnodb_credentials(version))

// GN AGS ODB: hbfauxodb-lv1 / gnauxodb
def gnagsodb(version: Version) = AppConfig(
  id = "gnagsodb",
  distribution = List(Linux64),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=10.2.2.255",
    "-Dedu.gemini.site=north",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsagn.hi.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.hi.gemini.edu",
    "-Xms2G",
    "-Xmx2G"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/GN@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GNqueue/Finders/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"              -> "/home/software/.auxfile",
    "edu.gemini.dataman.gsa.summit.host"   -> "fits.hi.gemini.edu",
    "edu.gemini.oodb.mail.smtpHost"        -> "smtp.hi.gemini.edu",
    "edu.gemini.util.trpc.name"            -> "Gemini North AGS ODB",
    "osgi.shell.telnet.ip"                 -> "10.1.46.11"
  )
) extending List(odbproduction(version), gnodb_credentials(version))

// GSODBTEST
def gsodbtest(version: Version) = AppConfig(
  id = "gsodbtest",
  distribution = List(Linux64),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dedu.gemini.site=south",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsags.cl.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.cl.gemini.edu"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"       -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"       -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"      -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.dataman.gsa.summit.host" -> "cpofits-lv1.cl.gemini.edu",
    "edu.gemini.oodb.mail.smtpHost"      -> "smtp.cl.gemini.edu",
    "edu.gemini.util.trpc.name"          -> "Gemini South ODB (Test)",
    "osgi.shell.telnet.ip"               -> "172.17.55.77"
  )
) extending List(odbtest(version), gsodbtest_credentials(version))

// GSODB
def gsodb(version: Version) = AppConfig(
  id = "gsodb",
  distribution = List(Linux64),
  vmargs = List(
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dedu.gemini.site=south",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=gsags.cl.gemini.edu",
    "-Dcron.odbMail.SITE_SMTP_SERVER=smtp.cl.gemini.edu"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GSqueue/Finders/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"              -> "/mount/petrohue/odbhome/ugemini/auxfile",
    "edu.gemini.dataman.gsa.summit.host"   -> "fits.cl.gemini.edu",
    "edu.gemini.dbTools.archive.directory" -> "/mount/petrohue/odbhome/ugemini/spdb/spdb.archive",
    "edu.gemini.oodb.mail.smtpHost"        -> "smtp.cl.gemini.edu",
    "edu.gemini.spdb.dir"                  -> "/mount/petrohue/odbhome/ugemini/spdb/spdb.active",
    "edu.gemini.util.trpc.name"            -> "Gemini South ODB",
    "osgi.shell.telnet.ip"                 -> "172.17.5.77"
  ),
  bundles = List(
    BundleSpec(50, "edu.gemini.smartgcal.servlet", version)
  )
) extending List(odbproduction(version), gsodb_credentials(version))


