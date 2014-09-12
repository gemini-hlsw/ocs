package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IFilter;

public class BadWeatherQueueProgramStatusExternalReport extends
		AbstractQueueProgramStatusExternalReport {

	public BadWeatherQueueProgramStatusExternalReport() {
		super("BadWeatherQueueProgramStatusExternalReport.vm");
	}
	
	@Override
	protected IFilter getFilter() {
		return new BandFilter(4);
	}

	String getFileName(String site, String semester) {
		return "badWeatherSchedQueue_" + site + "_" + semester + ".html";
	}

}
