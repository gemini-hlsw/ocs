package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spdb.reports.IFilter;

public final class DDProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

    public DDProgramStatusExternalReport() {
        super("ddProgramStatusExternalReport.vm");
    }

    protected IFilter getFilter() {
        return new ProgramTypeFilter(ProgramType.DirectorsTime$.MODULE$);
    }

    String getFileName(String site, String semester) {
        return "ddTime_" + site + "_" + semester + ".html";
    }

}
