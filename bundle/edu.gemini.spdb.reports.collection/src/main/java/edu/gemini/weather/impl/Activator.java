package edu.gemini.weather.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.gemini.epics.IEpicsClient;
import edu.gemini.weather.IWeatherBean;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		
		// Register our WeatherBean as an IEpicsClient and as an IWeatherBean!
		IWeatherBean bean = new WeatherBean();
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(IEpicsClient.EPICS_CHANNELS, WeatherBean.CHANNELS);
		context.registerService(new String[] { IEpicsClient.class.getName(), IWeatherBean.class.getName() }, bean, props);
		
	}

	public void stop(BundleContext arg0) throws Exception {
		
		// Unregistration is automatic!
		
	}

}
