package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.gemini.calunit.CalUnitConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.*;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.ICoaddExpSeqComponent;
import edu.gemini.spModel.seqcomp.SeqRepeatCoaddExp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.obsclass.ObsClass;

import java.util.*;

/**
 * A simple "Flat" iterator that does a flat observe for X times
 * with coadds and exposure time.
 */
public class SeqRepeatFlatObs extends SeqRepeatCoaddExp
        implements ICoaddExpSeqComponent {

    private static final long serialVersionUID = 2L;

    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_GEMFLAT;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatFlatObs> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeatFlatObs(), c -> new SeqRepeatFlatObsCB(c));

    public static final String OBSERVE_TYPE = InstConstants.DARK_OBSERVE_TYPE;

    // -- Private variables --

    private Set<Lamp> _lamps = new TreeSet<Lamp>();
    private Shutter _shutter = Shutter.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private Diffuser _diffuser = Diffuser.DEFAULT;

    /**
     * Default constructor.
     */
    public SeqRepeatFlatObs() {
        super(SP_TYPE, ObsClass.PARTNER_CAL);
        _lamps.add(Lamp.DEFAULT);
    }

    public Object clone() {
        SeqRepeatFlatObs copy = (SeqRepeatFlatObs)super.clone();
        copy._lamps = new TreeSet<Lamp>(_lamps);
        return copy;
    }

    /**
     * Override getTitle to return the observe count.
     */
    public String getTitle() {
        return (isArc() ? "Manual Arc: " : "Manual Flat: ") + Lamp.show(_lamps, Lamp::displayValue) + " (" + getStepCount() + "X)";
    }

    /**
     * Return the observe type property for this seq comp.
     */
    public String getObserveType() {
        return (isArc() ? InstConstants.ARC_OBSERVE_TYPE :
                InstConstants.FLAT_OBSERVE_TYPE);
    }


    /**
     * Return the number of lamps or arcs (will be at least 1)
     */
    public int getLampCount() {
        return _lamps.size();
    }

    /**
     * Return the selected lamps (one lamp or one or more arcs)
     */
    public Set<Lamp> getLamps() {
        return new TreeSet<Lamp>(_lamps);
    }

    /**
     * Set the lamp (set list to a single lamp).
     */
    public void setLamp(Lamp newValue) {
        Set<Lamp> oldValue = _lamps;
        _lamps = new TreeSet<Lamp>();
        _lamps.add(newValue);
        if (!_lamps.equals(oldValue)) {
            firePropertyChange(CalUnitConstants.LAMP_PROP, oldValue, _lamps);
        }
    }

    /**
     * Set the list of lamps.
     */
    public void setLamps(Collection<Lamp> newValue) {
        Set<Lamp> oldValue = _lamps;
        if (!newValue.equals(oldValue)) {
            _lamps = new TreeSet<Lamp>(newValue);
            firePropertyChange(CalUnitConstants.LAMP_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Lamps with a String.
     */
    private void _setLampsFromFormattedNames(String formattedList) {
        setLamps(Lamp.read(formattedList));
    }


    /** Return true if one or more arc lamps are selected */
    public boolean isArc() { return Lamp.containsArc(_lamps); }

    /**
     * Return the shutter as a Shutter object.
     */
    public Shutter getShutter() {
        return _shutter;
    }

    /**
     * Set the shutter.
     */
    public void setShutter(Shutter newValue) {
        Shutter oldValue = getShutter();
        if (!newValue.equals(oldValue)) {
            _shutter = newValue;
            firePropertyChange(CalUnitConstants.SHUTTER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Shutter with a String.
     */
    private void _setShutter(String name) {
        Shutter oldValue = getShutter();
        Shutter shutter = Shutter.getShutter(name, oldValue);

        // XXX: Fix for older XML files saved with wrong shutter value
        if (shutter == Shutter.OPEN && _lamps.size() == 1) {
            Lamp lamp = _lamps.iterator().next();
            if (lamp != Lamp.IR_GREY_BODY_HIGH && lamp != Lamp.IR_GREY_BODY_LOW) {
                shutter = Shutter.CLOSED;
            }
        }

        setShutter(shutter);
    }


    /**
     * Return the filter as a Filter object.
     */
    public Filter getFilter() {
        return _filter;
    }

    /**
     * Set the filter.
     */
    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (!newValue.equals(oldValue)) {
            _filter = newValue;
            firePropertyChange(CalUnitConstants.FILTER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Filter with a String.
     */
    private void _setFilter(String name) {
        Filter oldValue = getFilter();
        setFilter(Filter.getFilter(name, oldValue));
    }

    /**
     * Return the diffuser as a Diffuser object.
     */
    public Diffuser getDiffuser() {
        return _diffuser;
    }

    /**
     * Set the diffuser.
     */
    public void setDiffuser(Diffuser newValue) {
        Diffuser oldValue = getDiffuser();
        if (!newValue.equals(oldValue)) {
            _diffuser = newValue;
            firePropertyChange(CalUnitConstants.DIFFUSER_PROP,
                               oldValue, newValue);
        }
    }

    /**
     * Set the Diffuser with a String.
     */
    private void _setDiffuser(String name) {
        Diffuser oldValue = getDiffuser();
        setDiffuser(Diffuser.getDiffuser(name, oldValue));
    }


    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, CalUnitConstants.LAMP_PROP,     Lamp.show(getLamps(), Lamp::name));
        Pio.addParam(factory, paramSet, CalUnitConstants.SHUTTER_PROP,  getShutter().name());
        Pio.addParam(factory, paramSet, CalUnitConstants.FILTER_PROP,   getFilter().name());
        Pio.addParam(factory, paramSet, CalUnitConstants.DIFFUSER_PROP, getDiffuser().name());

        return paramSet;
    }

    public ObsClass getDefaultObsClass() {
        if (isArc()) return ObsClass.PROG_CAL;
        return ObsClass.PARTNER_CAL;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, CalUnitConstants.LAMP_PROP);
        if (v != null) {
            _setLampsFromFormattedNames(v);
        }
        v = Pio.getValue(paramSet, CalUnitConstants.SHUTTER_PROP);
        if (v != null) {
            _setShutter(v);
        }
        v = Pio.getValue(paramSet, CalUnitConstants.FILTER_PROP);
        if (v != null) {
            _setFilter(v);
        }
        v = Pio.getValue(paramSet, CalUnitConstants.DIFFUSER_PROP);
        if (v != null) {
            _setDiffuser(v);
        }

        // OT-411
        v = Pio.getValue(paramSet, InstConstants.OBS_CLASS_PROP);
        if (v == null) {
            setObsClass(getDefaultObsClass());
        }

    }
}
