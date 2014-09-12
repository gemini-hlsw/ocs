package edu.gemini.spModel.gemini.bhros.ech;

import java.awt.Color;

/**
 * Utility class for creating RGB colors from wavelengths. Colors not in the visible 
 * range (350-780nm) will be black.<p>
 * Adapted from the FORTRAN routine found at 
 * <a href="http://www.physics.sfasu.edu/astro/color/spectra.html">http://www.physics.sfasu.edu/astro/color/spectra.html</a>.
 */
public class WavelengthColor {

	public static Color getColorFromWaveLength(int nanometers) {

		int intensityMax = 255;

		double gamma = 1.00;
		double r, g, b;
		double factor;

		if (nanometers >= 350 && nanometers <= 439) {
			r = -(nanometers - 440d) / (440d - 350d);
			g = 0.0;
			b = 1.0;
		} else if (nanometers >= 440 && nanometers <= 489) {
			r = 0.0;
			g = (nanometers - 440d) / (490d - 440d);
			b = 1.0;
		} else if (nanometers >= 490 && nanometers <= 509) {
			r = 0.0;
			g = 1.0;
			b = -(nanometers - 510d) / (510d - 490d);
		} else if (nanometers >= 510 && nanometers <= 579) {
			r = (nanometers - 510d) / (580d - 510d);
			g = 1.0;
			b = 0.0;
		} else if (nanometers >= 580 && nanometers <= 644) {
			r = 1.0;
			g = -(nanometers - 645d) / (645d - 580d);
			b = 0.0;
		} else if (nanometers >= 645 && nanometers <= 780) {
			r = 1.0;
			g = 0.0;
			b = 0.0;
		} else {
			r = 0.0;
			g = 0.0;
			b = 0.0;
		}

		if (nanometers >= 350 && nanometers <= 419) {
			factor = 0.3 + 0.7 * (nanometers - 350d) / (420d - 350d);
		} else if (nanometers >= 420 && nanometers <= 700) {
			factor = 1.0;
		} else if (nanometers >= 701 && nanometers <= 780) {
			factor = 0.3 + 0.7 * (780d - nanometers) / (780d - 700d);
		} else {
			factor = 0.0;
		}

		return new Color(
			factorAdjust(r, factor, intensityMax, gamma),
			factorAdjust(g, factor, intensityMax, gamma),
			factorAdjust(b, factor, intensityMax, gamma)
		);
	}

	private static int factorAdjust(double component, double factor, int intensityMax, double gamma) {
		if (component == 0.0) {
			return 0;
		}
		return (int) Math.round(intensityMax * Math.pow(component * factor, gamma));
	}

}
