package edu.gemini.itc.gmos;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;

/**
 * This is crazy stuff based off the IRAF script gfoneshift.cl, which takes the central wavelength
 * and the lpmm and outputs the shift I use in the GMOS IFU-2 calculations. This could/should probably
 * be integrated into the DetectorTransmissionVisitor class.
 */
public class CCDGapCalc {

    /** Calculates the shift in unbinned pixels for the given wavelength for IFU-2. */
    public static double calcIfu2Shift(double cwavlen, double lpmm, GmosCommonType.DetectorManufacturer ccdType) {

        final double scale;               // plate scale in arcsecs per pixel
        final double asecmm = 1.611444;   // Arcsecs per mm
        final double sepmm  = 175.;       // Physical separation between IFU-2 slits, in mm.

        switch (ccdType) {
            case E2V:
                scale = 0.0727;
                break;
            case HAMAMATSU:
                scale = 0.080778;
                break;
            default:
                throw new Error("invalid ccd type");
        }

        double greq = (cwavlen*lpmm)/1.e6;

        double[] tilts = new double[66];
        double[] theta_i = new double[66];
        double[] theta_r = new double[66];
        double[] lambda_over_d = new double[66];
        for (int i = 0; i < 66; i++) {
            tilts[i] = i+1;
            theta_i[i] = 90.0 - tilts[i];   // Tilt is measured with respect to incoming beam: Tilt = 90 - theta_i
            theta_r[i] = 40.0 - tilts[i];   // Angle between GMOS collimator and camera = 50 = theta_i - theta_r
            // Grating equation: m lambda = d( sin(theta_i) + sin(theta_r) )
            lambda_over_d[i] = Math.sin(theta_r[i] * Math.PI/180.) + Math.sin(theta_i[i] * Math.PI/180.);
        }
        double tilt = findTilt(lambda_over_d, tilts, greq);
        tilt=tilt*Math.PI/180.0;
        double a=Math.sin(tilt+0.872665)/Math.sin(tilt);
        return sepmm*asecmm/(scale*a);
    }

    public static double findTilt (double[] xArray, double[] yArray, double targetX) {

        final double count = xArray.length;
        double targetY = 0.0;
        // Linear Interpolation (see http://en.wikipedia.org/wiki/Linear_interpolation):
        for (int i=1; i<count; i++){
            // The following takes into account both cases of x decreasing and increasing in value:
            if(((targetX < xArray[i-1]) && (targetX > xArray[i])) || ((targetX > xArray[i-1]) && (targetX < xArray[i]))) {
                targetY = yArray[i-1]+((yArray[i]-yArray[i-1])*((targetX-xArray[i-1])/(xArray[i]-xArray[i-1])));
            }
        }

        return targetY;
    }
}

