package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spdb.reports.IFilter;

public final class DDProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

    public DDProgramStatusExternalReport() {
        super("DDProgramStatusExternalReport.vm");
    }

    protected IFilter getFilter() {
        return new ProgramTypeFilter(ProgramType.DirectorsTime$.MODULE$);
    }

    String getFileName(final String site, final String semester) {
        return "ddTime_" + site + "_" + semester + ".html";
    }

}
