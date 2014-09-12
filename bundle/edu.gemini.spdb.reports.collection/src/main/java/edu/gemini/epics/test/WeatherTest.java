package edu.gemini.epics.test;

import edu.gemini.epics.IEpicsClient;
import edu.gemini.epics.impl.ChannelBindingSupport;
import gov.aps.jca.CAException;

import java.util.Map;
import java.util.TreeMap;

/**
 * Trivial test that does not use the OSGi activator.
 */
public class WeatherTest implements IEpicsClient {

	/** Map the channel names to friendly text names. */
	private static final Map<String, String> CHANNELS = new TreeMap<String, String>();
	static {
        CHANNELS.put("flam:sad:WINPOS", "Window Cover");
		CHANNELS.put("flam:sad:DCKERPOS", "Decker");
        CHANNELS.put("flam:sad:MOSPOS", "MOS Wheel");

        CHANNELS.put("flam:cc:applyC", "CC Activity");
        CHANNELS.put("flam:cc:state", "CC State");
        CHANNELS.put("flam:cc:health", "CC Health");
        CHANNELS.put("flam:cc:heartbeat", "CC Heartbeat");
	}

	private final ChannelBindingSupport cbs;

	public WeatherTest() throws CAException {
		 cbs = new ChannelBindingSupport(this);
        for (String channel : CHANNELS.keySet()) {
            cbs.bindChannel(channel);
        }
	}

	public void channelChanged(String channel, Object value) {
		System.out.println(CHANNELS.get(channel) + ": " + value);
	}

	public void connected() {
		// This will not be called
	}

	public void disconnected() {
		// This will not be called
	}

	@Override
	protected void finalize() throws Throwable {
		cbs.close();
		super.finalize();
	}

	public static void main(String[] args) throws InterruptedException, CAException {

//		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "172.17.2.255");
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "172.17.102.130");
		System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");

		new WeatherTest();
		Thread.sleep(10000);
        System.exit(0);
	}

}
