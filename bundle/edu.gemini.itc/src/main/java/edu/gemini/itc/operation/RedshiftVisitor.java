package edu.gemini.itc.operation;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.spModel.core.Redshift;
import java.util.logging.Logger;

/**
 * This visitor performs a redshift on the spectrum.
 */
public class RedshiftVisitor implements SampledSpectrumVisitor {
    private static final Logger Log = Logger.getLogger(RedshiftVisitor.class.getName());

    /**
     * For efficiency, no shift will be performed unless |z| is
     * larger than this value
     */
    public static final double MIN_SHIFT = 0.0001;

    private final double _z;  // z = v / c

    /**
     * @param z redshift = velocity / c
     */
    public RedshiftVisitor(final Redshift redshift) {
        _z = redshift.z();
    }

    /**
     * Performs the redshift on specified spectrum.
     */
    public void visit(final SampledSpectrum sed) {
        if (getShift() <= -0.9) {
            throw new IllegalArgumentException("Redshift must be > -0.9");
        } else if (Math.abs(getShift()) > MIN_SHIFT) {
            Log.fine("Applying redshift: z = " + getShift());
            sed.rescaleX(1.0 + getShift());
        }
    }

    /**
     * @return the redshift
     */
    public double getShift() {
        return _z;
    }

}
