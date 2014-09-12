package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IFilter;
import edu.gemini.spdb.reports.collection.table.QueueProgramStatusExternalTable;

import java.util.Map;

public class LargeProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

    public LargeProgramStatusExternalReport() {
        super("LargeProgramStatusExternalReport.vm");
    }

    private static class LPFilter implements IFilter {
        @Override
        public boolean accept(Map<IColumn, ?> row) {
            SPProgramID progId = (SPProgramID) row.get(QueueProgramStatusExternalTable.Columns.PROGRAM_ID);
            return progId.stringValue().contains("-LP-");
        }
    }

    @Override
    protected IFilter getFilter() {
        return new LPFilter();
    }

    String getFileName(String site, String semester) {
        return "largeProgSchedQueue_" + site + "_" + semester + ".html";
    }
}
