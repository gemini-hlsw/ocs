package edu.gemini.spdb.reports.osgi;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import edu.gemini.spdb.cron.CronJob;
import edu.gemini.util.osgi.ExternalStorage$;
import edu.gemini.util.security.principal.StaffPrincipal;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import edu.gemini.spdb.reports.impl.BatchReportsTask;

public class Activator implements BundleActivator {

	private ServiceTracker tableTracker;
	private ServiceTracker reportTracker;	
	private ServiceTracker databaseTracker;	

    // We run as the superuser
    private final Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

	public void start(BundleContext context) throws Exception {

		final File root = ExternalStorage$.MODULE$.getExternalDataFile(context, "batch");
		if (!root.exists() && !root.mkdir()) throw new IOException("Couldn't create dir: " + root.getPath());
		if (!root.isDirectory()) throw new IOException("Not a directory: " + root.getPath());

        final BatchReportsTask task = new BatchReportsTask(root);
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(CronJob.ALIAS, "reports");
        context.registerService(CronJob.class, task, props);

		tableTracker = new TableTracker(context);
		tableTracker.open();
		
		reportTracker = new ReportTracker(context);
		reportTracker.open();
		
		databaseTracker = new DatabaseTracker(context, root, user);
		databaseTracker.open();
	
	}

	public void stop(BundleContext context) throws Exception {
		
		tableTracker.close();
		tableTracker = null;

		reportTracker.close();
		reportTracker = null;

		databaseTracker.close();
		databaseTracker = null;
		
	}

}
