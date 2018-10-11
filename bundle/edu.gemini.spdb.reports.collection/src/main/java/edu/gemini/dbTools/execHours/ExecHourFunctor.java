package edu.gemini.dbTools.execHours;

import edu.gemini.dbTools.html.FtpProps;
import edu.gemini.dbTools.html.FtpUtil$;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharge;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spdb.cron.Storage;

import java.io.*;
import java.security.Principal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecHourFunctor extends DBAbstractQueryFunctor {

    private static final Pattern PAT = Pattern.compile("(G[SN])-(\\d{4}[AB])-(Q|C|DD|SV|LP)-(\\d+)");
    private static final float MS_PER_HOUR = 1000 * 60 * 60;

    private final Map<SPProgramID, ObsTimes> charges = new HashMap<SPProgramID, ObsTimes>();

    /** Need to pass to queryPrograms */
    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = (ISPProgram) node;
        final SPProgramID id = prog.getProgramID();
        if (id != null) {
            final Matcher m = PAT.matcher(id.stringValue());
            if (m.matches()) {
                charges.put(id, ObsTimesService.getCorrectedObsTimes(prog));
            }
        }
    }

    private void writeReport(final PrintStream out, final Site site) {

        final NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        out.println("# Exec Hour Report (" + site.abbreviation + ")");
        out.println("# Generated at " + new Date());
        out.print("# Columns are: Program ID, Elapsed");
        for (final ChargeClass cc : ChargeClass.values()) {
            out.print(", " + cc.displayValue());
        }
        out.println();

        for (final Map.Entry<SPProgramID, ObsTimes> e : charges.entrySet()) {
            final SPProgramID id = e.getKey();
            if (id.site() == site) {
                final ObsTimes times = e.getValue();
                final float elapsed = times.getTotalTime() / MS_PER_HOUR;
                final ObsTimeCharges charges = times.getTimeCharges();
                out.print(id + ", " + nf.format(elapsed));
                for (final ChargeClass cc : ChargeClass.values()) {
                    final ObsTimeCharge charge = charges.getTimeCharge(cc);
                    out.print(", " + nf.format(charge.getTime() / MS_PER_HOUR));
                }
                out.println();
            }
        }

    }

    /** Entry point. */
    // curl -i -G -d edu.gemini.dbTools.html.ftpAccount=software -d edu.gemini.dbTools.html.ftpPassword=lemon -d edu.gemini.dbTools.html.ftpHost=foobar -d edu.gemini.dbTools.html.ftpDestDir=crap  http://localhost:8442/cron/execHours
    public static void run(final Storage.Temp temp, Storage.Perm perm, final Logger log, final Map<String, String> env, Set<Principal> user) throws Exception {
        final Site site = Site.currentSiteOrNull;
        if (site != null) {

            // The receiving system is expecting the output file to have a specific name, so
            // to keep things simple we use the same name for the local file and the ftp code
            // will simply pick it up and reuse it.
            final File out;
            switch (site) {
                case GN:
                    out = temp.newFile("ExecHoursNorth.txt");
                    break;
                case GS:
                    out = temp.newFile("ExecHoursSouth.txt");
                    break;
                default:
                    throw new RuntimeException("Unknown site: " + site);
            }

            // Get our results. Nice that this is now a one-liner.
            final ExecHourFunctor func = SPDB.get().getQueryRunner(user).queryPrograms(new ExecHourFunctor());
            final Exception ex = func.getException();
            if (ex == null) {

                // Write our results with some amount of optimism about things getting cleaned up
                log.info("Writing output to temp file " + out.getAbsolutePath());
                final OutputStream os = new FileOutputStream(out);
                final PrintStream ps = new PrintStream(os);
                log.info("Found " + func.charges.size() + " items.");
                func.writeReport(ps, site);
                ps.flush();
                ps.close();
                os.flush();
                os.close();

                // And send the file to its final destination.
                log.info("FTPing...");
                final FtpProps props = new FtpProps(env);
                FtpUtil$.MODULE$.sendFile(log, out, props);

            } else {
                throw ex;
            }
        } else {
            log.warning("Cannot proceed. No current site.");
        }

    }

}
