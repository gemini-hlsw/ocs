// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
//
package edu.gemini.itc.altair;

import edu.gemini.itc.shared.FormatStringWriter;

/**
 * Altair AO class
 */
public class Altair {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String ALTAIR_LIB = "/altair";

    /**
     * Related files will start with this prefix
     */
    public static final String ALTAIR_PREFIX = "altair_";

    /**
     * Name of the altair background file
     */
    public static final String ALTAIR_BACKGROUND_FILENAME = "background";

    /**
     * Name of the Altair transmission file
     */
    public static final String ALTAIR_TRANSMISSION_FILENAME = "transmission";

    public static final String NGS = "naturalGuideStar";
    public static final String LGS = "laserGuideStar";

    private double wavelength, wavelengthMeters, telescopeDiameter, uncorrectedSeeing, guideStarDistance, guideStarMag, fwhmInst;
    private static final double geometricFactor = 1.0;

    private AltairBackgroundVisitor altairBackground;
    private AltairTransmissionVisitor altairTransmission;
    private String observingMode;  // LGS or NGS
    private boolean fieldLens; // Is field lens in use


    // Constructor
    // See REL-472
    public Altair(double wavelength, double telescopeDiameter, double uncorrectedSeeing, double guideStarDistance,
                  double guideStarMag, String observingMode, boolean fieldLens, double fwhmInst) throws Exception {
        altairBackground = new AltairBackgroundVisitor();
        altairTransmission = new AltairTransmissionVisitor();
        this.wavelength = wavelength;
        this.wavelengthMeters = wavelength * 10E9;
        this.telescopeDiameter = telescopeDiameter;
        this.uncorrectedSeeing = uncorrectedSeeing;
        this.guideStarDistance = guideStarDistance;
        this.guideStarMag = guideStarMag;
        this.observingMode = observingMode;
        this.fieldLens = fieldLens;
        this.fwhmInst = fwhmInst;
    }

    //Methods
    public AltairBackgroundVisitor getBackground() {
        return altairBackground;
    }

    public AltairTransmissionVisitor getTransmission() {
        return altairTransmission;
    }

    public double getr0() {
        //double r0_500 = (500E-9)/(uncorrectedSeeing*4.848E-6);
        //double r0 = r0_500*Math.pow((wavelengthMeters/500), 1.2);

        //Phil combined and simplified the above two eqations (notice he doesn't use the wavelength in meters anymore).
        return (0.1031 / uncorrectedSeeing) * Math.pow(wavelength / 500, 1.2);
    }

    // Calculates StrehlFit which is used to calc the strehl
    public double getStrehlFit() {
        double r0power, corr;
        double r0 = getr0();

        if (getObservingMode().equals(NGS)) {
            r0power = 0.96; //if NGS
            corr = 0.20; //if NGS
        } else {
            r0power = 0.79; //if LGS
            corr = 0.37; //if LGS
        }


        //double sigma = 0.04*Math.pow((telescopeDiameter/r0),1.66);  //old equation
        double sigma = corr * Math.pow((telescopeDiameter / r0), r0power);
        double strehl = Math.exp(-1 * sigma);

        return strehl;
    }

    // Calculates the strehl noise from the guideStar Magnitude
    //  the return values is used to calculate the strehl
    public double getStrehlNoiseMag() {
        double rgs0;

        if (getObservingMode().equals(NGS)) {
            rgs0 = 14.0; //if NGS
        } else {
            rgs0 = 17.0; //if LGS
        }

        // double strehlNoiseMag = Math.exp(-1*Math.pow(guideStarMag/14.5,16)*Math.pow(1650/wavelength,2));  //Old equation
        return Math.exp(-1 * Math.pow(guideStarMag / rgs0, 16) * Math.pow(1650 / wavelength, 2));
    }

    // Calculates the strehl noise from the guide star distance
    //  the return values is used to calculate the strehl
    public double getStrehlNoiseDist() {
        double dgs0, dgspow;

        if (fieldLensIsIn()) {
            dgs0 = 30d; //if FL in (FL always in for LGS)
            dgspow = 2d; //if FL in (FL always in for LGS)

        } else {
            dgs0 = 5d; //if FL out
            dgspow = 1d; //if FL out
        }
        //double strehlNoiseDistance = Math.exp(-1*Math.pow(guideStarDistance/12.5,2)*Math.pow(1650/wavelength,2));  //Old caclulation

        return Math.exp(-1 * Math.pow(guideStarDistance / dgs0, dgspow) * Math.pow(1650 / wavelength, 2));
    }

    // Function that calculates the strehl from the fit, distance and magnitude
    public double getStrehl() {

        return getStrehlFit() * getStrehlNoiseDist() * getStrehlNoiseMag();
//                * (getAOCorrectedFWHM()/getAOCorrectedFWHMc()); // REL-472
    }

    public double getFluxAttenuation() {
        return geometricFactor * getStrehl();
    }

    public double getAOCorrectedFWHM() {
        //double fwhmAO = Math.sqrt(Math.pow(wavelength/telescopeDiameter/4.848E-6,2)+Math.pow(0.035,2));
        return Math.sqrt(6.817E-10 * Math.pow(wavelength, 2) + 6.25E-4); //Phil simplified the above equation
    }


    public double getAOCorrectedFWHMc() {
        double fwhmAO = getAOCorrectedFWHM();
        return Math.sqrt(fwhmAO * fwhmAO + fwhmInst * fwhmInst); // REL-472
    }

    public String getObservingMode() {
        return observingMode;
    }

    public boolean fieldLensIsIn() {
        return fieldLens;
    }


    public String printSummary(FormatStringWriter device) {
        String s = "r0(" + wavelength + "nm) = " + device.toString(getr0()) + " m\n";
        s += "Strehl = " + device.toString(getStrehl()) + "\n";
        s += "FWHM of an AO-corrected core = " + device.toString(getAOCorrectedFWHMc()) + " arcsec\n";

//        // XXX Temp DEBUG (REL-472)
//        s += toString();

        return s;
    }

//    //Used for debugging
//    public String toString(){
//        String s = "\nAltair Debug Information:\n";
//
//        s +="r0: " + getr0() + "\n";
//        s +="Total Strehl (S): " + (getStrehlFit()*getStrehlNoiseDist()*getStrehlNoiseMag()) + "\n";
//        s +="Total Strehl (Sc): " + getStrehl()+ "\n";
//        s +="Base Strehl (Sfit): " + getStrehlFit() + "\n";
//        s +="Strehl GSdist Component (Saniso): " + getStrehlNoiseDist() + "\n";
//        s +="Strehl GSmag Component (Snoise): " + getStrehlNoiseMag() + "\n";
//        s +="FluxAttenuation: " + getFluxAttenuation() + "\n";
//        s +="AO Corrected FWHM (fwhmAOc): " + getAOCorrectedFWHMc() + " (fwhmAO=" + getAOCorrectedFWHM() + ")\n";
//        s +="Wavelength: " + wavelength + "\n";
//        s +="Uncorrected Seeing " + uncorrectedSeeing + "\n";
//        s +="Telescope Diameter " + telescopeDiameter + "\n";
//        s +="GS seperation " + guideStarDistance + "\n";
//        s +="GS Mag " + guideStarMag + "\n";
//	    s +="Observing Mode " + getObservingMode() + "\n";
//	    s +="Field Lens " + fieldLens + "\n";
//
//        return s;
//    }

}
