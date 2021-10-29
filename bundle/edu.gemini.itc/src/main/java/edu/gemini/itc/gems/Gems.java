package edu.gemini.itc.gems;

import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.shared.ExactIq;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.core.UniformSource$;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality;
import static edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality.*;
import edu.gemini.spModel.core.GaussianSource;
import edu.gemini.spModel.core.PointSource$;

import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Gems AO class
 */
public class Gems implements AOSystem {
    private static final Logger Log = Logger.getLogger( Gems.class.getName() );

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

    private final double wavelength;
    private final double uncorrectedSeeing;
    private static final double geometricFactor = 1.0;

    private final GemsBackgroundVisitor gemsBackground;
    private final GemsTransmissionVisitor gemsTransmission;

    // Average Strehl value entered in the web page
    private final double avgStrehl;

    // Strehl band value entered in the web page next to average strehl
    private final String strehlBand;

    // Selected IQ setting
    private final ImEither<ExactIq, ImageQuality> iq;

    // Point source or extended source
    private final SourceDefinition source;

    //Constructor
    public Gems(
            double wavelength,
            double uncorrectedSeeing,
            double avgStrehl,
            String strehlBand,
            ImEither<ExactIq, ImageQuality> iq,
            SourceDefinition source
    ) {
        gemsBackground = new GemsBackgroundVisitor();
        gemsTransmission = new GemsTransmissionVisitor();
        this.wavelength = wavelength;
        this.uncorrectedSeeing = uncorrectedSeeing;
        this.avgStrehl = avgStrehl;
        this.strehlBand = strehlBand;
        this.iq = iq;
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

    public double getWavelength() {
        return wavelength;
    }

    public String getStrehlBand() {
        return strehlBand;
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
    // Note: The IQ table should only be applied to the Point Source mode.

    public double getAOCorrectedFWHM() {
        return getAOCorrectedFWHM(false);
    }

    private static final Map<Tuple2<Character, ImageQuality>, Double> FWHM;

    static {
        final Map<Tuple2<Character, ImageQuality>, Double> m = new HashMap<>();

        m.put(new Pair<>('J', PERCENT_20), 0.08);
        m.put(new Pair<>('J', PERCENT_70), 0.13);
        m.put(new Pair<>('J', PERCENT_85), 0.15);
        m.put(new Pair<>('H', PERCENT_20), 0.07);
        m.put(new Pair<>('H', PERCENT_70), 0.10);
        m.put(new Pair<>('H', PERCENT_85), 0.13);
        m.put(new Pair<>('K', PERCENT_20), 0.06);
        m.put(new Pair<>('K', PERCENT_70), 0.09);
        m.put(new Pair<>('K', PERCENT_85), 0.12);

        FWHM = Collections.unmodifiableMap(m);
    }

    // TODO: passing a boolean is a temporary workaround in order to deal with caller that expects this method to throw an exception
    public double getAOCorrectedFWHM(boolean doThrow) {
        if (source.profile() == PointSource$.MODULE$ || source.profile() == UniformSource$.MODULE$) {
            // point source and REL-1371, added Uniform background source to calculate AO-corrected FWHM properly
            final char band = strehlBand.charAt(0);
            if (band == 'J' || band == 'H' || band == 'K') {

                if (iq.exists(iqEnum -> iqEnum == ANY)) {
                    // The web page always selects one of J, H or K, so if we get here, the IQ must be wrong
                    if (doThrow) {
                        // TODO: this is needed for error reporting in the html output, move this away from here!
                        throw new IllegalArgumentException("GeMS cannot be used in IQ=Any conditions");
                    } else {
                        return getAOCorrectedFWHM_oldVersion();
                    }
                } else {

                    double fwhm = 0.0;
                    if (iq.isLeft()) {  // Exact IQ:  use linear fit to above table of FWHM values
                        double exactiq = iq.toOptionLeft().getValue().toArcsec();
                        switch (strehlBand.charAt(0)) {
                            case 'J':
                                fwhm = 0.1525 * exactiq + 0.02598;
                                break;
                            case 'H':
                                fwhm = 0.1328 * exactiq + 0.01811;
                                break;
                            case 'K':
                                fwhm = 0.1328 * exactiq + 0.01475;
                                break;
                        }
                    } else {  // IQ bin
                        fwhm = FWHM.getOrDefault(new Pair<>(strehlBand.charAt(0), iq.toOption().getValue()), 0.0);
                     }
                    Log.fine(String.format("FWHM of AO-corrected core = %.3f arcsec", fwhm));
                    return fwhm;
                }

            } else {
                throw new IllegalArgumentException("Current ITC implementation for GeMS does not support band " + strehlBand);
            }

        } else if (source.profile() instanceof GaussianSource) {
            final double fwhm = ((GaussianSource) source.profile()).fwhm();
            Log.fine(String.format("FWHM of AO-corrected core = Gaussian source FWHM = %.3f arcsec", fwhm));
            return fwhm;

        } else {
            return 0.0;
        }

    }

}
