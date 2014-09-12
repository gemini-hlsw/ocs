package edu.gemini.epics.impl;

import edu.gemini.epics.IEpicsClient;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelBindingSupport {

	private static final Logger LOGGER = Logger.getLogger(ChannelBindingSupport.class.getName());
	private static Context ctx;

	private final IEpicsClient target;
	private final Set<Channel> channels = new HashSet<Channel>();
	private boolean closed;

	private final ConnectionListener connectionListener = new ConnectionListener() {
		public void connectionChanged(ConnectionEvent ce) {
			Channel ch = (Channel) ce.getSource();
			if (ce.isConnected()) {
				
				LOGGER.fine("Channel was opened for " + ch.getName());
				try {
					ch.addMonitor(Monitor.VALUE, monitorListener);
					ch.getContext().flushIO();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Could not add monitor to " + ch.getName(), e);
				}
				
			} else {
				
				// The channel was closed. 				
				// First, update the value to null.
				target.channelChanged(ch.getName(), null);
				
				// Now throw the dead channel away and reconnect. The old monitor
				// will get GC'd so we don't need to worry about it.
				if (!closed) {
					LOGGER.warning("Connection was closed for " + ch.getName());
					try {
						
						LOGGER.info("Destroying channel " + ch.getName() + ", state is " + ch.getConnectionState());					
						ch.destroy();
						
						LOGGER.info("Reconnecting channel " + ch.getName());
						ch = ch.getContext().createChannel(ch.getName(), this);
						ch.getContext().flushIO();
						
					} catch (Exception e) {					
						LOGGER.log(Level.SEVERE, "Trouble reconnecting channel " + ch.getName(), e);					
					}
				}
			 }
		}
	};
	
	private final MonitorListener monitorListener = new MonitorListener() {
		public void monitorChanged(MonitorEvent me) {
			Channel ch = (Channel) me.getSource();
			try {
				if (ch.getConnectionState() == Channel.CONNECTED) {
					ch.get(getListener);
					ch.getContext().flushIO();
				} else {					
					// This can happen when a channel is closing. We can safely ignore this event.
					LOGGER.info("Discarding monitor change event from closed channel: " + ch.getName());				
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not request value for " + ch.getName(), e);
			}
		}
	};
	

	private final GetListener getListener = new GetListener() {
		public void getCompleted(GetEvent ge) {
			Channel ch = (Channel) ge.getSource();
			try {
				
				// Get the new value
				Object value = ge.getDBR().getValue();
				if (value.getClass().isArray())
					value = Array.get(value, 0);

				target.channelChanged(ch.getName(), value);
				
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not get/set value for " + ch.getName(), e);
			}
		}
	};
	

	public ChannelBindingSupport(IEpicsClient target) throws CAException {
		this.target = target;
		if (ctx == null) {
			ctx = JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
			ctx.addContextExceptionListener(new ContextExceptionListener() {			
				public void contextException(ContextExceptionEvent cee) {
					LOGGER.log(Level.WARNING, "Trouble in JCA Context.", cee);
				}			
			});
			ctx.addContextMessageListener(new ContextMessageListener() {			
				public void contextMessage(ContextMessageEvent cme) {
					LOGGER.info(cme.getMessage());
				}			
			});
		}
	}
	
	public void bindChannel(String channel) throws CAException {
		channels.add(ctx.createChannel(channel, connectionListener));
		ctx.flushIO();
	}
	
	public void close() throws IllegalStateException, CAException {
		closed = true;
		for (Iterator<Channel> it = channels.iterator(); it.hasNext(); ) {
			Channel ch = it.next();
			try {
				ch.destroy();
			} catch (IllegalStateException ise) {
				// Ok; channel already destroyed.
			}
			it.remove();
		}
		LOGGER.info("Closed channel binder. " + ctx.getChannels().length + " channel(s) remaining in context.");
	}
	
	public static void destroy() {		
		try {
			if (ctx != null) {
				ctx.destroy();
				LOGGER.info("Destroyed JCA context.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Could not destroy JCA context.", e);
		}
		ctx = null;
	}
	
}


