package edu.gemini.spdb.reports.osgi;

import java.io.File;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import edu.gemini.spdb.reports.impl.www.DispatchServlet;

public class HttpTracker extends ServiceTracker {

	private static final Logger LOGGER = Logger.getLogger(HttpTracker.class.getName());

    private final IDBDatabaseService db;
	private final File batchRoot;
    private final Set<Principal> user;

	public HttpTracker(BundleContext context, File batchRoot, IDBDatabaseService db, Set<Principal> user) {
		super(context, HttpService.class.getName(), null);
		this.batchRoot = batchRoot;
        this.db = db;
        this.user = user;
	}

	@Override
	public Object addingService(ServiceReference ref) {
		HttpService http = (HttpService) context.getService(ref);
		
		// Set up the web app.
		try {			
			http.registerServlet("/reports", new DispatchServlet(db, user), new Hashtable(), null);
		} catch (ServletException se) {
			LOGGER.log(Level.SEVERE, "Trouble setting up web application.", se);
		} catch (NamespaceException ne) {
			LOGGER.log(Level.SEVERE, "Trouble setting up web application.", ne);
		}		
		
		// Set up the batch directory.		
		try {			
			http.registerResources("/batch", "", new ReportContext(batchRoot.toURL()));			
		} catch (MalformedURLException mue) {
			LOGGER.log(Level.SEVERE, "Trouble setting up batch resource mount.", mue);
		} catch (NamespaceException ne) {
			LOGGER.log(Level.SEVERE, "Trouble setting up batch resource mount.", ne);
		}
		return http;
	}
	
	@Override
	public void removedService(ServiceReference ref, Object service) {
		HttpService http = (HttpService) service;		
		http.unregister("/reports");		
		http.unregister("/batch");		
		context.ungetService(ref);
	}
	
}
