package edu.gemini.spdb.reports.collection.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.collection.table.TimeAccountingSummaryTable.Columns;
import edu.gemini.spdb.reports.collection.util.BundleVelocityReport;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;
import edu.gemini.spdb.reports.collection.util.ReportUtils;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.SimpleSort;

public abstract class AbstractTimeAccountingSummaryReport extends BundleVelocityReport {
	
	private static final ISort[] SORTS = new ISort[] {
		new SimpleSort(Columns.DATE, ISort.Order.DESC),
		new SimpleSort(Columns.PROGRAM_ID, ISort.Order.ASC),
	};

	public void configureQuery(final IQuery q) {
		q.setOutputColumns(getOutputColumns());
		q.setGroups(getGroups());
		q.setSorts(SORTS);
	}

	protected abstract ISort[] getGroups();
	protected abstract IColumn[] getOutputColumns();
	protected abstract String getDateValue(IRow row);
	
	public List<File> execute(final IQuery query, final Map<IDBDatabaseService, List<IRow>> results, final File parentDir) throws IOException {
		final List<File> files = new ArrayList<>();
		for (final Map.Entry<IDBDatabaseService, List<IRow>> e: results.entrySet()) {

			final String abbrev = DatabaseNameManager.getInstance().getSiteAbbreviation(e.getKey());
			final String siteName = DatabaseNameManager.getInstance().getSiteName(e.getKey());
			
			// Need to break it down by calendar semester.
			final Map<String, List<IRow>> semesterRows = new TreeMap<>();
			for (final IRow row: e.getValue()) {
				final String sem = ReportUtils.semester(getDateValue(row));
				final List<IRow> rows = semesterRows.computeIfAbsent(sem, s -> new ArrayList<>());
				rows.add(row);
			}
			
			// Now write a file per semester
			for (final Entry<String, List<IRow>> entry: semesterRows.entrySet()) {

				final Map<String, Object> vc = new TreeMap<>();
				vc.put("results", entry.getValue());
				vc.put("query", query);
				vc.put("abbrev", abbrev);
				vc.put("siteName", siteName);
				vc.put("now", new Date());
				vc.put("db", e.getKey());
				vc.put("dbm", DatabaseNameManager.getInstance());
				vc.put("escaper", new HtmlEscaper());
				vc.put("semester", entry.getKey());
				
				final File out = new File(parentDir, "tas_" + abbrev + "_" + entry.getKey() + "." + getFileExtension());
				merge(out, getResourcePath(getTemplateName()), vc);
				files.add(out);
			}
		}		
		return files;
	}

	public boolean isPublic() {
		return false;
	}
	
	protected abstract String getTemplateName();
	protected abstract String getFileExtension();
	
}
