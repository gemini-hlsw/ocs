package edu.gemini.spdb.reports.collection.table;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.util.AbstractTable;

@SuppressWarnings("unchecked")
public class ExecHoursTable extends AbstractTable {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(ExecHoursTable.class.getName());
	private static final long serialVersionUID = 1L;
	private static final float MS_PER_HOUR = 1000 * 60 * 60;
	private static final String SHORT_DESCRIPTION = "Elapsed time per science program and charge class.";
	private static final String DISPLAY_NAME = "Executed Program Hours";

	public static enum Columns implements IColumn {

		PROGRAM_ID("Program ID", "%s"),
		ALLOCATED_HRS("Allocated", "%2.2f"),
		ELAPSED_HRS("Elapsed", "%2.2f"),
		NON_CHARGED_HRS("Non-charged", "%2.2f"),
		PARTNER_HRS("Partner", "%2.2f"),
		PROGRAM_HRS("Program", "%2.2f"),
		PI_AFFILIATE("PI Affiliate", "%s"),
		PI_FIRST_NAME("PI First Name", "%s"),
		PI_LAST_NAME("PI Last Name", "%s"),
		PI_EMAIL("PI Email", "%s");

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

	public ExecHoursTable() {
		super(Domain.PROGRAM, Columns.values(), DISPLAY_NAME, SHORT_DESCRIPTION);
	}

	public List<Map<IColumn, Object>> getRows(Object node) {
//		try {

			ISPProgram prog = (ISPProgram) node;
			SPProgram program = (SPProgram) prog.getDataObject();

			// Skip irrelevant programs.
            if (!TypeCheck.isScienceType(prog.getProgramID())) return Collections.emptyList();

			ObsTimes times = ObsTimesService.getCorrectedObsTimes(prog);
			ObsTimeCharges charges = times.getTimeCharges();

			Map<IColumn, Object> row = new HashMap<IColumn, Object>();

			row.put(Columns.PROGRAM_ID, prog.getProgramID());
			try {
				row.put(Columns.ALLOCATED_HRS, program.getAwardedProgramTime().getMilliseconds() / MS_PER_HOUR);
			} catch (NullPointerException npe) {
				LOGGER.warning("Program " + prog.getProgramID() + " has null awarded time.");
				row.put(Columns.ALLOCATED_HRS, 0f);
			}
			row.put(Columns.ELAPSED_HRS, times.getTotalTime() / MS_PER_HOUR);
			row.put(Columns.NON_CHARGED_HRS, charges.getTime(ChargeClass.NONCHARGED) / MS_PER_HOUR);
			row.put(Columns.PARTNER_HRS, charges.getTime(ChargeClass.PARTNER) / MS_PER_HOUR);
			row.put(Columns.PROGRAM_HRS, charges.getTime(ChargeClass.PROGRAM) / MS_PER_HOUR);

			SPProgram sp = (SPProgram) prog.getDataObject();
			row.put(Columns.PI_AFFILIATE, sp.getPIInfo().getAffiliate());
			row.put(Columns.PI_FIRST_NAME, sp.getPIInfo().getFirstName());
			row.put(Columns.PI_LAST_NAME, sp.getPIInfo().getLastName());
			row.put(Columns.PI_EMAIL, sp.getPIInfo().getEmail());

			return Collections.singletonList(row);

//		} catch (RemoteException re) {
//			throw new RuntimeException(re);
//		}
	}

}
