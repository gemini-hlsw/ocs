package jsky.app.ot.gemini.bhros.ech;

import java.text.NumberFormat;

/**
 * Change wavelength microns to and from display units. Right now it's nanometers
 * but could change to angstroms if needed.
 */
public class EchellogramDisplayUnits {

	public static final String UNIT_NAME = "nanometer"; 
	public static final String UNIT_NAME_PLURAL = "nanometers"; 
	public static final String UNIT_ABBREV = "nm"; 
	public static final String FORMAT_STRING = "0.00";
	public static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
	static {
		NUMBER_FORMAT.setMinimumFractionDigits(2);
		NUMBER_FORMAT.setMaximumFractionDigits(2);
		NUMBER_FORMAT.setMinimumIntegerDigits(1);
	}
	
	public static double fromMicrons(double microns) {
		return microns * 1000.0;
	}
	
	public static String formatMicrons(double microns) {
		return NUMBER_FORMAT.format(fromMicrons(microns));
	}
	
	public static double toMicrons(double displayUnits) {
		return displayUnits / 1000.0;
	}
	
}
