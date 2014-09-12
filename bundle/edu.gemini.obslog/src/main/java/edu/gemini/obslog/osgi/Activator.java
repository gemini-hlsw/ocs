package edu.gemini.obslog.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private DatabaseTracker dbTracker;

	public void start(BundleContext context) throws Exception {

        dbTracker = new DatabaseTracker(context);
        dbTracker.open();

        System.out.println("edu.gemini.obslog started.");
	}

	public void stop(BundleContext context) throws Exception {

        dbTracker.close();
        dbTracker = null;

		System.out.println("edu.gemini.obslog stopped.");
	}
	
}
