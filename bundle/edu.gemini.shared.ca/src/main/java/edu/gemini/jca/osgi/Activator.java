package edu.gemini.jca.osgi;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;

public class Activator implements BundleActivator, ContextExceptionListener, ContextMessageListener {

	private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());
	private Context cac;
	private ServiceRegistration reg;
	
	public void start(BundleContext context) throws Exception {
		try {
			cac = JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
			cac.addContextExceptionListener(this);
			cac.addContextMessageListener(this);
			if (context != null)
				reg = context.registerService(Context.class.getName(), cac, new Hashtable<String,String>());
		} catch (CAException cae) {
			LOGGER.log(Level.SEVERE, "Could not create JCA Context.", cac);
			throw cae;
		}
	}

	public void stop(BundleContext context) throws Exception {
		if (reg != null) reg.unregister();
		try {
			cac.destroy();
			cac.dispose();
			cac = null;
		} catch (CAException cae) {
			LOGGER.log(Level.SEVERE, "Could not destroy/dispose JCA Context.", cac);
			throw cae;
		}
	}

	public void contextException(ContextExceptionEvent cee) {
		LOGGER.warning(cee.getChannel().getName() + ": " + cee.getMessage());
	}

	public void contextMessage(ContextMessageEvent cme) {
		LOGGER.warning(cme.getMessage());
	}

	public Context getContext() {
		return cac;
	}
	
}
