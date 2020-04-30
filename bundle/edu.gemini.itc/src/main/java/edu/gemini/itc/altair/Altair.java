package edu.gemini.itc.altair;

import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.gems.GemsFluxAttenuationVisitor;
import edu.gemini.itc.shared.AltairParameters;
import edu.gemini.spModel.gemini.altair.AltairParams;
import java.util.logging.Logger;

/**
 * Altair AO class
 */
public class Altair implements AOSystem {
    private static final Logger Log = Logger.getLogger( Altair.class.getName() );

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

    private static final double geometricFactor = 1.0;

    private final AltairParameters altair;
    private final double wavelength;
    private final double telescopeDiameter;
    private final double uncorrectedSeeing;
    private final double extinction;
    private final double fwhmInst;

    private final AltairBackgroundVisitor altairBackground;
    private final AltairTransmissionVisitor altairTransmission;

    public Altair(double wavelength, double telescopeDiameter, double uncorrectedSeeing, double extinction, AltairParameters altair, double fwhmInst) {
        this.altair = altair;
        this.wavelength = wavelength;
        this.telescopeDiameter = telescopeDiameter;
        this.extinction = extinction;
        this.uncorrectedSeeing = uncorrectedSeeing;
        this.fwhmInst = fwhmInst;
        this.altairBackground = new AltairBackgroundVisitor();
        this.altairTransmission = new AltairTransmissionVisitor();
        Log.fine(String.format("Extinction = %.1f mag", extinction));

        validateInputParameters(altair);
    }

    public void validateInputParameters(final AltairParameters p) {
        // Limiting magnitudes from the public Altair web pages:
        double maglimit_NGS = 15.1;
        double maglimit_LGS = 18.5;

        if (p.wfsMode().equals(AltairParams.GuideStarType.LGS) && (p.guideStarMagnitude() + extinction) > maglimit_LGS)
            throw new IllegalArgumentException(
                    String.format("Laser guide star must be at least R=%.1f in these conditions.", maglimit_LGS - extinction));

        if (p.wfsMode().equals(AltairParams.GuideStarType.NGS) && (p.guideStarMagnitude() + extinction) > maglimit_NGS)
            throw new IllegalArgumentException(
                    String.format("Natural guide star must be at least R=%.1f in these conditions.", maglimit_NGS - extinction));

        if (p.wfsMode().equals(AltairParams.GuideStarType.LGS) && p.fieldLens().equals(AltairParams.FieldLens.OUT))
            throw new IllegalArgumentException("The field Lens must be IN when Altair is in LGS mode.");
    }

    //Methods
    public AltairParams.GuideStarType getWFSMode() {
        return altair.wfsMode();
    }

    public double getGuideStarSeparation() {
        return altair.guideStarSeparation();
    }

    public double getGuideStarMagnitude() {
        return altair.guideStarMagnitude();
    }

    public SampledSpectrumVisitor getBackgroundVisitor() {
        return altairBackground;
    }

    public SampledSpectrumVisitor getTransmissionVisitor() {
        return altairTransmission;
    }

    public SampledSpectrumVisitor getFluxAttenuationVisitor() {
        return new AltairFluxAttenuationVisitor(getFluxAttenuation());
    }

    public SampledSpectrumVisitor getHaloFluxAttenuationVisitor() {
        return new GemsFluxAttenuationVisitor(1 - getStrehl());
    }

    public double getr0() {
        return (0.1031 / uncorrectedSeeing) * Math.pow(wavelength / 500, 1.2);
    }

    public double getWavelength() {
        return wavelength;
    }
    
    public AltairParams.FieldLens getfieldlens() { return altair.fieldLens(); }

    // Calculates StrehlFit which is used to calc the strehl
    public double getStrehlFit() {
        double r0power, corr;
        double r0 = getr0();

        if (altair.wfsMode().equals(AltairParams.GuideStarType.NGS)) {
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

        if (altair.wfsMode().equals(AltairParams.GuideStarType.NGS)) {
            rgs0 = 14.0; //if NGS
        } else {
            rgs0 = 17.0; //if LGS
        }

        return Math.exp(-1 * Math.pow((altair.guideStarMagnitude() + extinction) / rgs0, 16) * Math.pow(1650 / wavelength, 2));
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

        return Math.exp(-1 * Math.pow(altair.guideStarSeparation() / dgs0, dgspow) * Math.pow(1650 / wavelength, 2));
    }

    // Function that calculates the strehl from the fit, distance and magnitude
    public double getStrehl() {
        return getStrehlFit() * getStrehlNoiseDist() * getStrehlNoiseMag();
    }

    public double getFluxAttenuation() {
        return geometricFactor * getStrehl();
    }

    public double getAOCorrectedFWHM() {
        double fwhmAO = Math.sqrt(6.817E-10 * Math.pow(wavelength, 2) + 6.25E-4);
        return Math.sqrt(fwhmAO * fwhmAO + fwhmInst * fwhmInst); // REL-472
    }

    private boolean fieldLensIsIn() {
        return altair.fieldLens().equals(AltairParams.FieldLens.IN);
    }

}
