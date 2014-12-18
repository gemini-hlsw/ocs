package edu.gemini.itc.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        System.out.println("edu.gemini.itc started.");
    }

    public void stop(BundleContext context) throws Exception {
        System.out.println("edu.gemini.itc stopped.");
    }
	
}
