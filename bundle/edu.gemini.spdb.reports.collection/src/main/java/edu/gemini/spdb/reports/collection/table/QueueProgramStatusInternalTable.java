package edu.gemini.spdb.reports.collection.table;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.collection.util.ReportUtils;
import edu.gemini.spdb.reports.collection.util.SiteQualityUtil;
import edu.gemini.spdb.reports.util.AbstractTable;


import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class QueueProgramStatusInternalTable extends AbstractTable {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(QueueProgramStatusInternalTable.class.getName());
    private static final long serialVersionUID = 1L;
    private static final float MS_PER_HOUR = 1000 * 60 * 60;
    private static final Pattern PAT_DO_NOT_START = Pattern.compile("do not start", Pattern.CASE_INSENSITIVE);
    private static final String DISPLAY_NAME = "Queue Program Status (Internal)";
    private static final String SHORT_DESCRIPTION = "Program status from planning point of view; used for internal report.";

    public static enum Columns implements IColumn {

        SEMESTER("Semester", "%s"),
        BAND("Band", "%d"),
        PROGRAM_ID("Program ID", "%s"),
        STATUS("Status", "%s"),
        COMP("Comp%", "%2.0f"), // TODO: use a byte
        REMAINING("Remaining", "%2.2f"),
        REMAINING_BAND3("Rem. Band 3", "%2.2f"),
        OBS_CONDS("Obs. Conds", "%s"),
        RA_RANGE("RA", "%s"),
        INST_MODE("Inst/Mode", "%s"),
        INST_RES("Inst Res.", "%s"),
        PLANNING_NOTE("Planning Note", "%s"),
        COLOR("Color", "%s");

        final String caption;
        final String format;

        Columns(String caption, String format) {
            this.caption = caption;
            this.format = format;
        }

        public String getCaption() {
            return caption;
        }

        public String format(Object value) {
            return String.format(Locale.getDefault(), format, value);
        }

        public Comparator getComparator() {
            return null;
        }

    }

    public QueueProgramStatusInternalTable() {
        super(Domain.PROGRAM, Columns.values(), DISPLAY_NAME, SHORT_DESCRIPTION);
    }

	public List<Map<IColumn, Object>> getRows(final Object node) {
//		try {

            // Domain is program. Get the id.
            final ISPProgram progShell = (ISPProgram) node;
            final SPProgramID id = progShell.getProgramID();

            // Skip anything that's not a science program
            if (!TypeCheck.isScienceType(id))
                return Collections.emptyList();

            // Get the semester
            String semester = ReportUtils.getSemester(id);

            // Fetch the program itself.
            final SPProgram prog = (SPProgram) progShell.getDataObject();

            // Get the Queue band. If it's missing or invalid, log and punt.
            final String sband = prog.getQueueBand();
            int band;
            try {
                band = Integer.parseInt(sband); // doesn't throw NPE
            } catch (NumberFormatException nfe) {
                LOGGER.fine("Program " + id + " has invalid queue band: " + sband);
                return Collections.emptyList();
//				band = 0;
            }

            // Execution status
            final String status = ReportUtils.getExecutionStatus(progShell, prog, true);

            // Charged time.
            long charged = 0;
            for (ISPObservation obsShell: progShell.getAllObservations()) {
                charged += ReportUtils.getChargedTime(obsShell);
            }

            // Allocated time
            TimeValue allocatedTV = prog.getAwardedProgramTime();
            if (allocatedTV == null) {
                LOGGER.fine("Program " + id + " has null allocatedTime. Skipping.");
                return  Collections.emptyList();
            }
            long allocated = allocatedTV.getMilliseconds();

            // Completion %
            final float completion = prog.isCompleted() ? 100.0f : Math.min(100.0f, 100.0f * charged / allocated);

            // Remaining
            final float remaining = (allocated - charged) / MS_PER_HOUR;

            float remainingBand3;
            if (band == 3) {
                long minBand3 = ReportUtils.getBand3Minimum(progShell);
                remainingBand3 = (minBand3 - charged) / MS_PER_HOUR;
            } else {
                remainingBand3 = (float) 0.0;
            }

            // Dominating observing conditions
            String obsConds = SiteQualityUtil.getDominatingConditions(progShell);

            // RA range
            String raRange = ReportUtils.getRaRange(progShell);

            // Instruments
            String instruments = ReportUtils.getScienceInstruments(progShell);

            String gmos = ReportUtils.getInstResources(progShell);

            // Planning note
            String planningNote = ReportUtils.getPlanningNoteTitle(progShell);

            // Are all obs in phase 2?
            boolean allPhase2 = false;
            for (ISPObservation obsShell: progShell.getAllObservations()) {
                SPObservation obs = (SPObservation) obsShell.getDataObject();
                if (obs.getPhase2Status() == ObsPhase2Status.PI_TO_COMPLETE) {
                    allPhase2 = true;
                } else {
                    allPhase2 = false;
                    break;
                }
            }

            // Color
            final String color;
            if (completion >= 100.0 || prog.isCompleted()) {
                color = "yellow";
            } else if (allPhase2 || (planningNote != null && PAT_DO_NOT_START.matcher(planningNote).matches())) {
                color = "blue";
            } else {
                color = prog.isActive() ? "green" : "red";
            }

            // Done. Build the row and return it.
            final Map<IColumn, Object> row = new HashMap<IColumn, Object>();
            row.put(Columns.SEMESTER, semester);
            row.put(Columns.BAND, band);
            row.put(Columns.PROGRAM_ID, id);
            row.put(Columns.STATUS, status);
            row.put(Columns.COMP, completion);
            row.put(Columns.REMAINING, remaining);
            row.put(Columns.REMAINING_BAND3, remainingBand3);
            row.put(Columns.OBS_CONDS, obsConds);
            row.put(Columns.RA_RANGE, raRange);
            row.put(Columns.INST_MODE, instruments);
            row.put(Columns.INST_RES, gmos);
            row.put(Columns.PLANNING_NOTE, planningNote);
            row.put(Columns.COLOR, color);
            return Collections.singletonList(row);

//		} catch (RemoteException re) {
//			throw new RuntimeException(re);
//		}
	}

}





