package edu.gemini.spdb.reports.osgi;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import edu.gemini.spdb.reports.IReport;
import edu.gemini.spdb.reports.impl.ReportManager;

public class ReportTracker extends ServiceTracker {

	private static final Logger LOGGER = Logger.getLogger(ReportTracker.class.getName());
	
	public ReportTracker(BundleContext context) {
		super(context, IReport.class.getName(), null);
	}

	@Override
	public Object addingService(ServiceReference ref) {
		IReport report = (IReport) context.getService(ref);
		LOGGER.info("Adding report: " + ref.getProperty(IReport.SERVICE_PROP_ID));
		ReportManager.getInstance().add(
			(String) ref.getProperty(IReport.SERVICE_PROP_ID), 
			(String) ref.getProperty(IReport.SERVICE_PROP_TABLE_ID),
			report);
		return report;
	}
	
	@Override
	public void removedService(ServiceReference ref, Object service) {
		LOGGER.info("Removing report: " + ref.getProperty(IReport.SERVICE_PROP_ID));
		ReportManager.getInstance().remove((String) ref.getProperty(IReport.SERVICE_PROP_ID));
		context.ungetService(ref);
	}
	
}
