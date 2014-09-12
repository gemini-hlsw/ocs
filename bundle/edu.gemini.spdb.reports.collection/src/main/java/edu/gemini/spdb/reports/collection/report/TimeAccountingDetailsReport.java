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
import edu.gemini.spdb.reports.collection.table.TimeAccountingDetailsTable.Columns;
import edu.gemini.spdb.reports.collection.util.BundleVelocityReport;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;
import edu.gemini.spdb.reports.collection.util.ReportUtils;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.SimpleSort;

public class TimeAccountingDetailsReport extends BundleVelocityReport {
	
	private static final IColumn[] COLUMNS = new IColumn[] {
		Columns.DATE,
		Columns.PROGRAM_ID,
		Columns.INSTRUMENT,
		Columns.PRG,
		Columns.CAL,
		Columns.ACCOUNT,
		Columns.COMMENT,
	};

	private static final ISort[] SORTS = new ISort[] {
		new SimpleSort(Columns.DATE, ISort.Order.DESC),
	};

	public void configureQuery(IQuery q) {		
		q.setOutputColumns(COLUMNS);
		q.setSorts(SORTS);
	}

	public List<File> execute(IQuery query, Map<IDBDatabaseService, List<IRow>> results, File parentDir) throws IOException {
		List<File> files = new ArrayList<File>();
		for (Map.Entry<IDBDatabaseService, List<IRow>> e: results.entrySet()) {

			String abbrev = DatabaseNameManager.getInstance().getSiteAbbreviation(e.getKey());
			String siteName = DatabaseNameManager.getInstance().getSiteName(e.getKey());
			
			// Need to break it down by calendar semester.
			Map<String, List<IRow>> semesterRows = new TreeMap<String, List<IRow>>();
			for (IRow row: e.getValue()) {
				String sem = ReportUtils.semester((String) row.getValue(0)); // ugh
				List<IRow> rows = semesterRows.get(sem);
				if (rows == null) {
					rows = new ArrayList<IRow>();
					semesterRows.put(sem, rows);
				}
				rows.add(row);
			}
			
			// Now write a file per semester
			for (Entry<String, List<IRow>> entry: semesterRows.entrySet()) {
			
				Map<String, Object> vc = new TreeMap<String, Object>();
				vc.put("results", entry.getValue());
				vc.put("query", query);
				vc.put("abbrev", abbrev);
				vc.put("siteName", siteName);
				vc.put("now", new Date());
				vc.put("escaper", new HtmlEscaper());
				vc.put("semester", entry.getKey());

				
				File out = new File(parentDir, "tad_" + abbrev + "_" +  entry.getKey() + ".txt");
				merge(out, getResourcePath("TimeAccountingDetailsReport.vm"), vc);
				files.add(out);			

			}
		}
		return files;
	}


	public boolean isPublic() {
		return false;
	}
	
}
