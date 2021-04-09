package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spdb.reports.IFilter;

public final class FTProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

    public FTProgramStatusExternalReport() {
        super(
            "ProgramStatusExternalReport.vm",
            (site, band) -> String.format("%s Fast Turnaround Time Queue Band %s", site, band)
        );
    }

    @Override
    protected IFilter getFilter() {
        return new ProgramTypeFilter(ProgramType.FastTurnaround$.MODULE$);
    }

    @Override
    String getFileName(final String site, final String semester) {
        return "ftTime_" + site + "_" + semester + ".html";
    }

}
