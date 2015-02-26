package edu.gemini.spdb.reports.osgi;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.impl.TableManager;

public class TableTracker extends ServiceTracker {

	private static final Logger LOGGER = Logger.getLogger(TableTracker.class.getName());
	
	public TableTracker(BundleContext context) {
		super(context, ITable.class.getName(), null);
	}

	@Override
	public Object addingService(ServiceReference ref) {
		
		final String id = (String) ref.getProperty(ITable.SERVICE_PROP_ID);
		if (id == null) {
			LOGGER.warning("Ignoring table with no ID.");
			return null;
		}

		ITable table = (ITable) context.getService(ref);
		LOGGER.info("Adding table: " + table.getClass().getName());
		
		TableManager.getInstance().put(id, table);
		return table;
	}
	
	@Override
	public void removedService(ServiceReference ref, Object service) {

		ITable table = (ITable) service;		
		LOGGER.info("Removing table: " + table.getClass().getName());
		final String id = (String) ref.getProperty(ITable.SERVICE_PROP_ID);
		TableManager.getInstance().remove(id);
		context.ungetService(ref);
	}
	
}
