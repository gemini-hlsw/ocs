package edu.gemini.itc.gems;

import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.AOSystem;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.SampledSpectrumVisitor;

/**
 * Gems AO class
 */
public class Gems implements AOSystem {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String GEMS_LIB = "/gems";

    /**
     * Related files will start with this prefix
     */
    public static final String GEMS_PREFIX = "canopus_";

    /**
     * Name of the gems background file
     */
    public static final String GEMS_BACKGROUND_FILENAME = "background";

    /**
     * Name of the Gems transmission file
     */
    public static final String GEMS_TRANSMISSION_FILENAME = "transmission";

    private double wavelength, wavelengthMeters, telescopeDiameter, uncorrectedSeeing;
    private static final double geometricFactor = 1.0;

    private GemsBackgroundVisitor gemsBackground;
    private GemsTransmissionVisitor gemsTransmission;

    // Average Strehl value entered in the web page
    private double avgStrehl;

    // Strehl band value entered in the web page next to average strehl
    private String strehlBand;

    // Selected IQ setting (in percent) from web page
    private double imageQualityPercentile;

    // Point source or extended source
    private final SourceDefinition source;

    //Constructor
    public Gems(double wavelength, double telescopeDiameter, double uncorrectedSeeing, double avgStrehl,
                String strehlBand, double imageQualityPercentile, SourceDefinition source) {
        gemsBackground = new GemsBackgroundVisitor();
        gemsTransmission = new GemsTransmissionVisitor();
        this.wavelength = wavelength;
        this.wavelengthMeters = wavelength * 10E9;
        this.telescopeDiameter = telescopeDiameter;
        this.uncorrectedSeeing = uncorrectedSeeing;
        this.avgStrehl = avgStrehl;
        this.strehlBand = strehlBand;
        this.imageQualityPercentile = imageQualityPercentile;
        this.source = source;
    }

    //Methods
    public SampledSpectrumVisitor getBackgroundVisitor() {
        return gemsBackground;
    }

    public SampledSpectrumVisitor getTransmissionVisitor() {
        return gemsTransmission;
    }

    public SampledSpectrumVisitor getFluxAttenuationVisitor() {
        return new GemsFluxAttenuationVisitor(getFluxAttenuation());
    }

    public SampledSpectrumVisitor getHaloFluxAttenuationVisitor() {
        return new GemsFluxAttenuationVisitor(1 - getAvgStrehl());
    }

    public double getr0() {
        return (0.1031 / uncorrectedSeeing) * Math.pow(wavelength / 500, 1.2);
    }

    // Function that calculates the strehl from the fit, distance and magnitude
    public double getAvgStrehl() {
        return avgStrehl;
    }

    public double getFluxAttenuation() {
        return geometricFactor * getAvgStrehl();
    }

    public double getAOCorrectedFWHM_oldVersion() {
        return Math.sqrt(6.817E-10 * Math.pow(wavelength, 2) + 6.25E-4); //Phil simplified the above equation
    }

    // See REL-1352:
    // Instead of the current calculation based on first-principles for normal AO systems,
    // the ITC must use the following lookup table to determine the FWHM of the AO-corrected core for
    // calculations involving GeMS (GSAOI, F2+GeMS in the future).
    //
    // J: IQ20=0.08'' IQ70=0.13'' IQ85=0.15"
    // H: IQ20=0.07'' IQ70=0.10'' IQ85=0.13''
    // K: IQ20=0.06'' IQ70=0.09'' IQ85=0.12''
    //
    // If IQAny is selected in the observing conditions, then give the error message,
    // "GeMS cannot be used in IQ=Any conditions"
    //
    // J, H, or K is the band selected in the menu beside where the average Strehl ratio is entered.
    //
    // Context: The GSAOI ITC currently uses the algorithm defined here
    // http://www.gemini.edu/?q=node/10269
    // to calculate the FWHM of the AO-corrected core for GSAOI. However, this value is smaller than
    // the measured FWHM from GSAOI data so the ITC is over-estimating the S/N.
    //
    // Note: The IQ table should only be applied to the Point Source mode,
    public double getAOCorrectedFWHM() {
        return getAOCorrectedFWHM(false);
    }

    // TODO: passing a boolean is a temporary workaround in order to deal with caller that expects this method to throw an exception
    public double getAOCorrectedFWHM(boolean doThrow) {
        switch (source.getProfileType()) {
            case POINT:
                // point source
                final int IQ20 = 20, IQ70 = 70, IQ85 = 85;
                final int iq = (int) (imageQualityPercentile * 100);
                switch (strehlBand.charAt(0)) {
                    case 'J':
                        switch (iq) {
                            case IQ20:
                                return 0.08;
                            case IQ70:
                                return 0.13;
                            case IQ85:
                                return 0.15;
                        }
                        break;
                    case 'H':
                        switch (iq) {
                            case IQ20:
                                return 0.07;
                            case IQ70:
                                return 0.10;
                            case IQ85:
                                return 0.13;
                        }
                        break;
                    case 'K':
                        switch (iq) {

                            case IQ20:
                                return 0.06;
                            case IQ70:
                                return 0.09;
                            case IQ85:
                                return 0.12;
                        }
                        break;
                }
                break;

            case GAUSSIAN:
                return source.getFWHM();

            default:
                return 0.0;
        }

        // The web page always selects one of J, H or K, so if we get here, the IQ must be wrong
        if (doThrow) {
            // TODO: this is needed for error reporting in the html output, move this away from here!
            throw new IllegalArgumentException("GeMS cannot be used in IQ=Any conditions");
        } else {
            return getAOCorrectedFWHM_oldVersion();
        }
    }


    public String printSummary() {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);
        String s = "r0(" + wavelength + "nm) = " + device.toString(getr0()) + " m\n";
        s += "Average Strehl = " + device.toString(getAvgStrehl() * 100) + "%\n";
        s += "FWHM of an AO-corrected core = ";
        try {
            s += device.toString(getAOCorrectedFWHM(true)) + " arcsec\n";
        } catch (IllegalArgumentException ex) {
            s += "<span style=\"color:red; font-style:italic;\">Error: " + ex.getMessage() + "</span>\n";
        }

        return s;
    }

    //Used for debugging
    public String toString() {

        String s = "GeMS Debug Information:";

        s += "\n";

        s += "r0: " + getr0() + "\n";
        s += "Average Strehl: " + getAvgStrehl() * 100 + "\n";
        s += "FluxAttenuation: " + getFluxAttenuation() + "\n";
        try {
            s += "AO Corrected FWHM: " + getAOCorrectedFWHM() + "\n";
        } catch (IllegalArgumentException ex) {
            s += ex.getMessage() + "\n";
        }
        s += "Wavelength: " + wavelength + "\n";
        s += "Uncorrected Seeing " + uncorrectedSeeing + "\n";
        s += "Telescope Diameter " + telescopeDiameter + "\n";

        return s;
    }

}
