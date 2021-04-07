package edu.gemini.spdb.reports.collection.report;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Semester;
import edu.gemini.spdb.reports.*;
import edu.gemini.spdb.reports.collection.table.QueueProgramStatusExternalTable.Columns;
import edu.gemini.spdb.reports.collection.table.TypeCheck;
import edu.gemini.spdb.reports.collection.util.BundleVelocityReport;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.SimpleSort;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractQueueProgramStatusExternalReport extends BundleVelocityReport {

    private static final IColumn[] COLUMNS = new IColumn[]{
            Columns.PROGRAM_ID,
            Columns.PI_LAST_NAME,
            Columns.PARTNERS,
            Columns.TITLE,
            Columns.INST_MODE,
            Columns.HOURS_ALLOC,
            Columns.STATUS,
            Columns.DATES,
            Columns.COMP,
            Columns.ROLLOVER,
    };

    private static final ISort[] SORTS = new ISort[]{
            new SimpleSort(Columns.PROGRAM_ID),
    };

    private static final ISort[] GROUPS = new ISort[]{
            new SimpleSort(Columns.SEMESTER, ISort.Order.DESC),
            new SimpleSort(Columns.BAND),
    };

    public void configureQuery(final IQuery q) {
        q.setGroups(GROUPS);
        q.setSorts(SORTS);
        q.setOutputColumns(COLUMNS);
        q.setFilter(new CompoundFilter(new LaterThan2005AFilter(), getFilter()));
    }

    private final String templateName;

    // Subclasses override to return a custom filter
    protected abstract IFilter getFilter();

    public AbstractQueueProgramStatusExternalReport(final String templateName) {
        this.templateName = templateName;
    }

    public List<File> execute(final IQuery query, final Map<IDBDatabaseService, List<IRow>> results, final File parentDir) throws IOException {

        // Collect, for each semester, a list of SemesterInfos. There will normally just be
        // two SemesterInfos per semester, but during testing there may be fewer or more.
        final List<File> ret = new ArrayList<>();
        final Map<String, List<SemesterInfo>> masterMap = new TreeMap<>();

        for (Entry<IDBDatabaseService, List<IRow>> e : results.entrySet()) {

            // Get the database's site abbreviation to create the internal map.
            final IDBDatabaseService db = e.getKey();

            // Now collect the semesters for this database.
            final Map<String, SemesterInfo> semInfo = new TreeMap<>();
            for (IRow row : e.getValue()) {
                final String semester = (String) row.getGroupValues()[0];
                SemesterInfo info = semInfo.get(semester);
                if (info == null) {
                    info = new SemesterInfo(semester, db);
                    semInfo.put(semester, info);
                }
                info.add(row);
                info.addBand((Integer) row.getGroupValues()[1]);
            }

            // And append them to the master map.
            for (Entry<String, SemesterInfo> entry : semInfo.entrySet()) {
                List<SemesterInfo> list = masterMap.get(entry.getKey());
                if (list == null) {
                    list = new ArrayList<>();
                    masterMap.put(entry.getKey(), list);
                }
                list.add(entry.getValue());
            }


            // And write one page per site/semester.
            for (Entry<String, List<SemesterInfo>> e2 : masterMap.entrySet()) {

                final List<SemesterInfo> infoList = e2.getValue();
                if (!infoList.isEmpty()) { // shouldn't happen, really

                    final String site = infoList.get(0).getSiteAbbrev();
                    final String semester = e2.getKey();
                    final File file = new File(parentDir, getFileName(site, semester));
                    ret.add(file);

                    final Map<String, Object> vc = new TreeMap<>();
                    vc.put("infoList", infoList);
                    vc.put("sites", site);
                    vc.put("query", query);
                    vc.put("semester", semester);
                    vc.put("escaper", new HtmlEscaper());
                    vc.put("now", new Date());

                    merge(file, getResourcePath(templateName), vc);

                }

            }

        }

        // Done.
        return ret;
    }

    public boolean isPublic() {
        return true;
    }

    public final class SemesterInfo extends ArrayList<IRow> {

        private static final long serialVersionUID = 1L;

        private final String semester;
        private final IDBDatabaseService db;
        private int maxBand;

        public SemesterInfo(String semester, IDBDatabaseService db) {
            this.db = db;
            this.semester = semester;
        }

        public String getSite() {
            return DatabaseNameManager.getInstance().getSiteName(db);
        }

        public String getSiteAbbrev() {
            return DatabaseNameManager.getInstance().getSiteAbbreviation(db);
        }

        public String getSemester() {
            return semester;
        }

        public String getSemester(int i) {
            // Here we take advantage of integer division, which actually
            // does what want in this case. Works for all values of i.
            int year = Integer.parseInt(semester.substring(0, 4));
            if (!semester.endsWith("A")) ++i;
            return (year * 2 + i) / 2 + (i % 2 == 0 ? "A" : "B");
        }

        public IDBDatabaseService getDb() {
            return db;
        }

        // Privateish stuff

        void addBand(int band) {
            maxBand = Math.max(maxBand, band);
        }


        // ==== THESE METHODS ARE NOT REFERENCED IN JAVA CODE BUT USED IN VELOCITY TEMPLATE

        public String getHash() {
            return "#"; // don't ask. velocity can go fuck itself
        }

        public String getFileName(final int i) {
            return AbstractQueueProgramStatusExternalReport.this.getFileName(getSiteAbbrev(), getSemester(i));
        }

        public String getSemesterAbbrev(final int i) {
            return getSemester(i);
        }

        public int getMaxBand() {
            return maxBand;
        }


    }

    abstract String getFileName(String site, String semester);

    static final class LaterThan2005AFilter implements IFilter {
        private static final long serialVersionUID = 1L;

        public boolean accept(Map<IColumn, ?> row) {

            String semester = (String) row.get(Columns.SEMESTER);
            int year = Integer.parseInt(semester.substring(0, 4));
            return (year > 2005 || (year == 2005 && semester.endsWith("B")));

        }
    }

    static final class BandFilter implements IFilter {
        private static final long serialVersionUID = 1L;
        private final int[] bands;

        public BandFilter(final int... bands) {
            this.bands = bands;
        }

        public boolean accept(final Map<IColumn, ?> row) {

            final Integer band = (Integer) row.get(Columns.BAND);
            for (int b : bands)
                if (b == band) return true;
            return false;

        }

    }


    static final class ProgramTypeFilter implements IFilter {
        private static final long serialVersionUID = 1L;
        private final Set<ProgramType> types;

        public ProgramTypeFilter(final ProgramType type) {
            this.types = new HashSet<ProgramType>() {{ add(type); }};
        }

        public ProgramTypeFilter(final ProgramType... types) {
            this.types = new HashSet<>(Arrays.asList(types));
        }

        public boolean accept(Map<IColumn, ?> row) {
            final SPProgramID pid = (SPProgramID) row.get(Columns.PROGRAM_ID);
            return TypeCheck.isAnyOf(pid, types);
        }

    }
}




