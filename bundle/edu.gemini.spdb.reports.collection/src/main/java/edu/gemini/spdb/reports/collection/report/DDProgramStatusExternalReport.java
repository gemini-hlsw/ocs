package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IFilter;
import edu.gemini.spdb.reports.collection.table.DDTable.Columns;
import edu.gemini.spdb.reports.collection.table.TypeCheck;

import java.util.Map;

public final class DDProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {


    protected static class DDFilter implements IFilter {
        @Override
        public boolean accept(Map<IColumn, ?> row) {
            SPProgramID progId = (SPProgramID) row.get(Columns.PROGRAM_ID);
            return TypeCheck.is(progId, ProgramType.DirectorsTime$.MODULE$);
        }
    }

    public DDProgramStatusExternalReport() {
        super("ddProgramStatusExternalReport.vm");
    }

    protected IFilter getFilter() {
        return new DDFilter();
    }

    String getFileName(String site, String semester) {
        return "ddTime_" + site + "_" + semester + ".html";
    }

}
