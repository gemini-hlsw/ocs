//
// $Id: Driver.java 27566 2010-10-25 13:52:14Z nbarriga $
//
package edu.gemini.dbTools.semesterStatus;

import edu.gemini.dbTools.html.FtpUtil;
import edu.gemini.dbTools.html.FtpProps;
import edu.gemini.dbTools.odbState.OdbStateConfig;
import edu.gemini.dbTools.odbState.OdbStateIO;
import edu.gemini.dbTools.odbState.ProgramState;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spdb.cron.CronStorage;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Driver {

    private static Map<ProgramGroupId, List<ProgramState>> _groupPrograms(final ProgramState[] pstateA) {
        // Filter out everything but Q programs, and sort these by GN/GS and semester.
        final Map<ProgramGroupId, List<ProgramState>> progStateMap = new HashMap<ProgramGroupId, List<ProgramState>>();
        for (final ProgramState pstate : pstateA) {
            final SPProgramID pid = pstate.getProgramId();
            if (pid != null) {
                final ProgramGroupId pg;
                try {
                    pg = ProgramGroupId.parse(pid);
                    if ("Q".equals(pg.getKind())) {
                        // Add this program state object to those sharing the same location and semester.
                        List<ProgramState> progList = progStateMap.get(pg);
                        if (progList == null) {
                            progList = new ArrayList<ProgramState>();
                            progStateMap.put(pg, progList);
                        }
                        progList.add(pstate);
                    }
                } catch (ParseException e) {
                    // ok; not a science program
                }
            }
        }
        return progStateMap;
    }

    private static File _writeImage(final RenderedImage image, final ProgramGroupId pg, final File outdir, final Logger log) throws IOException {
        final String fileName = "ODBsnap" + pg.getSite() + pg.getSemester() + ".png";
        final File chartFile = new File(outdir, fileName);
        log.info("writing " + chartFile.getAbsolutePath());
        ImageIO.write(image, "png", chartFile);
        return chartFile;
    }

    public static void run(final CronStorage store, final Logger log, final Map<String, String> env, Set<Principal> user) throws IOException {

        final FtpProps props = new FtpProps(env);
        final Site loc = Site.currentSiteOrNull;

        if (loc != null) {

            // Read all the program state information and sort them into groups to be charted.
            final OdbStateConfig config  = new OdbStateConfig(store.tempDir());
            final ProgramState[] pstateA = new OdbStateIO(log, config.stateFile).readState();
            final Map<ProgramGroupId, List<ProgramState>> progStateMap = _groupPrograms(pstateA);

            // Create semester chart files for each program group.
            final List<File> chartFiles = new ArrayList<>();
            for (final Map.Entry<ProgramGroupId, List<ProgramState>> me : progStateMap.entrySet()) {

                // Get the program group and the programs that belong to it.
                final ProgramGroupId pg = me.getKey();

                // Filter out charts for other locations
                if (loc != pg.getSite()) continue;
                final List<ProgramState> pstateList = me.getValue();
                final ProgramState[] groupA = pstateList.toArray(ProgramState.EMPTY_STATE_ARRAY);

                // Create the image for the chart.
                final SemesterChart sc = new SemesterChart(pg, groupA);
                final RenderedImage image = sc.create();

                // Write the image to a file.
                try {
                    final File outfile = _writeImage(image, pg, store.tempDir(), log);
                    chartFiles.add(outfile);
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Trouble writing image for " + pg, ioe);
                }
            }

            // FTP the chart files.
            if (chartFiles.size() > 0) {
                try {
                    FtpUtil.sendFiles(log, chartFiles, props);
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }

        } else {
            log.warning("no site configured; can't continue");
        }

    }
}
