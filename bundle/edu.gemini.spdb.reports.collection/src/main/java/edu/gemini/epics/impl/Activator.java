package edu.gemini.epics.impl;

import edu.gemini.epics.IEpicsClient;
import gov.aps.jca.CAException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	static {
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "172.17.2.255");
		System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
	}

	private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());
	private ServiceTracker tracker = null;
	private BundleContext context = null;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		tracker = new ServiceTracker(context, IEpicsClient.class.getName(), this);
		tracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		tracker.close();
		tracker = null;
		this.context = null;
		ChannelBindingSupport.destroy();
	}

	public Object addingService(ServiceReference ref) {


		IEpicsClient client = (IEpicsClient) context.getService(ref);
		LOGGER.info("IEpicsClient added: " + client);

		try {

			ChannelBindingSupport cbs = new ChannelBindingSupport(client);
			String[] channels = (String[]) ref.getProperty(IEpicsClient.EPICS_CHANNELS);
            for (String channel : channels) {
                cbs.bindChannel(channel);
            }
			client.connected();
			return cbs;

		} catch (CAException cae) {
			LOGGER.log(Level.SEVERE, "Could not connect to EPICS.", cae);
			return null;
		}

	}

	public void modifiedService(ServiceReference ref, Object obj) {
		// TODO: rebind channels
	}

	public void removedService(ServiceReference ref, Object obj) {

		IEpicsClient client = (IEpicsClient) context.getService(ref);
		LOGGER.info("IEpicsClient removed: " + client);

		ChannelBindingSupport cbs = (ChannelBindingSupport) obj;
		try {
			cbs.close();
			client.disconnected();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Could not close channel binder.", e);
		}

	}


}
