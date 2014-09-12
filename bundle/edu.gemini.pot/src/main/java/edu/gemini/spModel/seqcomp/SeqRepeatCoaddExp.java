// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatCoaddExp.java 7987 2007-08-02 23:16:52Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;


/**
 * This class is provided as a super class for simple repeating sequence
 * components that need a coadds and exposure time in addition to their
 * repeat value.
 * <p>
 */
public abstract class SeqRepeatCoaddExp extends SeqRepeat
        implements Serializable {

    // for serialization
    private static final long serialVersionUID = 1L;

    private int _coaddsCount = InstConstants.DEF_COADDS;
    private double _exposureTime = InstConstants.DEF_EXPOSURE_TIME;

    /**
     * Constructor for subclasses.
     */
    protected SeqRepeatCoaddExp(SPComponentType spType, ObsClass obsClass) {
        super(spType, obsClass);
    }

    /**
     * Return the current coadds count.
     */
    public int getCoaddsCount() {
        return _coaddsCount;
    }

    /**
     * Set the coadds count and fire an event.
     */
    public void setCoaddsCount(int newValue) {
        int oldValue = _coaddsCount;
        if (newValue != oldValue) {
            _coaddsCount = newValue;
            firePropertyChange(InstConstants.COADDS_PROP, oldValue, newValue);
        }
    }

    /**
     * Return the current exposure time value.
     */
    public double getExposureTime() {
        return _exposureTime;
    }

    /**
     * Set the exposure time and fire an event.
     */
    public void setExposureTime(double newValue) {
        double oldValue = _exposureTime;
        if (newValue != oldValue) {
            _exposureTime = newValue;
            firePropertyChange(InstConstants.EXPOSURE_TIME_PROP, oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, InstConstants.COADDS_PROP, String.valueOf(getCoaddsCount()));
        Pio.addParam(factory, paramSet, InstConstants.EXPOSURE_TIME_PROP, String.valueOf(getExposureTime()));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, InstConstants.COADDS_PROP);
        if (v != null) {
            _coaddsCount = Integer.parseInt(v);
        }
        v = Pio.getValue(paramSet, InstConstants.EXPOSURE_TIME_PROP);
        if (v != null) {
            _exposureTime = Double.parseDouble(v);
        }
    }
}

