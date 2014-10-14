package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spdb.reports.IFilter;

public final class FTProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

    public FTProgramStatusExternalReport() {
        super("FTProgramStatusExternalReport.vm");
    }

    protected IFilter getFilter() {
        return new ProgramTypeFilter(ProgramType.FastTurnaround$.MODULE$);
    }

    String getFileName(final String site, final String semester) {
        return "ftTime_" + site + "_" + semester + ".html";
    }

}
