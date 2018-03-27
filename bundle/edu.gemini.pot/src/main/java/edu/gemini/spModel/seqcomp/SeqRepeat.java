// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeat.java 7987 2007-08-02 23:16:52Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.data.AbstractDataObject;

import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.obsclass.ObsClass;


public class SeqRepeat extends AbstractDataObject implements ISPSeqObject {

    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_REPEAT;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeat> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeat(), c -> new SeqRepeatCB(c));

    // for serialization
    private static final long serialVersionUID = 3L;

    private int _repeatCount = 1;

    // Observe/charging class
    private ObsClass _obsClass;

    public SeqRepeat() {
        super(SP_TYPE);
    }

    /**
     * Constructor for subclasses.
     */
    protected SeqRepeat(SPComponentType spType, ObsClass obsClass) {
        super(spType);
        _obsClass = obsClass;
    }

    /**
     * Return the current repeat count.
     */
    public int getStepCount() {
        return _repeatCount;
    }

    /**
     * Set the repeat count and fire an event.
     */
    public void setStepCount(int newValue) {
        int oldValue = _repeatCount;
        if (newValue != oldValue) {
            _repeatCount = newValue;
            firePropertyChange(InstConstants.REPEAT_COUNT_PROP, oldValue, newValue);
        }
    }

    /**
     * Return the observation (charging) class for this object.
     */
    public ObsClass getObsClass() {
        return _obsClass;
    }

    /**
     * Set the observation (charging) class for this object.
     */
    public void setObsClass(ObsClass newValue) {
        ObsClass oldValue = _obsClass;
        if (newValue != oldValue) {
            _obsClass = newValue;
            firePropertyChange(InstConstants.OBS_CLASS_PROP,
                               oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, InstConstants.REPEAT_COUNT_PROP, String.valueOf(getStepCount()));
        if (_obsClass != null) {
            Pio.addParam(factory, paramSet, InstConstants.OBS_CLASS_PROP, _obsClass.name());
        }

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, InstConstants.REPEAT_COUNT_PROP);
        if (v != null) {
            _repeatCount = Integer.parseInt(v);
        }

        v = Pio.getValue(paramSet, InstConstants.OBS_CLASS_PROP);
        if (v != null) {
            _obsClass = ObsClass.parseType(v);
        }
    }

    public String getTitle() {
        return super.getTitle() + " (" + getStepCount() + "X)";
    }

    public String getEditableTitle() {
        // before editing, remove the count
        return getTitle().replaceAll(" [(][0-9]+X[)]\\z", "");
    }
}

