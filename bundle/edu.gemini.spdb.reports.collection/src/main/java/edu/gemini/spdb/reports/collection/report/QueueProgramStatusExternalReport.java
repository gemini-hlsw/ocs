package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IFilter;

public final class QueueProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

	public QueueProgramStatusExternalReport() {
		super(
			"ProgramStatusExternalReport.vm",
			(site, band) -> String.format("%s Scientific Ranking Band %s", site, band)
		);
	}

	@Override
	protected IFilter getFilter() {
		return new BandFilter(1, 2, 3);
	}

	@Override
    String getFileName(final String site, final String semester) {
        return "schedQueue_" + site + "_" + semester + ".html";
    }

}
