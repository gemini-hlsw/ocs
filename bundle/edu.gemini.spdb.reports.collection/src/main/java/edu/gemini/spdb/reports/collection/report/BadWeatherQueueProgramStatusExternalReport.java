package edu.gemini.spdb.reports.collection.report;

import edu.gemini.spdb.reports.IFilter;

public final class BadWeatherQueueProgramStatusExternalReport extends AbstractQueueProgramStatusExternalReport {

	public BadWeatherQueueProgramStatusExternalReport() {
		super(
			"ProgramStatusExternalReport.vm",
			(site, band) -> String.format("%s Poor Weather Queue", site)
		);
	}

	@Override
	protected IFilter getFilter() {
		return new BandFilter(4);
	}

	@Override
	String getFileName(final String site, final String semester) {
		return "badWeatherSchedQueue_" + site + "_" + semester + ".html";
	}

}
