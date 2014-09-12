package edu.gemini.shared.ca;

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.shared.ca.weather.WeatherBean;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Provides very simple introspection-based, field-level binding of members to EPICS channels.
 * If a PropertyChangeSupport is provided, the ChannelBindingSupport will fire property change
 * events as appropriate for the target bean. For simplicity the following assumptions are
 * made:
 * <ul>
 * <li>The channel's type must be compatible with the declared type of the member. Primitives will be
 * wrapped/unwrapped automatically. Array members must be arrays of primitives.
 * <li>The bound property's name must be identical to the member name on the target class. The same
 * name will be used for property change events.
 * <li>If a value is not available for whatever reason, the bound variable will be assigned its
 * default value (i.e., false for booleans, 0 for numbers, null for reference types). So if you
 * want to distinguish between default values and missing values, bind a wrapper type instead of
 * a primitive.
 * </ul> 
 * Property updates happen entirely asynchronously, and it is not possible to know how long it
 * will take. In general it will take only a few seconds for a bound property to receive its 
 * initial update.
 * <p>
 * The binding support class handles network disconnects and reconnects correctly (I think). So
 * you shouldn't have to worry about it.
 */
public class ChannelBindingSupport {

	private static final Logger LOGGER = Logger.getLogger(WeatherBean.class.getName());
	private static Context ctx;

	private final Object target;
	private final PropertyChangeSupport pcs;
	private final Map<Channel, Field> fields = new HashMap<Channel, Field>();

	/**
	 * The connection listener handles connect/disconnect/reconnect.
	 */
	private final ConnectionListener connectionListener = new ConnectionListener() {
		public void connectionChanged(ConnectionEvent ce) {
			Channel ch = (Channel) ce.getSource();
			if (ce.isConnected()) {
				
				// The channel was just opened. Attach a monitor so we can get 
				// value updates.
				LOGGER.info("Channel was opened for " + ch.getName());
				try {
					ch.addMonitor(Monitor.VALUE, monitorListener);
					ch.getContext().flushIO();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Could not add monitor to " + ch.getName(), e);
				}
				
			} else {
				
				// The channel was closed unexpectedly. 
				
				// First, Set the field to its default value.
				LOGGER.warning("Connection was closed for " + ch.getName());
				Field field = fields.get(ch);				
				try {
					
					// Get the old value
					Object oldValue = field.get(target);

					// New value is zero for primitives, null for other types.
					// I am very proud of this little trick.
					Object value = Array.get(Array.newInstance(field.getType(), 1), 0);
					
					// Set and fire change event
					field.set(target, value);
					if (pcs != null)
						pcs.firePropertyChange(field.getName(), oldValue, value);
					
				} catch (Exception e) {					
					LOGGER.log(Level.WARNING, "Trouble setting default value for " + ch.getName());					
				}
				
				// Now throw the dead channel away and reconnect. The old monitor
				// will get GC'd so we don't need to worry about it.
				try {
					
					LOGGER.info("Destroying channel " + ch.getName());
					fields.remove(ch);
					ch.destroy();
					
					LOGGER.info("Reconnecting channel " + ch.getName());
					ch = ch.getContext().createChannel(ch.getName(), this);
					fields.put(ch, field);
					ch.getContext().flushIO();
					
				} catch (Exception e) {
					
					LOGGER.log(Level.SEVERE, "Trouble reconnecting channel " + ch.getName(), e);
					
				}
			 }
		}
	};
	
	/**
	 * When a monitor changes, we simply request the value.
	 */
	private final MonitorListener monitorListener = new MonitorListener() {
		public void monitorChanged(MonitorEvent me) {
			Channel ch = (Channel) me.getSource();
			try {
				if (ch.getConnectionState() == Channel.CONNECTED) {
					ch.get(getListener);
					ch.getContext().flushIO();
				} else {					
					// This can happen when a channel is closing. We can safely ignore this event.
					LOGGER.warning("Discarding monitor change event from closed channel: " + ch.getName());					
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not request value for " + ch.getName(), e);
			}
		}
	};
	
	/**
	 * When the value is available, we set the value on our target object and 
	 * fire a PropertyChangeEvent.
	 */
	private final GetListener getListener = new GetListener() {
		public void getCompleted(GetEvent ge) {
			Channel ch = (Channel) ge.getSource();
			try {
				
				// Get the old value
				Field field = fields.get(ch);
				if (field != null) {
					
					Object oldValue = field.get(target);
					
					// And the new value
					Object value = ge.getDBR().getValue();
					if (!field.getType().isArray())
						value = Array.get(value, 0);
					
					// Set and fire change event
					field.set(target, value);
					if (pcs != null)
						pcs.firePropertyChange(field.getName(), oldValue, value);
					
				} else {
					
					LOGGER.warning("Discarding completed get for closed channel " + ch.getName());
					
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not get/set value for " + ch.getName(), e);
			}
		}
	};
	
	/**
	 * Constructs a new ChannelBindingSupport without property change events. Fields will
	 * be bound but there will be no property change notifications.
	 * @param target the instance to be bound
	 * @throws CAException if EPICS cannot be initialized
	 */
	public ChannelBindingSupport(Object target) throws CAException {
		this(target, null);
	}
	

	/**
	 * Constructs a new ChannelBindingSupport with property change events. The target object
	 * will generally be the same target object for the passed PropertyChangeSupport.
	 * @param target the object to be bound
	 * @param pcs a PropertyChangeSupport through which change events will be fired
	 * @throws CAException if EPICS cannot be initialized
	 */
	public ChannelBindingSupport(Object target, PropertyChangeSupport pcs) throws CAException {
		this.target = target;
		this.pcs = pcs;
		if (ctx == null) {
			ctx = JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		}
	}
	
	/**
	 * Binds a field to a channel. If the field does not exist in the target object's class,
	 * the target's superclass will be examined, and so on up the hierarchy. See comments in
	 * the class description above for the assumptions that are made here.
	 * <p>
	 * Note that client code can safely re-throw an Error on NoSuchFieldException and
	 * SecurityException because these are related to coding mistakes and should only 
	 * occur during initial coding if you do something wrong. The other exceptions should
	 * be handled by client code. 
	 * @throws NoSuchFieldException if a field with the specified name is  not found
	 * @throws SecurityException if access to the information is denied
	 * @throws CAException if EPICS is not initialized
	 * @throws IllegalStateException if EPICS is not in proper state to create channels 
	 */
	public void bindChannel(String property, String channel) throws SecurityException, NoSuchFieldException, IllegalStateException, CAException {
		Field field = null;
		for (Class c = target.getClass(); c != null; c = c.getSuperclass()) {
			try {
				field = c.getDeclaredField(property);
			} catch (NoSuchFieldException nsfe) {				
			}
		}
		if (field == null)
			throw new NoSuchFieldException(property);
		field.setAccessible(true);
		fields.put(ctx.createChannel(channel, connectionListener), field);
		ctx.flushIO();
	}
	
}


