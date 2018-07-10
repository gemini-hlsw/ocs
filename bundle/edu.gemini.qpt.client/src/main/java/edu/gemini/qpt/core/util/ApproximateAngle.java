package edu.gemini.qpt.core.util;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioSerializable;


/**
 * An immutable composite type representing an angle and degree of variance,
 * and a <code>contains()</code> method. This object is PioSerializable, for
 * your convenience :-/
 * @author rnorris
 */
public class ApproximateAngle implements PioSerializable {

    private static final String PROP_VARIANCE = "variance";
    private static final String PROP_ANGLE = "angle";
    
    private final int angle;
    private final int variance;
    
    ///
    /// CONSTRUCTORS AND PIO
    ///
    
    /**
     * Construct a new approximate angle.
     * @param angle will be normalized to 0..360
     * @param variance must be between 0 and 180
     */
    public ApproximateAngle(int angle, int variance) {
        if (variance < 0 || variance > 180) 
            throw new IllegalArgumentException("Variance must be between 0 and 180.");
        while (angle < 0) angle += 360;
        while (angle > 360) angle -= 360;
        this.angle = angle;
        this.variance = variance;
    }

    public ApproximateAngle(ParamSet paramSet) {
        this(
            Pio.getIntValue(paramSet, PROP_ANGLE, 0), 
            Pio.getIntValue(paramSet, PROP_VARIANCE, 0)
        );
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        Pio.addIntParam(factory, params, PROP_ANGLE, angle);
        Pio.addIntParam(factory, params, PROP_VARIANCE, variance);
        return params;
    }

    ///
    /// ACCESSORS
    ///
    
    public int getAngle() {
        return angle;
    }

    public int getVariance() {
        return variance;
    }
    
    ///
    /// DOMAIN METHODS
    ///
    
    public boolean contains(int angle) {
        int lo = this.angle - variance;
        int hi = this.angle + variance;
        while (angle < lo) angle += 360;
        while (angle > hi) angle -= 360;
        return lo <= angle && angle <= hi;
    }

    ///
    /// OBJECT OVERRIDES
    ///
    
    @Override
    public String toString() {
        return angle + "\u00B0\u00B1" + variance + "\u00B0";
    }

    @Override
    public int hashCode() {
        return angle ^ variance;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApproximateAngle) {
            ApproximateAngle a = (ApproximateAngle) obj;
            return angle == a.angle && variance == a.variance;
        }
        return false;
    }
    
    
}
