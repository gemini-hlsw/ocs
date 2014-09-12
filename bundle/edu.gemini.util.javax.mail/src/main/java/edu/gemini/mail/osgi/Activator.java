package edu.gemini.mail.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.gemini.mail.RetryQueue;
import edu.gemini.mail.RetrySMTPTransport;

public class Activator implements BundleActivator {

	private RetryQueue queue;
	
	public void start(BundleContext context) throws Exception {
		queue = new RetryQueue(context.getDataFile("dummy").getParentFile());
		RetrySMTPTransport.setQueue(queue);
		queue.open();
	}

	public void stop(BundleContext context) throws Exception {
		RetrySMTPTransport.setQueue(null);
		queue.close();
		queue = null;
	}

	public RetryQueue getQueue() {
		return queue;
	}
	
}
