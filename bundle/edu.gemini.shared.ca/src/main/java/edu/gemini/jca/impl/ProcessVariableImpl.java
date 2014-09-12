package edu.gemini.jca.impl;

import edu.gemini.jca.ProcessVariable;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of IProcessVarible that maps from Java type JT to EPICS type
 * ET.
 * @author rnorris
 * @param <JT>
 * @param <ET>
 */
public abstract class ProcessVariableImpl<JT, ET>  implements ProcessVariable<JT> {


	private static final Logger LOGGER = Logger.getLogger(ProcessVariableImpl.class.getName());

	private Context ctx;
	private Channel channel;
	private final Listeners listeners = new Listeners();
	private JT value;
	private boolean pending = false;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final String name;
	private boolean closed = false;

	public ProcessVariableImpl(String fqcn) {
		name = fqcn;
	}

	public void open(Context context) throws IOException {
		ctx = context;
		try {
			init(name);
		} catch (CAException cae) {
			throw new IOException(cae.getMessage());
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.firePropertyChange(PROP_CONNECTED, null, channel != null && channel.getConnectionState() == Channel.CONNECTED);
		pcs.firePropertyChange(PROP_VALUE, null, get());
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	public String getName() {
		return name;
	}

	public void close() {
		try {
			closed = true;
			channel.destroy();
			channel.dispose();
			ctx = null;
		} catch (CAException cae) {
			LOGGER.log(Level.WARNING, "Trouble destroying/disposing channel " + channel.getName(), cae);
		}
	}

	protected abstract ET toEpics(JT o);

	protected abstract JT fromEpics(ET o);

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}

	public synchronized void put(JT value) throws IOException {

		// Make sure there's not another put pending.
		while (pending) {
			try {
				wait();
			} catch (InterruptedException e) {
                // ignore
			}
		}
		pending = true;

		try {

				// We don't have runtime access to T so we don't know what type of value
			// we're trying to put. We will turn wrapper types into single-element
			// arrays of the equivalent primitive, and just turn Strings into
			// single-element String arrays. It seems to be the case that all fields
			// ultimately are arrays for some reason.
			Object o = toEpics(value);
			if (o instanceof Byte)    { channel.put(new byte[]  { (Byte)    o }, listeners); } else
			if (o instanceof Short)   { channel.put(new short[] { (Short)   o }, listeners); } else
			if (o instanceof Integer) { channel.put(new int[]   { (Integer) o }, listeners); } else
			if (o instanceof Double)  { channel.put(new double[]{ (Double)  o }, listeners); } else
			if (o instanceof Float)   { channel.put(new float[] { (Float)   o }, listeners); } else
			if (o instanceof String)  { channel.put(new String[]{ (String)  o }, listeners); } else
				throw new IllegalArgumentException("I can only deal with Byte, Short, Integer, Double, Float, and String.");

			channel.getContext().flushIO();

		} catch (CAException cae) {
			IOException ioe = new IOException(cae.getMessage());
			ioe.initCause(cae);
			throw ioe;
		}
	}

	public JT get() {
		return value;
	}

	private void init(String fqcn) throws CAException {
		channel = ctx.createChannel(fqcn, listeners);
		ctx.flushIO();
	}

	public boolean isConnected() {
		return channel != null && channel.getConnectionState() == Channel.CONNECTED;
	}

	private class Listeners implements ConnectionListener, MonitorListener, GetListener, PutListener {


		public void connectionChanged(ConnectionEvent ce) {
			if (ce.isConnected()) {

				LOGGER.fine("Channel was opened for " + channel.getName());
				pcs.firePropertyChange(PROP_CONNECTED, false, true);
				try {
					channel.addMonitor(Monitor.VALUE, this);
					channel.get(listeners);
					channel.getContext().flushIO();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Could not add monitor to " + channel.getName(), e);
				}

			} else {

				// The channel was closed.
				JT prev = value;
				value = null;
				pcs.firePropertyChange(PROP_VALUE, prev, null);
				pcs.firePropertyChange(PROP_CONNECTED, true, false);

				// Now throw the dead channel away and reconnect. The old monitor
				// will get GC'd so we don't need to worry about it.
				if (!closed) {
					LOGGER.warning("Connection was closed: " + channel.getName());
					try {

						LOGGER.info("Reconnecting " + channel.getName());
						try {
							channel.destroy();
						} catch (IllegalStateException ise) {
							//  this is ok
						}
						init(channel.getName());

					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Trouble reconnecting channel " + channel.getName(), e);
					}
				} else {
					LOGGER.info("Closed: " + channel.getName());
				}

			 }
		}



		public void monitorChanged(MonitorEvent me) {
			Channel ch = (Channel) me.getSource();
			try {
				if (ch.getConnectionState() == Channel.CONNECTED) {
					ch.get(1, this);
					ch.getContext().flushIO();
				} else {
					// This can happen when a channel is closing. We can safely ignore this event.
					LOGGER.info("Discarding monitor change event from closed channel: " + ch.getName());
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not request value for " + ch.getName(), e);
			}
		}


		@SuppressWarnings("unchecked")
		public void getCompleted(GetEvent ge) {
			Channel ch = (Channel) ge.getSource();
			try {

				DBR dbr = ge.getDBR();
				Object o = (dbr == null) ? null : dbr.getValue();
				if (o != null && o.getClass().isArray()) {
					o = Array.get(o, 0);
				}
				JT prev = value;
				value = fromEpics((ET) o);
				pcs.firePropertyChange(PROP_VALUE, prev, value);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not get/set value for " + ch.getName(), e);
			}
		}

		public void close() throws CAException {
			closed = true;
			channel.destroy();
		}

		public void putCompleted(PutEvent pe) {
			synchronized (ProcessVariableImpl.this) {
				pending = false;
				ProcessVariableImpl.this.notifyAll();
			}
		}

	}

	public static class EnumString<E extends Enum> extends ProcessVariableImpl<E, String> {

		private final Class type;

		public EnumString(String fqcn, Class type) {
			super(fqcn);
			this.type = type;
		}

		@Override
		protected String toEpics(E o) {
			return o.name();
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E fromEpics(String o) {
			try {
				return (E) Enum.valueOf(type, o);
			} catch (IllegalArgumentException iae) {
				LOGGER.warning("Item " + o + " not found in " + Arrays.toString(type.getEnumConstants()));
				return null;
			}
		}

	}

	public static class EnumShort<E extends Enum> extends ProcessVariableImpl<E, Short> {

		private final E[] values;

		public EnumShort(String fqcn, E[] values) {
			super(fqcn);
			this.values = values;
		}

		@Override
		protected Short toEpics(E o) {
			return (short) o.ordinal();
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E fromEpics(Short o) {
			try {
				return values[o];
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				LOGGER.warning("Index " + o + " out of bounds in " + Arrays.toString(values));
				return null;
			}
		}

	}

	public static class EnumInt<E extends Enum> extends ProcessVariableImpl<E, Integer> {

		private final E[] values;

		public EnumInt(String fqcn, E[] values) {
			super(fqcn);
			this.values = values;
		}

		@Override
		protected Integer toEpics(E o) {
			return o.ordinal();
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E fromEpics(Integer o) {
			try {
				return values[o];
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				LOGGER.warning("Index " + o + " out of bounds in " + Arrays.toString(values));
				return null;
			}
		}

	}

	public static class Identity<T> extends ProcessVariableImpl<T, T> {

		public Identity(String fqcn) {
			super(fqcn);
		}

		@Override
		protected T toEpics(T o) {
			return o;
		}

		@Override
		protected T fromEpics(T o) {
			return o;
		}


	}


}


