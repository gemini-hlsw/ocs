package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.ghost.Ghost$;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;

/**
 * A SeqRepeat specifically for GHOST: no coadds, two exposures.
 */
public class GhostSeqRepeatExp extends SeqRepeat implements Serializable {
    private static final long serialVersionUID = 1L;

    private double redExposureTime = InstConstants.DEF_EXPOSURE_TIME;
    private int redExposureCount = InstConstants.DEF_REPEAT_COUNT;
    private double blueExposureTime = InstConstants.DEF_EXPOSURE_TIME;
    private int blueExposureCount = InstConstants.DEF_REPEAT_COUNT;

    protected GhostSeqRepeatExp(SPComponentType spType, ObsClass obsClass) {
        super(spType, obsClass);
    }

    public double getRedExposureTime() {
        return redExposureTime;
    }
    public void setRedExposureTime(double newValue) {
        final double oldValue = redExposureTime;
        if (oldValue != newValue) {
            redExposureTime = newValue;
            firePropertyChange(Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP(), oldValue, newValue);
        }
    }

    public int getRedExposureCount() {
        return redExposureCount;
    }
    public void setRedExposureCount(int newValue) {
        final int oldValue = redExposureCount;
        if (oldValue != newValue) {
            redExposureCount = newValue;
            firePropertyChange(GHOST_RED_EXPOSURE_COUNT, oldValue, newValue);
        }
    }

    public double getBlueExposureTime() {
        return blueExposureTime;
    }
    public void setBlueExposureTime(double newValue) {
        final double oldValue = blueExposureTime;
        if (oldValue != newValue) {
            blueExposureTime = newValue;
            firePropertyChange(Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP(), oldValue, newValue);
        }
    }

    public int getBlueExposureCount() {
        return blueExposureCount;
    }
    public void setBlueExposureCount(int newValue) {
        final int oldValue = blueExposureCount;
        if (oldValue != newValue) {
            blueExposureCount = newValue;
            firePropertyChange(GHOST_BLUE_EXPOSURE_COUNT, oldValue, newValue);
        }
    }

    @Override
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);

        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), redExposureTime);
        Pio.addIntParam(factory, paramSet, GHOST_RED_EXPOSURE_COUNT.getName(), redExposureCount);
        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), blueExposureTime);
        Pio.addIntParam(factory, paramSet, GHOST_BLUE_EXPOSURE_COUNT.getName(), blueExposureCount);

        return paramSet;
    }

    @Override
    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

        setRedExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
        setRedExposureCount(Pio.getIntValue(paramSet, GHOST_RED_EXPOSURE_COUNT.getName(), InstConstants.DEF_REPEAT_COUNT));
        setBlueExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
        setBlueExposureCount(Pio.getIntValue(paramSet, GHOST_BLUE_EXPOSURE_COUNT.getName(), InstConstants.DEF_REPEAT_COUNT));
    }

    private static final boolean query_yes = true;
    private static final boolean query_no = false;
    private static final boolean iter_yes = true;
    private static final boolean iter_no = false;

    public static PropertyDescriptor GHOST_RED_EXPOSURE_COUNT = PropertySupport.init("redExposureCount", GhostSeqRepeatExp.class, query_no, iter_no);
    public static PropertyDescriptor GHOST_BLUE_EXPOSURE_COUNT = PropertySupport.init("blueExposureCount", GhostSeqRepeatExp.class, query_no, iter_no);
}
