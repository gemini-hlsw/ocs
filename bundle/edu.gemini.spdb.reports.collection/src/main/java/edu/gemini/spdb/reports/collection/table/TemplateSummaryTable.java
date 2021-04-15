package edu.gemini.spdb.reports.collection.table;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.util.AbstractTable;

import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class TemplateSummaryTable extends AbstractTable {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(TemplateSummaryTable.class.getName());
	private static final long serialVersionUID = 1L;
	private static final float MS_PER_HOUR = 1000 * 60 * 60;
	private static final String DESC = "Templates";
	private static final String CAPTION = "Template Summary";

	public static enum Columns implements IColumn {
        // REL-775: The information that must be extracted for each target is:
        // Progid
        // Band
        // Program ToO status
        // Template id (number in [] of template node)
        // Target name
        // RA/Dec (on 1st day of semester if non-sidereal?, export as 0:0:0/0:0:0 if a ToO target type)
        // Conditions constraints
        // Instrument configuration (template/blueprint name), eg. GMOS-S LongSlit R831_G5322 OG515_G0330 Longslit 0.75 arcsec
        // Phase 1 time (total observing time)

		PROGRAM_ID("Program ID", "%s"),
		BAND("Band", "%s"),
		PROG_TOO_STATUS("Program ToO status", "%s"),
        TEMPLATE_ID("Template id", "%s"),
        TARGET_NAME("Target name", "%s"),
        RA("RA(h:m:s)", "%s"),
        DEC("DEC(d:m:s)", "%s"),
        COND_CONSTRAINTS("Conditions constraints", "%s"),
        INST_CONFIG("Instrument configuration", "%s"),
        PHASE1_TIME("Phase 1 time", "%s");

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

	}

	public TemplateSummaryTable() {
		super(Domain.PROGRAM, Columns.values(), CAPTION, DESC);
	}

	public List<Map<IColumn, Object>> getRows(Object domainObject) {
		List<Map<IColumn, Object>> rows = new ArrayList<Map<IColumn, Object>>();

        // Domain is Science Program
        final ISPProgram programShell = (ISPProgram) domainObject;

        // Find the SPProgramID. If we can't find the ID, it's not a real
        // science program and we can skip it.
        SPProgramID programID = programShell.getProgramID();
        if (programID == null) {
            LOGGER.fine("Program has no id: " + programShell);
            return Collections.emptyList();
        }
        final SPProgram progDataObj = (SPProgram) programShell.getDataObject();

        // Get the Queue band. If it's missing or invalid, log and punt.
        final String sband = progDataObj.getQueueBand();
        final int band;
        try {
            band = Integer.parseInt(sband); // doesn't throw NPE
        } catch (NumberFormatException nfe) {
            LOGGER.fine("Program " + programID + " has invalid queue band: " + sband);
            return Collections.emptyList();
        }

        TooType tooType = progDataObj.getTooType();

        // See if the program conatains a template folder.
        ISPTemplateFolder ispTemplateFolder = programShell.getTemplateFolder();
        if (ispTemplateFolder == null) {
            LOGGER.fine("No template folder in " + programID);
            return Collections.emptyList();
        }
        for(ISPTemplateGroup ispTemplateGroup : ispTemplateFolder.getTemplateGroups()) {
            TemplateGroup templateGroup = (TemplateGroup)ispTemplateGroup.getDataObject();
            String templateId = templateGroup.getVersionToken().toString();
            for(ISPTemplateParameters ispTemplateParameters : ispTemplateGroup.getTemplateParameters()) {
                String instConfig = templateGroup.getTitle().replace(",", "");

                TemplateParameters ps = (TemplateParameters)ispTemplateParameters.getDataObject();
                SPTarget target = ps.getTarget();
                SPSiteQuality.Conditions conditions = ps.getSiteQuality().conditions();
                TimeValue time = ps.getTime();

                appendRows(rows, programID, band, tooType, templateId, target, conditions, time, instConfig);
            }
        }
		return rows;

	}

	private void appendRows(List<Map<IColumn, Object>> rows, SPProgramID id, int band, TooType tooType, String templateId,
                            SPTarget target, SPSiteQuality.Conditions conditions, TimeValue time, String instConfig) {

        Map<IColumn, Object> row = createRow();
        row.put(Columns.PROGRAM_ID, id);
        row.put(Columns.BAND, band);
        row.put(Columns.PROG_TOO_STATUS, tooType.getDisplayValue());
        row.put(Columns.TEMPLATE_ID, templateId);
        row.put(Columns.TARGET_NAME, target.getName());
        row.put(Columns.RA, target.getRaString(None.instance()).getOrElse(""));
        row.put(Columns.DEC, target.getDecString(None.instance()).getOrElse(""));
        row.put(Columns.COND_CONSTRAINTS, conditions.toString().replaceAll(",", ""));
        row.put(Columns.INST_CONFIG, instConfig);
        row.put(Columns.PHASE1_TIME, String.format("%.2f hrs", time.getTimeAmount()));
        rows.add(row);
    }

	private Map<IColumn, Object> createRow() {
		return new HashMap<IColumn, Object>();
	}
}


