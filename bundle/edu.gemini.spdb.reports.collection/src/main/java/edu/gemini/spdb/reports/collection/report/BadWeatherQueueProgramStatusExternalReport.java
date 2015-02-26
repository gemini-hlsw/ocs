package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IFilter;

public final class BadWeatherQueueProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

	public BadWeatherQueueProgramStatusExternalReport() {
		super("BadWeatherQueueProgramStatusExternalReport.vm");
	}
	
	@Override
	protected IFilter getFilter() {
		return new BandFilter(4);
	}

	String getFileName(final String site, final String semester) {
		return "badWeatherSchedQueue_" + site + "_" + semester + ".html";
	}

}
