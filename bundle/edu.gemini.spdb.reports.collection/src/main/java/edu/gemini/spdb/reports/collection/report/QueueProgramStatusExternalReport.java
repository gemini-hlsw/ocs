package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IFilter;

public final class QueueProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

	public QueueProgramStatusExternalReport() {
		super("QueueProgramStatusExternalReport.vm");
	}
	
	@Override
	protected IFilter getFilter() {
		return new BandFilter(1, 2, 3);
	}
	
    String getFileName(final String site, final String semester) {
        return "schedQueue_" + site + "_" + semester + ".html";
    }

}
