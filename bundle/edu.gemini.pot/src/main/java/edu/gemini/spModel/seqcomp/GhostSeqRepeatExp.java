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
 * A SeqRepeat specifically for GHOST: no coadds, two exposures.
 */
public class GhostSeqRepeatExp extends SeqRepeat implements Serializable {
    private static final long serialVersionUID = 1L;

    private double redExposureTime = InstConstants.DEF_EXPOSURE_TIME;
    private double blueExposureTime = InstConstants.DEF_EXPOSURE_TIME;

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

    @Override
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);

        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), redExposureTime);
        Pio.addDoubleParam(factory, paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), blueExposureTime);

        return paramSet;
    }

    @Override
    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

        setRedExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
        setBlueExposureTime(Pio.getDoubleValue(paramSet, Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(), InstConstants.DEF_EXPOSURE_TIME));
    }
}
