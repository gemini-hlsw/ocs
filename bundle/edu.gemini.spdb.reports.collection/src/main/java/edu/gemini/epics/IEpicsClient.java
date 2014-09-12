package edu.gemini.epics;

/**
 * Interface for a client that wishes to be told when an Epics channel value is
 * updated. Clients should register instances as services, specifying the channels
 * they are interested in via the property EPICS_CHANNELS, which should be an
 * array of Strings.
 */
public interface IEpicsClient {

	/**
	 * Service property for defing the channels that the client cares about.
	 * Pass this as part of your registration as an array of Strings.
	 */
	String EPICS_CHANNELS = IEpicsClient.class.getName() + ".EPICS_CHANNELS";
	
	/**
	 * Called when the specified channel value changes.
	 */
	void channelChanged(String channel, Object value);
	
	/**
	 * Called when the client is connected.
	 */
	void connected();
	
	/**
	 * Called when the client is disconnected.
	 */
	void disconnected();
	
}
