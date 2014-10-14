package edu.gemini.spdb.reports.collection.osgi;

import edu.gemini.spdb.reports.IReport;
import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.collection.report.*;
import edu.gemini.spdb.reports.collection.table.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

public final class Activator implements BundleActivator {

	public void start(final BundleContext context) throws Exception {

		// Add our tables.
		addTable(context, new TimeAccountingSummaryTable());
        addTable(context, new TemplateSummaryTable());  // REL-775
		addTable(context, new ExecHoursTable());
		addTable(context, new QueueProgramStatusInternalTable());
		addTable(context, new QueueProgramStatusExternalTable());
        addTable(context, new DDTable());

		// And our report.
		addReport(context, new TimeAccountingSummaryHtmlReport(), TimeAccountingSummaryTable.class);
		addReport(context, new TimeAccountingSummaryTextReport(), TimeAccountingSummaryTable.class);
        addReport(context, new TemplateSummaryHtmlReport(), TemplateSummaryTable.class);
      	addReport(context, new TemplateSummaryTextReport(), TemplateSummaryTable.class);
		addReport(context, new ExecHoursReport(), ExecHoursTable.class);
		addReport(context, new QueueProgramStatusExternalReport(), QueueProgramStatusExternalTable.class);
		addReport(context, new BadWeatherQueueProgramStatusExternalReport(), QueueProgramStatusExternalTable.class);
        addReport(context, new LargeProgramStatusExternalReport(), QueueProgramStatusExternalTable.class);
        addReport(context, new DDProgramStatusExternalReport(), DDTable.class);
		addReport(context, new QueueProgramStatusInternalReport(), QueueProgramStatusInternalTable.class);

	}


	public void stop(final BundleContext arg0) throws Exception {
		// nothing needed
	}

	private void addReport(final BundleContext context, final IReport report, final Class<? extends ITable> tableClass) {

		// By convention, within this bundle the reports are all identified
		// by classname since they are all stateless (as are the tables).
		// By convention, within this bundle the tables are all identified
		// by classname since they are all stateless.
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(IReport.SERVICE_PROP_ID, report.getClass().getName());
		props.put(IReport.SERVICE_PROP_TABLE_ID, tableClass.getName());
		context.registerService(IReport.class.getName(), report, props);

	}

	private void addTable(final BundleContext context, final ITable table) {

		// By convention, within this bundle the tables are all identified
		// by classname since they are all stateless.
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(ITable.SERVICE_PROP_ID, table.getClass().getName());
		context.registerService(ITable.class.getName(), table, props);

	}

}
