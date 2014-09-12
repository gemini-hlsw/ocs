package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.collection.table.TimeAccountingSummaryTable.Columns;
import edu.gemini.spdb.reports.util.SimpleSort;

public class TimeAccountingSummaryHtmlReport extends
		AbstractTimeAccountingSummaryReport {

	private static final ISort[] GROUPS = new ISort[] {
		new SimpleSort(Columns.DATE, ISort.Order.DESC),
	};

	private static final IColumn[] COLUMNS = new IColumn[] {
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
		return "TimeAccountingSummaryHtmlReport.vm";
	}

	@Override
	protected String getFileExtension() {
		return "html";
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
	protected String getDateValue(IRow row) {
		return (String) row.getGroupValue(0);
	}

}
