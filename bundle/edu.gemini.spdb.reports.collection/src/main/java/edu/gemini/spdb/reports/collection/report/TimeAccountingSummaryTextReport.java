package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.collection.table.TimeAccountingSummaryTable.Columns;

public class TimeAccountingSummaryTextReport extends
		AbstractTimeAccountingSummaryReport {
	
	private static final ISort[] GROUPS = new ISort[]{};

	private static final IColumn[] COLUMNS = new IColumn[] {
		Columns.DATE,
		Columns.PROGRAM_ID,
		Columns.INSTRUMENT,
		Columns.PRG,
		Columns.CAL,
		Columns.TOTAL,
		Columns.ACCOUNT,
		Columns.COMMENT,
	};

	@Override
	protected String getTemplateName() {
		return "TimeAccountingSummaryTextReport.vm";
	}

	@Override
	protected String getFileExtension() {
		return "txt";
	}

	@Override
	protected ISort[] getGroups() {
		return GROUPS;
	}
	
	@Override
	protected IColumn[] getOutputColumns() {
		return COLUMNS;
	}

	@Override
	protected String getDateValue(final IRow row) {
		return (String) row.getValue(0);
	}

}
