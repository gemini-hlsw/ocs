package edu.gemini.spdb.reports.collection.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.collection.table.QueueProgramStatusInternalTable.Columns;
import edu.gemini.spdb.reports.collection.util.BundleVelocityReport;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.SimpleSort;

public class QueueProgramStatusInternalReport extends BundleVelocityReport {

    private static final IColumn[] COLUMNS = new IColumn[] {
        Columns.COLOR,
        Columns.PROGRAM_ID,
        Columns.STATUS,
        Columns.COMP,
        Columns.REMAINING,
        Columns.REMAINING_BAND3,
        Columns.OBS_CONDS,
        Columns.RA_RANGE,
        Columns.INST_MODE,
        Columns.INST_RES,
        Columns.PLANNING_NOTE,
    };

    private static final ISort[] SORTS = new ISort[] {
        new SimpleSort(Columns.PROGRAM_ID),
    };

    private static final ISort[] GROUPS = new ISort[] {
        new SimpleSort(Columns.SEMESTER, ISort.Order.DESC),
        new SimpleSort(Columns.BAND),
    };

    public void configureQuery(IQuery q) {
        q.setGroups(GROUPS);
        q.setSorts(SORTS);
        q.setOutputColumns(COLUMNS);
    }

    public List<File> execute(IQuery query, Map<IDBDatabaseService, List<IRow>> results, File parentDir) throws IOException {

        // Collect semesters and their associated file, by db. Also collect
        // a list of all files we're going to write.
        Map<IDBDatabaseService, Map<String, File>> siteSemFile = new HashMap<IDBDatabaseService, Map<String, File>>();
        List<File> files = new ArrayList<File>();
        for (Entry<IDBDatabaseService, List<IRow>> e: results.entrySet()) {

            // Get the database's site abbreviation to create the internal map.
            IDBDatabaseService db = e.getKey();
            String abbrev = DatabaseNameManager.getInstance().getSiteAbbreviation(db);
            SortedMap<String, File> semFile = new TreeMap<String, File>();
            siteSemFile.put(db, semFile);

            // Now collect the semesters for this database.
            for (IRow row: e.getValue()) {
                String semester = (String) row.getGroupValues()[0];
                if (!semFile.containsKey(semester)) {
                    File file = new File(parentDir, "queueProgramStatusInternal_" + abbrev + "_" + semester + ".html");
                    semFile.put(semester, file);
                    files.add(file);
                }
            }
        }

        // Ok, now we can generate the files.
        for (Entry<IDBDatabaseService, List<IRow>> e: results.entrySet()) {
            writeReport(query, e.getKey(), e.getValue(), siteSemFile);
        }

        // Done.
        return files;
    }

    public void writeReport(IQuery query, IDBDatabaseService database, Iterable<IRow> rows, Map<IDBDatabaseService, Map<String, File>> siteSemFile) throws IOException {

        // Split the rows into N lists, one per semester. This is not
        // terribly efficient.
        Map<String, Integer> maxBands = new TreeMap<String, Integer>();
        Map<String, List<IRow>> semesterRows = new TreeMap<String, List<IRow>>();
        for (IRow row: rows) {
            String semester = (String) row.getGroupValues()[0];

            List<IRow> subset = semesterRows.get(semester);
            if (subset == null) {
                subset = new ArrayList<IRow>();
                semesterRows.put(semester, subset);
            }
            subset.add(row);

            Integer max = maxBands.get(semester);
            if (max == null) max = 0;
            maxBands.put(semester, Math.max(max, (Integer) row.getGroupValues()[1]));
        }

        // And write one page per semester.
        Map<String, File> semFile = siteSemFile.get(database);
        for (Entry<String, List<IRow>> e: semesterRows.entrySet()) {
            File file = semFile.get(e.getKey());
            Map<String, Object> vc = new TreeMap<String, Object>();
            vc.put("db", database);
            vc.put("dbm", DatabaseNameManager.getInstance());
            vc.put("query", query);
            vc.put("results", e.getValue());
            vc.put("semester", e.getKey());
            vc.put("escaper", new HtmlEscaper());
            vc.put("semesters", semesterRows.keySet());
            vc.put("now", new Date());
            vc.put("siteSemFile", siteSemFile);
            vc.put("maxBand", maxBands.get(e.getKey()));
            merge(file, getResourcePath("QueueProgramStatusInternalReport.vm"), vc);
        }

    }

    public boolean isPublic() {
        return false;
    }

}
