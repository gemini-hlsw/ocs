package jsky.app.ot.shared.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println("jsky.app.ot.shared started.");
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("jsky.app.ot.shared stopped.");
	}
	
}
