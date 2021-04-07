package edu.gemini.spdb.reports.collection.report;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Semester;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.collection.table.TemplateSummaryTable.Columns;
import edu.gemini.spdb.reports.collection.util.BundleVelocityReport;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.SimpleSort;

public abstract class AbstractTemplateSummaryReport extends BundleVelocityReport {

	private static final ISort[] SORTS = new ISort[] {
		new SimpleSort(Columns.PROGRAM_ID, ISort.Order.ASC),
	};

	public void configureQuery(IQuery q) {
		q.setOutputColumns(getOutputColumns());
		q.setGroups(getGroups());
		q.setSorts(SORTS);
	}

	protected abstract ISort[] getGroups();
	protected abstract IColumn[] getOutputColumns();
	protected abstract Optional<Semester> getSemester(IRow row);

	public List<File> execute(IQuery query, Map<IDBDatabaseService, List<IRow>> results, File parentDir) throws IOException {
		List<File> files = new ArrayList<>();
		for (Map.Entry<IDBDatabaseService, List<IRow>> e: results.entrySet()) {

			String abbrev = DatabaseNameManager.getInstance().getSiteAbbreviation(e.getKey());
			String siteName = DatabaseNameManager.getInstance().getSiteName(e.getKey());

			// Need to break it down by calendar semester.
			final Map<Semester, List<IRow>> semesterRows = new TreeMap<>();
			for (IRow row: e.getValue()) {

				getSemester(row).ifPresent(sem -> {
					List<IRow> rows = semesterRows.get(sem);
					if (rows == null) {
						rows = new ArrayList<>();
						semesterRows.put(sem, rows);
					}
					rows.add(row);
				});
			}

			// Now write a file per semester
			for (Entry<Semester, List<IRow>> entry: semesterRows.entrySet()) {

				Map<String, Object> vc = new TreeMap<String, Object>();
				vc.put("results", entry.getValue());
				vc.put("query", query);
				vc.put("abbrev", abbrev);
				vc.put("siteName", siteName);
				vc.put("now", new Date());
				vc.put("db", e.getKey());
				vc.put("dbm", DatabaseNameManager.getInstance());
				vc.put("escaper", new HtmlEscaper());
				vc.put("semester", entry.getKey().format());

				File out = new File(parentDir, "tsr_" + abbrev + "_" + entry.getKey() + "." + getFileExtension());
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
