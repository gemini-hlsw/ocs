package edu.gemini.spdb.reports.collection.table;


import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimeCorrection;
import edu.gemini.spModel.util.SPTreeUtil;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;
import edu.gemini.spModel.timeacct.TimeAcctAward;
import edu.gemini.spModel.timeacct.TimeAcctCategory;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.util.AbstractTable;

public class TimeAccountingSummaryTable extends AbstractTable {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(TimeAccountingSummaryTable.class.getName());
	private static final long serialVersionUID = 1L;
	private static final float MS_PER_HOUR = 1000 * 60 * 60;
	private static final String DESC = "Nightly time charges by observation.";
	private static final String CAPTION = "Time Account Summary";

	public enum Columns implements IColumn {

		DATE("UTC Date", "%s"),
		PROGRAM_ID("Program ID", "%s"),
		INSTRUMENT("Instrument", "%s"),
		PRG("PRG", "%2.2f"),
		CAL("CAL", "%2.2f"),
		TOTAL("Total", "%2.2f"),
		ACCOUNT("Account", "%s"),
		COMMENT("Comment", "%s");

		final String caption;
		final String format;

		Columns(final String caption, final String format) {
			this.caption = caption;
			this.format = format;
		}

		public String getCaption() {
			return caption;
		}

		public String format(final Object value) {
			return String.format(Locale.getDefault(), format, value);
		}

		public Comparator getComparator() {
			return null;
		}

	}

	public TimeAccountingSummaryTable() {
		super(Domain.PROGRAM, Columns.values(), CAPTION, DESC);
	}

	public List<Map<IColumn, Object>> getRows(final Object domainObject) {
        final List<Map<IColumn, Object>> rows = new ArrayList<>();

        // Domain is Science Program
        final ISPProgram programShell = (ISPProgram) domainObject;

        // Find the SPProgramID. If we can't find the ID, it's not a real
        // science program and we can skip it.
        final SPProgramID id = programShell.getProgramID();
        if (id == null) {
            LOGGER.fine("Program has no id: " + programShell);
            return Collections.emptyList();
        }

        // Get the program type. If it's null, we can skip it because it's
        // not a science program.
        if (!TypeCheck.isScienceType(id)) return Collections.emptyList();

        // Find the SiteDesc. If we can't determine the site, we can't
        // figure out the UTC time and can't continue.
        final Site site = id.site();
        if (site == null) {
            LOGGER.fine("Cannot determine site for " + id);
            return Collections.emptyList();
        }

        // Ok. Keep going, get the time accounting categories in use.
        // If none, then we cannot report for this program.
        final SPProgram progDataObj = (SPProgram) programShell.getDataObject();
        final TimeAcctAllocation alloc = progDataObj.getTimeAcctAllocation();
        if ((alloc == null) || (alloc.getSum().getProgramAward().isZero())) {
            LOGGER.fine("No time accounting information for " + id);
            return Collections.emptyList();
        }
        final SortedMap<TimeAcctCategory, Double> ratios = alloc.getRatios(TimeAcctAward::getProgramAward);
        if (ratios.size() == 0) {
            LOGGER.fine("No time accounting category for " + id);
            return Collections.emptyList();
        }


        // Collect all visits that happened on a given night, as well as all
        // instruments in use on that night. When the loop exits, we will
        // have the information we need to aggregate the time accounting.
        for (final ISPObservation obsShell : programShell.getAllObservations()) {

            // This observation may have been executed in several visits,
            // and more than one instrument ...
            final Map<ObservingNight, Set<ObsVisit>> nightVisits = new HashMap<>();

            // Map each observing night to the set of instruments used on that night.
            final Map<ObservingNight, Set<SPComponentType>> nightInstruments = new HashMap<>();

            // Find the ObsRecord. If there is none, there is no time to
            // charge. Continue with the next observation.
            final ObsClass obsClass = ObsClassService.lookupObsClass(obsShell); // never null
            final ObsLog log = ObsLog.getIfExists(obsShell);
            if (log == null) continue;

            // Get the target
            final SPObservation obs = (SPObservation) obsShell.getDataObject();
            final String target = "[" + obsShell.getObservationNumber() + "] " + obs.getTitle();

            // Find the charge class for this observation.
            final ChargeClass defaultChargeClass = obsClass.getDefaultChargeClass();

            // Find the instrument. If there is none, I'm not sure what this
            // means. But we will keep going since it doesn't really matter.
            final ISPObsComponent instrument = SPTreeUtil.findInstrument(obsShell);

            // Collect visits and instruments per night.
            for (final ObsVisit visit : log.getVisits()) {

                // Determine night and initialize map entries if needed.
                final ObservingNight night = new ObservingNight(site, visit.getEndTime());
                final Set<ObsVisit> visits = nightVisits.computeIfAbsent(night, n -> new HashSet<>());
                final Set<SPComponentType> instruments = nightInstruments.computeIfAbsent(night, n -> new HashSet<>());

                // Collect information for this visit.
                visits.add(visit);
                if (instrument != null) instruments.add(instrument.getType());

            }

            // Ok, now we know everything that has ever happened to the current
            // science program, grouped by the night on which it happened. All
            // we need to do now is total everything up and write out some rows.
            for (final Entry<ObservingNight, Set<ObsVisit>> entry : nightVisits.entrySet()) {

                // What night are we dealing with? Instruments too.
                final ObservingNight night = entry.getKey();
                final Set<SPComponentType> instruments = nightInstruments.get(night);
                final String instrumentsString = getInstrumentsString(instruments);

                // Total up the time charges for each charge class.
                double prg = 0.0;
                double cal = 0.0;

                for (final ObsVisit visit : entry.getValue()) {

                    // Get the charges for this visit.
                    final ObsTimeCharges charges = visit.getTimeCharges(defaultChargeClass);

                    // Add to nightly totals
                    prg += charges.getTime(ChargeClass.PROGRAM) / MS_PER_HOUR;
                    cal += charges.getTime(ChargeClass.PARTNER) / MS_PER_HOUR;
                }

                appendRows(rows, id, ratios, night, instrumentsString, prg, cal, target);
            }
        }

        // Iterate the observations again, writing out rows for each time
        // correction. These are listed one by one, so we don't need to
        // do any summing up.
        for (final ISPObservation obsShell : programShell.getAllObservations()) {
            final SPObservation obs = (SPObservation) obsShell.getDataObject();
            for (final ObsTimeCorrection corr : obs.getObsTimeCorrections()) {

                final ObservingNight night = new ObservingNight(site, corr.getTimestamp());
                final ChargeClass cc = corr.getChargeClass();
                final double time = corr.getCorrection().getMilliseconds() / MS_PER_HOUR;
                final ISPObsComponent instrument = SPTreeUtil.findInstrument(obsShell);
                final String instrumentString = (instrument != null) ? instrument.getType().readableStr : null;
                final String reason = ImOption.apply(corr.getReason())
                        .filter(s -> !s.trim().isEmpty())
                        .getOrElse("<no reason given>");

                final double prg = (cc == ChargeClass.PROGRAM) ? time : 0.0;
                final double cal = (cc == ChargeClass.PARTNER) ? time : 0.0;

                appendRows(rows, id, ratios, night, instrumentString, prg, cal, "[" + obsShell.getObservationNumber() + "] time corr: " + reason);

            }
        }

        return rows;
    }

	private void appendRows(final List<Map<IColumn, Object>> rows, final SPProgramID id,
                            final Map<TimeAcctCategory, Double> ratios, final ObservingNight night,
                            final String instrumentsString, final double prg, final double cal, final String comment) {

        // Get the total.  prg + cal could cancel each other out and equal 0.0,
        // yet we'd still want a row.  So check for cal != 0 || prg != 0 below..
        final double total = prg + cal;

        // Write out a row, if any time was
        // charged. We can skip this if cal and rpt are zero.
        if (cal != 0.0 || prg != 0.0) {
            for (final Entry<TimeAcctCategory, Double> ratioEntry : ratios.entrySet()) {
                // Get accounts string
                final TimeAcctCategory cat = ratioEntry.getKey();
                final double ratio = ratioEntry.getValue();
                final String account = cat.name();

                // We can create a table row now!
                final Map<IColumn, Object> row = createRow();
                row.put(Columns.DATE, night.getNightString());
                row.put(Columns.PROGRAM_ID, id);
                row.put(Columns.INSTRUMENT, instrumentsString);
                row.put(Columns.PRG, prg * ratio);
                row.put(Columns.CAL, cal * ratio);
                row.put(Columns.TOTAL, total * ratio);
                row.put(Columns.ACCOUNT, account);
                if (comment != null) row.put(Columns.COMMENT, comment);
                rows.add(row);
            }
        }
	}

	private String getInstrumentsString(final Set<SPComponentType> types) {
		final StringBuilder builder = new StringBuilder();
		for (final SPComponentType type: types) {
			if (builder.length() > 0) builder.append(", ");
			builder.append(type.readableStr);
		}
		return builder.toString();
	}

	private Map<IColumn, Object> createRow() {
		return new HashMap<>();
	}


}


