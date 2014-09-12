package edu.gemini.spdb.reports.collection.table;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.collection.util.ReportUtils;
import edu.gemini.spdb.reports.util.AbstractTable;

public class DDTable extends AbstractTable {

	private static final Logger LOGGER = Logger.getLogger(DDTable.class.getName());
	private static final long serialVersionUID = 1L;
	private static final float MS_PER_HOUR = 1000 * 60 * 60;
	private static final String DISPLAY_NAME = "Director's Discretionary time";
	private static final String SHORT_DESCRIPTION = "Director's Discretionary time.";

	public static enum Columns implements IColumn {

		SEMESTER("Semester", "%s"),
		BAND("Band", "%d"),
		PROGRAM_ID("Program ID", "%s"),
		PI_LAST_NAME("PI Last Name", "%s"),
		PARTNERS("Partner", "%s"), // TODO: REMOVE, NOT USED
		TITLE("Program Title", "%s"),
		INST_MODE("Inst/Mode", "%s"),
		HOURS_ALLOC("Alloc.", "%1.2f"), // TODO: use a byte
		STATUS("Status", "%s"),
		DATES("Dates Taken", "%s"),
		COMP("Comp%", "%2.0f"), // TODO: use a byte
		ROLLOVER("Rollover", "%s");

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

	public DDTable() {
		super(ITable.Domain.PROGRAM, Columns.values(), DISPLAY_NAME, SHORT_DESCRIPTION);
	}

	public List<Map<IColumn, Object>> getRows(final Object node) {
        // Domain is program. Get the id.
        final ISPProgram progShell = (ISPProgram) node;
        final SPProgramID id = progShell.getProgramID();

        // Skip anything that's not a DD program,
        if (!TypeCheck.is(id, ProgramType.DirectorsTime$.MODULE$)) return Collections.emptyList();

        // Get the semester
        String semester = ReportUtils.getSemester(id);

        // Fetch the program itself.
        final SPProgram prog = (SPProgram) progShell.getDataObject();

        // Get the Queue band. If it's missing or invalid, log and punt.
        final String sband = prog.getQueueBand();
        final int band;
        try {
            band = Integer.parseInt(sband); // doesn't throw NPE
        } catch (NumberFormatException nfe) {
            LOGGER.fine("Program " + id + " has invalid queue band: " + sband);
            return Collections.emptyList();
        }

        // Charged time.
        long charged = 0;
        for (ISPObservation obsShell: progShell.getAllObservations()) {
            charged += ReportUtils.getChargedTime(obsShell);
        }

        // Allocated time
        final TimeValue allocatedTV = prog.getAwardedTime();
        if (allocatedTV == null) {
            LOGGER.fine("Program " + id + " has null allocatedTime. Skipping.");
            return Collections.emptyList();
        }
        final long allocated = allocatedTV.getMilliseconds();

        // Completion %
        final float completion = prog.isCompleted() ? 100.0f : Math.min(100.0f, 100.0f * charged / allocated);

        // Rollover
        final String rollover = getRollover(semester, progShell);

        // Done. Build the row and return it.
        final Map<IColumn, Object> row = new HashMap<IColumn, Object>();
        row.put(Columns.SEMESTER, semester);
        row.put(Columns.BAND, band);
        row.put(Columns.PROGRAM_ID, id);
        row.put(Columns.PI_LAST_NAME, prog.getPILastName());
        row.put(Columns.PARTNERS, "");
        row.put(Columns.TITLE,  prog.getTitle());
        row.put(Columns.INST_MODE, ReportUtils.getScienceInstruments(progShell));
        row.put(Columns.HOURS_ALLOC, prog.getAwardedTime().getMilliseconds() / MS_PER_HOUR);
        row.put(Columns.STATUS, ReportUtils.getExecutionStatus(progShell, prog, false));
        row.put(Columns.DATES, getDates(progShell));
        row.put(Columns.COMP, completion);
        row.put(Columns.ROLLOVER, rollover);
        return Collections.singletonList(row);
	}

	private static String getRollover(String semester, ISPProgram prog)  {
		if (semester == null || !ReportUtils.isRollover(prog))
			return null;
		return getRollover(semester);
	}

	// 2004A => [r05A]
	private static String getRollover(String semester) {
		final int year = Integer.parseInt(semester.substring(0, 4));
		return "[r" + Integer.toString(year + 1).substring(2) + semester.charAt(4) + "]";
	}

	private Object getDates(ISPProgram progShell)  {
		SortedSet<String> set = new TreeSet<String>();
		Site site = ReportUtils.getSiteDesc(progShell.getProgramID());
		for (ISPObservation obs: progShell.getAllObservations()) {
			ObsLog log = ObsLog.getIfExists(obs);
			if (log != null) {
				for (ObsVisit visit: log.getVisits()) {
					String utc = new ObservingNight(site, visit.getStartTime()).getNightString();
					set.add(utc);
				}
			}
		}
		StringBuilder builder = new StringBuilder();
		for (String utc : set) {
			if (builder.length() > 0)
				builder.append(" / ");
			builder.append(utc);
		}
		return builder.toString();
	}
}





