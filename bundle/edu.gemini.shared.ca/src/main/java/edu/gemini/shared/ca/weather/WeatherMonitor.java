package edu.gemini.shared.ca.weather;

import gov.aps.jca.CAException;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main class that shows the Cerro Pachon weather monitor.
 * @author rnorris
 */
public class WeatherMonitor {
	
	static {		
		System.setProperty("apple.awt.brushMetalLook", "true");
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "172.17.2.255");
		System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");		
	}
	
	public static void main(String[] args) throws CAException {
		WeatherFrame wf = new WeatherFrame();
		wf.setVisible(true);
		wf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});
	}

}
