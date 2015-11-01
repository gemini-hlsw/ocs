
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
          jluhrs(v),
          abrighton(v),
          fnussber(v),
          cquiroz(v),
          sraaphorst(v),
          with_remote_gogo(v),
            odbtest(v),
              gsodbtest(v),
              gnodbtest(v),
            odbproduction(v),
              gsodb(v),
              gnodb(v)
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
    "-Dnetworkaddress.cache.ttl=60"
  ),
  props = Map(
    "edu.gemini.auxfile.chunkSize"                     -> "32768",
    "edu.gemini.auxfile.fits.dest"                     -> "/please/specify/in/host/specific/property/file",
    "edu.gemini.auxfile.other.dest"                    -> "/please/specify/in/host/specific/property/file",
    "edu.gemini.auxfile.other.host"                    -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.server"                        -> "true",
    "edu.gemini.dataman.dbScanMinutes"                 -> "15",
    "edu.gemini.dataman.gsaCrcUrl"                     -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/geminiInfo?file=%FILE%&options=-ufilecrc",
    "edu.gemini.dataman.gsa.query.host"                -> "hbffits2.hi.gemini.edu",
    "edu.gemini.dataman.xfer.cadcGroup.gsa"            -> "cadcops",
    "edu.gemini.dataman.xfer.cadcRoot.gsa"             -> "/home/cadcops/installed/mdIngest",
    "edu.gemini.dataman.xfer.destDir.gsa"              -> "/u1/CADC/dataflow",
    "edu.gemini.dataman.xfer.mdIngestScript.gsa"       -> "/home/cadcops/installed/mdIngest/mdIngest",
    "edu.gemini.dataman.xfer.tempDir.gsa"              -> "/u1/CADC/dataflow/.xfer",
    "edu.gemini.dirmon.activeSetPollPeriod"            -> "5000",
    "edu.gemini.dirmon.activeSetSize"                  -> "100",
    "edu.gemini.dirmon.fullDirPollPeriod"              -> "120000",
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
    "edu.gemini.smartgcal.host"                        -> "gsodbtest2",
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
    BundleSpec("edu.gemini.horizons.server",             version),
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
    BundleSpec("edu.gemini.seqexec.shared",              version),
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
    BundleSpec("org.scala-lang.scala-reflect",           Version(2, 10, 1)),
    BundleSpec("org.scalaz.concurrent",                  Version(7, 0, 5)),
    BundleSpec("slf4j.api",                              Version(1, 6, 4)),
    BundleSpec("slf4j.jdk14",                            Version(1, 6, 4)),
    BundleSpec("squants",                                Version(0, 5, 3)),
    BundleSpec("org.apache.commons.logging",             Version(1, 1, 0)),
    BundleSpec("com.cosylab.epics.caj",                  Version(1, 0, 2)),
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
    "gosh.args"              -> "--nointeractive",
    "osgi.shell.telnet.port" -> "8224"
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
    "edu.gemini.dataman.gsaXferUrl"              -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.rawDir"                  -> "/Users/rnorris/tmp/dataman/raw",
    "edu.gemini.dataman.smtpHost"                -> "smtp.cl.gemini.edu",
    "edu.gemini.dataman.workDir"                 -> "/Users/rnorris/tmp/dataman/work",
    "edu.gemini.dataman.xfer.cadcGroup.gsa"      -> "rnorris",
    "edu.gemini.dataman.xfer.cadcRoot.gsa"       -> "/Users/rnorris/tmp/dataman/cadc",
    "edu.gemini.dataman.xfer.destDir.base"       -> "/Users/rnorris/tmp/dataman/base",
    "edu.gemini.dataman.xfer.destDir.gsa"        -> "/Users/rnorris/tmp/dataman/gsa",
    "edu.gemini.dataman.xfer.host.base"          -> "cookie",
    "edu.gemini.dataman.xfer.host.gsa"           -> "cookie",
    "edu.gemini.dataman.xfer.mdIngestScript.gsa" -> "/Users/rnorris/tmp/dataman/cadc/mdIngest",
    "edu.gemini.dataman.xfer.tempDir.base"       -> "/Users/rnorris/tmp/dataman/base/.xfer",
    "edu.gemini.dataman.xfer.tempDir.gsa"        -> "/Users/rnorris/tmp/dataman/gsa/.xfer",
    "edu.gemini.datasetfile.workDir"             -> "/Users/rnorris/tmp/dataman/work",
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
    "edu.gemini.dataman.gsaXferUrl"              -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.rawDir"                  -> "/Users/swalker/dataman/raw",
    "edu.gemini.dataman.smtpHost"                -> "smtp.cl.gemini.edu",
    "edu.gemini.dataman.workDir"                 -> "/Users/swalker/dataman/work",
    "edu.gemini.dataman.xfer.cadcGroup.gsa"      -> "staff",
    "edu.gemini.dataman.xfer.cadcRoot.gsa"       -> "/Users/swalker/dataman/cadc",
    "edu.gemini.dataman.xfer.destDir.base"       -> "/Users/swalker/dataman/base",
    "edu.gemini.dataman.xfer.destDir.gsa"        -> "/Users/swalker/dataman/gsa",
    "edu.gemini.dataman.xfer.host.base"          -> "localhost",
    "edu.gemini.dataman.xfer.host.gsa"           -> "localhost",
    "edu.gemini.dataman.xfer.mdIngestScript.gsa" -> "/Users/swalker/dataman/cadc/mdIngest",
    "edu.gemini.dataman.xfer.tempDir.base"       -> "/Users/swalker/dataman/base/.xfer",
    "edu.gemini.dataman.xfer.tempDir.gsa"        -> "/Users/swalker/dataman/gsa/.xfer",
    "edu.gemini.datasetfile.workDir"             -> "/Users/swalker/dataman/work",
    "edu.gemini.services.server.start"           -> "false",
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/Users/swalker/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Shane's ODB (Test)"
  )
) extending List(with_gogo(version), swalker_credentials(version))

// FNUSSBER
def fnussber(version: Version) = AppConfig(
  id = "fnussber",
  distribution = List(TestDistro),
  vmargs = List(
    "-Xmx2000M",
    "-XX:MaxPermSize=196M",
    "-Dedu.gemini.site=north",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/fnussber/.spdb/sftp",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/fnussber/.spdb/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/fnussber/.spdb/cron"
  ),
  props = Map(
    "edu.gemini.smartgcal.host"     -> "localhost",
    "edu.gemini.spdb.dir"           -> "/Users/fnussber/.spdb/",
    "edu.gemini.auxfile.root"       -> "/Users/fnussber/.auxfile",
    "edu.gemini.util.trpc.name"     -> "Florian's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"  -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest" -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"  -> "gsconfig.gemini.edu"
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
     "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/sraaphor/.spdb/sftp",
     "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
     "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/sraaphor/cron",
     "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
     "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/sraaphor/cron"
  ),
  props = Map(
    "edu.gemini.smartgcal.host"            -> "localhost",
    "edu.gemini.spdb.dir"                  -> "/Users/sraaphor/.spdb/",
    "edu.gemini.auxfile.root"              -> "/Users/sraaphor/.auxfile",
    "edu.gemini.util.trpc.name"            -> "Sebastian's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"         -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest"        -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"         -> "gsconfig.gemini.edu",
    "edu.gemini.dbTools.archive.directory" -> "/Users/sraaphor/tmp/archiver"
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
    "edu.gemini.spdb.dir"           -> "/Users/cquiroz/.spdb/",
    "edu.gemini.auxfile.root"       -> "/Users/cquiroz/.auxfile",
    "edu.gemini.util.trpc.name"     -> "Carlos's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"  -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest" -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"  -> "gsconfig.gemini.edu"
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
    "edu.gemini.spdb.dir"           -> "/home1/jluhrs/.spdb/",
    "edu.gemini.auxfile.root"       -> "/home1/jluhrs/.auxfile",
    "edu.gemini.util.trpc.name"     -> "Javier's ODB (Test)",
    "edu.gemini.auxfile.fits.dest"  -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.other.dest" -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"  -> "gsconfig.gemini.edu"
  )
) extending List(with_gogo(version), jluhrs_credentials(version))

// ABRIGHTON
def abrighton(version: Version) = AppConfig(
  id = "abrighton",
  distribution = List(TestDistro),
  vmargs = List(
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
    "-Dcom.cosylab.epics.caj.CAJContext.addr_list=172.17.2.255",
    "-Dcron.*.edu.gemini.dbTools.html.ftpDestDir=/Users/abrighto/sftp",
    "-Dcron.*.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpDestDir=/Users/abrighto/cron",
    "-Dcron.archive.edu.gemini.dbTools.html.ftpHost=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.host=localhost",
    "-Dcron.reports.edu.gemini.spdb.reports.public.remotedir=/Users/abrighto/cron",
    "-Dedu.gemini.site=south",
    "-Xmx1024M",
    "-XX:MaxPermSize=196M"
  ),
  props = Map(
    "edu.gemini.auxfile.fits.dest"               -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"               -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"              -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.root"                    -> "/Users/abrighto/.auxfile",
    "edu.gemini.dataman.gsaXferUrl"              -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.rawDir"                  -> "/Users/abrighto/dataman/raw",
    "edu.gemini.dataman.smtpHost"                -> "smtp.cl.gemini.edu",
    "edu.gemini.dataman.workDir"                 -> "/Users/abrighto/dataman/work",
    "edu.gemini.dataman.xfer.cadcGroup.gsa"      -> "staff",
    "edu.gemini.dataman.xfer.cadcRoot.gsa"       -> "/Users/abrighto/dataman/cadc",
    "edu.gemini.dataman.xfer.destDir.base"       -> "/Users/abrighto/dataman/base",
    "edu.gemini.dataman.xfer.destDir.gsa"        -> "/Users/abrighto/dataman/gsa",
    "edu.gemini.dataman.xfer.host.base"          -> "localhost",
    "edu.gemini.dataman.xfer.host.gsa"           -> "localhost",
    "edu.gemini.dataman.xfer.mdIngestScript.gsa" -> "/Users/abrighto/dataman/cadc/mdIngest",
    "edu.gemini.dataman.xfer.tempDir.base"       -> "/Users/abrighto/dataman/base/.xfer",
    "edu.gemini.dataman.xfer.tempDir.gsa"        -> "/Users/abrighto/dataman/gsa/.xfer",
    "edu.gemini.datasetfile.workDir"             -> "/Users/abrighto/dataman/work",
    "edu.gemini.smartgcal.host"                  -> "localhost",
    "edu.gemini.spdb.dir"                        -> "/Users/abrighto/.spdb/",
    "edu.gemini.util.trpc.name"                  -> "Brightons's ODB (Test)"
  )
) extending List(with_gogo(version), abrighton_credentials(version))

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
    "edu.gemini.spdb.dir"                  -> "/home/software/ugemini/spdb/spdb.active",
    "edu.gemini.auxfile.root"              -> "/home/software/ugemini/auxfile",
    "edu.gemini.smartgcal.svnRootUrl"      -> "http://source.gemini.edu/gcal/branches/development/calibrations",
    "edu.gemini.util.trpc.name"            -> "Gemini ODB (Test)",
    "edu.gemini.dbTools.archive.directory" -> "/home/software/ugemini/spdb/spdb.archive"
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
    "edu.gemini.auxfile.fits.dest"     -> "/gemsoft/var/data/ictd/test/GN@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"     -> "gnconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"    -> "/gemsoft/var/data/finder/GNqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.dataman.gsaXferUrl"    -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsagn&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.rawDir"        -> "/mount/wikiwiki/dhs",
    "edu.gemini.dataman.smtpHost"      -> "smtp.hi.gemini.edu",
    "edu.gemini.dataman.workDir"       -> "/mount/wikiwiki/dataflow",
    "edu.gemini.dataman.xfer.host.gsa" -> "gsagn",
    "edu.gemini.datasetfile.workDir"   -> "/mount/wikiwiki/dataflow",
    "edu.gemini.oodb.mail.smtpHost"    -> "smtp.hi.gemini.edu",
    "edu.gemini.util.trpc.name"        -> "Gemini North ODB (Test)",
    "osgi.shell.telnet.ip"             -> "10.1.5.83"
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
    "edu.gemini.dataman.gsaXferUrl"        -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsagn&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.gsa.query.host"    -> "fits.hi.gemini.edu",
    "edu.gemini.dataman.rawDir"            -> "/mount/wikiwiki/dhs",
    "edu.gemini.dataman.smtpHost"          -> "smtp.hi.gemini.edu",
    "edu.gemini.dataman.workDir"           -> "/mount/wikiwiki/dataflow",
    "edu.gemini.dataman.xfer.host.gsa"     -> "gsagn",
    "edu.gemini.datasetfile.workDir"       -> "/mount/wikiwiki/dataflow",
    "edu.gemini.dbTools.archive.directory" -> "/mount/wikiwiki/odbhome/ugemini/spdb/spdb.archive",
    "edu.gemini.oodb.mail.smtpHost"        -> "smtp.hi.gemini.edu",
    "edu.gemini.spdb.dir"                  -> "/mount/wikiwiki/odbhome/ugemini/spdb/spdb.active",
    "edu.gemini.util.trpc.name"            -> "Gemini North ODB",
    "osgi.shell.telnet.ip"                 -> "10.2.4.77"
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
    "edu.gemini.auxfile.fits.dest"     -> "/gemsoft/var/data/ictd/test/GS@SEMESTER@/@PROG_ID@",
    "edu.gemini.auxfile.fits.host"     -> "gsconfig.gemini.edu",
    "edu.gemini.auxfile.other.dest"    -> "/gemsoft/var/data/finder/GSqueue/Finders-Test/@SEMESTER@/@PROG_ID@",
    "edu.gemini.dataman.gsaXferUrl"    -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.rawDir"        -> "/mount/petrohue/dhs",
    "edu.gemini.dataman.smtpHost"      -> "smtp.cl.gemini.edu",
    "edu.gemini.dataman.workDir"       -> "/mount/petrohue/dataflow",
    "edu.gemini.dataman.xfer.host.gsa" -> "gsags",
    "edu.gemini.datasetfile.workDir"   -> "/mount/petrohue/dataflow",
    "edu.gemini.oodb.mail.smtpHost"    -> "smtp.cl.gemini.edu",
    "edu.gemini.util.trpc.name"        -> "Gemini South ODB (Test)",
    "osgi.shell.telnet.ip"             -> "172.17.55.81"
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
    "edu.gemini.dataman.gsaXferUrl"        -> "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&file=%FILE%&mode=file&format=text&timezone=utc&reverse=1",
    "edu.gemini.dataman.gsa.query.host"    -> "fits.cl.gemini.edu",
    "edu.gemini.dataman.rawDir"            -> "/mount/petrohue/dhs",
    "edu.gemini.dataman.smtpHost"          -> "smtp.cl.gemini.edu",
    "edu.gemini.dataman.workDir"           -> "/mount/petrohue/dataflow",
    "edu.gemini.dataman.xfer.host.gsa"     -> "gsags",
    "edu.gemini.datasetfile.workDir"       -> "/mount/petrohue/dataflow",
    "edu.gemini.dbTools.archive.directory" -> "/mount/petrohue/odbhome/ugemini/spdb/spdb.archive",
    "edu.gemini.oodb.mail.smtpHost"        -> "smtp.cl.gemini.edu",
    "edu.gemini.spdb.dir"                  -> "/mount/petrohue/odbhome/ugemini/spdb/spdb.active",
    "edu.gemini.util.trpc.name"            -> "Gemini South ODB",
    "osgi.shell.telnet.ip"                 -> "172.17.5.77"
  ),
  bundles = List(
    BundleSpec(50, "edu.gemini.smartgcal.servlet", version)
  )
) extending List(odbproduction(version), gsodbtest_credentials(version))


