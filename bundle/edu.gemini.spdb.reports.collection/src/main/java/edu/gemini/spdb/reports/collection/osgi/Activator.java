package edu.gemini.spdb.reports.collection.osgi;

import java.util.Hashtable;

import edu.gemini.spdb.reports.collection.report.*;
import edu.gemini.spdb.reports.collection.table.DDTable;
import edu.gemini.spdb.reports.collection.table.TemplateSummaryTable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.gemini.spdb.reports.IReport;
import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.collection.table.ExecHoursTable;
import edu.gemini.spdb.reports.collection.table.QueueProgramStatusExternalTable;
import edu.gemini.spdb.reports.collection.table.QueueProgramStatusInternalTable;
import edu.gemini.spdb.reports.collection.table.TimeAccountingSummaryTable;
import edu.gemini.spdb.reports.collection.util.DatabaseNameManager;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {

//		// Hack: this will go away
//		DatabaseNameManager.getInstance().setSiteMap(context.getProperty("edu.gemini.spdb.reports.siteMap"));

		// Add our tables.
// SCT-286 (2): they are no longer interested in TAD
//		addTable(context, new TimeAccountingDetailsTable());
		addTable(context, new TimeAccountingSummaryTable());
        addTable(context, new TemplateSummaryTable());  // REL-775
		addTable(context, new ExecHoursTable());
		addTable(context, new QueueProgramStatusInternalTable());
		addTable(context, new QueueProgramStatusExternalTable());
        addTable(context, new DDTable());
//		addTable(context, new LGSTargetTable());

		// And our report.
// SCT-286 (2): they are no longer interested in TAD report
//		addReport(context, new TimeAccountingDetailsReport(), TimeAccountingDetailsTable.class);
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


	public void stop(BundleContext arg0) throws Exception {
		// nothing needed
	}

	private void addReport(BundleContext context, IReport report, Class<? extends ITable> tableClass) {

		// By convention, within this bundle the reports are all identified
		// by classname since they are all stateless (as are the tables).
		// By convention, within this bundle the tables are all identified
		// by classname since they are all stateless.
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(IReport.SERVICE_PROP_ID, report.getClass().getName());
		props.put(IReport.SERVICE_PROP_TABLE_ID, tableClass.getName());
		context.registerService(IReport.class.getName(), report, props);

	}

	private void addTable(BundleContext context, ITable table) {

		// By convention, within this bundle the tables are all identified
		// by classname since they are all stateless.
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(ITable.SERVICE_PROP_ID, table.getClass().getName());
		context.registerService(ITable.class.getName(), table, props);

	}

}
