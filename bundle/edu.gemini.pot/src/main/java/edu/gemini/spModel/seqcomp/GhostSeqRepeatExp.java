package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.ghost.Ghost$;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

/**
 * A SeqRepeat specifically for GHOST.
 * The difference here is that GHOST does not have a single exposure time, but due to having two detectors - a red
 * and a blue - exposure times and exposure counts for each. Additionally, GHOST does not have coadds.
 */
public abstract class GhostSeqRepeatExp extends SeqRepeat implements GhostExpSeqComponent, Serializable {
    private static final long serialVersionUID = 1L;

    private double redExposureTime   = InstConstants.DEF_EXPOSURE_TIME;
    private int    redExposureCount  = InstConstants.DEF_REPEAT_COUNT;
    private double blueExposureTime  = InstConstants.DEF_EXPOSURE_TIME;
    private int    blueExposureCount = InstConstants.DEF_REPEAT_COUNT;

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
            firePropertyChange(Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP(), oldValue, newValue);
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
            firePropertyChange(Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP().getName(), oldValue, newValue);
        }
    }

    @Override
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);

        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), redExposureTime);
        Pio.addIntParam(factory, paramSet, Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP().getName(), redExposureCount);
        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), blueExposureTime);
        Pio.addIntParam(factory, paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP().getName(), blueExposureCount);

        return paramSet;
    }

    @Override
    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

        setRedExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
        setRedExposureCount(Pio.getIntValue(paramSet, Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP().getName(), InstConstants.DEF_REPEAT_COUNT));
        setBlueExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
        setRedExposureCount(Pio.getIntValue(paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP().getName(), InstConstants.DEF_REPEAT_COUNT));
    }
}
