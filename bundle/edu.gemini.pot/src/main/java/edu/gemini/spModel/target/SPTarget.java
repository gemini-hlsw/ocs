// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPTarget.java 45443 2012-05-23 20:26:52Z abrighton $
//
package edu.gemini.spModel.target;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.system.*;

/** A mutable cell containing an ITarget. */
public final class SPTarget extends TransitionalSPTarget {

    private ITarget _target;

    /** SPTarget with default empty target. */
    public SPTarget() {
        this(new HmsDegTarget()); // why not
    }

    /** SPTarget with given target. */
    public SPTarget(final ITarget target) {
        _target = target;
    }

    /** SPTarget with the given RA/Dec in degrees. */
    public SPTarget(final double raDeg, final double degDec) {
        this();
        setRaDecDegrees(raDeg, degDec);
    }

    /** Return the contained target. */
    public ITarget getTarget() {
        return _target;
    }

    /** Replace the contained target and notify listeners. */
    public void setTarget(final ITarget target) {
        _target = target;
        _notifyOfUpdate();
    }

    /**
     * Replace the contained target with a new, empty target of the specified type, or do nothing
     * if the contained target is of the specified type.
     */
    public void setTargetType(final ITarget.Tag tag) {
        if (tag != _target.getTag())
            setTarget(ITarget.forTag(tag));
    }

    /** Return a paramset describing this SPTarget. */
    public ParamSet getParamSet(final PioFactory factory) {
        return SPTargetPio.getParamSet(this, factory);
    }

    /** Re-initialize this SPTarget from the given paramset */
    public void setParamSet(final ParamSet paramSet) {
        SPTargetPio.setParamSet(paramSet, this);
    }

    /** Construct a new SPTarget from the given paramset */
    public static SPTarget fromParamSet(final ParamSet pset) {
        return SPTargetPio.fromParamSet(pset);
    }

    /** Clone this SPTarget. */
    public SPTarget clone() {
        return new SPTarget(_target.clone());
    }

}
