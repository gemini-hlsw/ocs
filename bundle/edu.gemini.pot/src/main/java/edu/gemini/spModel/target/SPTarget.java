package edu.gemini.spModel.target;

import edu.gemini.spModel.core.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.system.*;

/** A mutable cell containing an ITarget. */
public final class SPTarget extends TransitionalSPTarget {

    private ITarget _target;
    private Target _newTarget = SiderealTarget.empty();

    public Target getNewTarget() {
        return _newTarget;
    }

    public void setNewTarget(Target target) {
        _newTarget = target;
        System.out.println("*** SPTarget.setNewTarget: " + target);
        _notifyOfUpdate();
    }

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
        if (tag != _target.getTag()) {
            _target = ITarget.forTag(tag);
            switch (tag) {
                case SIDEREAL:
                    setNewTarget(SiderealTarget.empty());
                    break;
                default:
                    setNewTarget(edu.gemini.spModel.core.NonSiderealTarget.empty());
            }
            _notifyOfUpdate();
        }
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
