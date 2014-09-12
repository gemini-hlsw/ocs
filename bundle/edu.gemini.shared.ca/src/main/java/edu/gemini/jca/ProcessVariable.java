package edu.gemini.jca;

import java.beans.PropertyChangeListener;
import java.io.IOException;

import gov.aps.jca.Context;

/**
 * Represents a live mapping of a process variable from EPICS shared memory into
 * Java. Updates to the EPICS variable are propogated to Java automatically via
 * an underlying monitor and are distributed to clients via PropertyChangeEvents, 
 * as are connection state changes.
 * <p>
 * Implementations are meant to be reliable wrappers for all of bookkeeping that 
 * is normally required when using the low level Channel Access libraries. 
 * Channel reconnect must be automatic and hidden in the implementation, as well
 * as I/O flushing and management of the event chains for the connect, get, and 
 * put protocols.
 * <p>
 * IProcessVariable instances are normally created via methods on an
 * IProcessVariableFactory, then open()ed using a JCA Context object. Both the
 * factory and the Context are normally acquired via an OSGi ServiceTracker.
 * @author rnorris
 * @param <T>
 */
public interface ProcessVariable<T> {

	public static final String PROP_VALUE = "value";
	public static final String PROP_CONNECTED = "connected";

	void addPropertyChangeListener(PropertyChangeListener listener);
	void removePropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * Opens the process variable in the provided CA Context. The channel will
	 * be reconnected if possible on network failure.
	 * @param context
	 * @throws IOException
	 */
	void open(Context context) throws IOException;

	/**
	 * Closes the underlying channel and cleans up. Listeners will receive a 
	 * final set of property updates.
	 */
	void close();
	
	/**
	 * Returns the last known value for the process variable. Does not block.
	 * @return
	 */
	T get();
	
	/**
	 * Returns true if the underlying channel is connected. Note that this is
	 * not the same thing as whether the IProcessVariable is open or not; the
	 * underlying channel's connection state may change due to network issues.
	 * @return
	 */
	boolean isConnected();
	
	/**
	 * Sets the value of this process variable. There will be a property update
	 * event when the value has actually changed. This method will not block 
	 * unless another put() is pending completion.
	 * @param value
	 * @throws IOException
	 */
	void put(T value) throws IOException;
	
	/**
	 * Returns the fully-qualified channel name.
	 * @return
	 */
	String getName();
	
}







