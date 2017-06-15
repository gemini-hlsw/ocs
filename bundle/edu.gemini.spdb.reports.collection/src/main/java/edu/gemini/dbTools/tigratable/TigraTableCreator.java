//
// $Id: TigraTableCreator.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.tigratable;

import edu.gemini.dbTools.html.FtpProps;
import edu.gemini.dbTools.html.FtpUtil$;
import edu.gemini.pot.client.SPDB;
import edu.gemini.sp.vcs.log.VcsLog;
import edu.gemini.sp.vcs2.VcsService;
import edu.gemini.spModel.core.Site;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TigraTableCreator {

    final BundleContext ctx;

    public TigraTableCreator(BundleContext ctx) {
        this.ctx = ctx;
    }

    private static void _writeRows(final TigraTable tt, final BufferedWriter bw) throws IOException {
        bw.write("var TABLE_CONTENT");
        bw.write(tt.getSemesterKey());
        bw.write(" = [\n");
        final List<TigraTableRow> rows = tt.getRows();
        final Iterator<TigraTableRow> it = rows.iterator();
        if (it.hasNext()) {
            bw.write(it.next().toString());
        }
        while (it.hasNext()) {
            bw.write(",\n");
            bw.write(it.next().toString());
        }
        bw.write("\n];\n");
    }

    private static void _writeTables(final List<TigraTable> tables, final File output) throws IOException {
        BufferedWriter bw = null;
        try {
            final FileWriter fw = new FileWriter(output);
            bw = new BufferedWriter(fw);
            for (final TigraTable tt : tables) {
                _writeRows(tt, bw);
            }
        } finally {
            if (bw != null)
                bw.close();
        }
    }

    // curl -i -G -d edu.gemini.dbTools.html.ftpAccount=software -d edu.gemini.dbTools.html.ftpPassword=lemon -d edu.gemini.dbTools.html.ftpHost=foobar -d edu.gemini.dbTools.html.ftpDestDir=crap  http://localhost:8442/cron/tigraTable
    public void run(final File tempDir, final Logger log, final Map<String, String> env, Set<Principal> user) throws Exception {
        final Site site = Site.currentSiteOrNull;
        if (site != null) {

            // The receiving system is expecting the output file to have a specific name, so
            // to keep things simple we use the same name for the local file and the ftp code
            // will simply pick it up and reuse it.
            final File out;
            switch (site) {
                case GN:
                    out = new File(tempDir, "TigraTableNorth.js");
                    break;
                case GS:
                    out = new File(tempDir, "TigraTableSouth.js");
                    break;
                default:
                    throw new RuntimeException("Unknown site: " + site);
            }

            // Create the output file.
            final ServiceReference<VcsLog> ref = ctx.getServiceReference(VcsLog.class);
            if (ref == null) {
                log.severe("Could not find a VcsLog. TigraTable data not updated.");
            } else {
                log.info("Writing TigraTable data to " + out.getAbsolutePath());
                final VcsLog           vcsLog = ctx.getService(ref);
                final List<TigraTable> tables = TigraTableFunctor.getTigraTables(SPDB.get(), vcsLog, user);
                _writeTables(tables, out);

                // FTP the results.
                final FtpProps props = new FtpProps(env);
                FtpUtil$.MODULE$.sendFile(log, out, props);
            }

        } else {
            log.warning("Cannot proceed. No current site.");
        }
    }


}

