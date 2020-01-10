package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.gemini.calunit.CalUnitConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.*;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.GhostExpSeqComponent;
import edu.gemini.spModel.seqcomp.GhostSeqRepeatExp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.obsclass.ObsClass;

import java.util.*;

/**
 * The GHOST flat iterator, and equivalent of SeqRepeatFlatObs.
 */
final public class GhostSeqRepeatFlatObs extends GhostSeqRepeatExp
    implements GhostExpSeqComponent {
    private static final long serialVersionUID = 1L;

    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_GHOST_GEMFLAT;

    public static final ISPNodeInitializer<ISPSeqComponent, GhostSeqRepeatFlatObs> NI =
            new ComponentNodeInitializer<>(SP_TYPE, GhostSeqRepeatFlatObs::new, GhostSeqRepeatFlatObsCB::new);

    public static final String OBSERVE_TYPE = InstConstants.FLAT_OBSERVE_TYPE;

    private Set<Lamp> lamps = new TreeSet<>();
    private Shutter shutter = Shutter.DEFAULT;
    private Filter filter = Filter.DEFAULT;
    private Diffuser diffuser = Diffuser.DEFAULT;

    public GhostSeqRepeatFlatObs() {
        super(SP_TYPE, ObsClass.PARTNER_CAL);
        lamps.add(Lamp.DEFAULT);
    }

    @Override
    public Object clone() {
        final GhostSeqRepeatFlatObs copy = (GhostSeqRepeatFlatObs)super.clone();
        copy.lamps = new TreeSet<>(lamps);
        return copy;
    }

    @Override
    public String getTitle() {
        return (isArc() ? "Manual Ghost Arc: " : "Manual Ghost Flat: ") + Lamp.show(lamps, Lamp::displayValue)
                + " (" + getStepCount() + "X)";
    }

    @Override
    public String getObserveType() {
        return (isArc() ? InstConstants.ARC_OBSERVE_TYPE :
                InstConstants.FLAT_OBSERVE_TYPE);
    }

    public int getLampCount() {
        return lamps.size();
    }

    public Set<Lamp> getLamps() {
        return new TreeSet<>(lamps);
    }
    public void setLamp(Lamp newValue) {
        final Set<Lamp> oldValue = lamps;
        lamps = new TreeSet<>();
        lamps.add(newValue);
        if (!lamps.equals(oldValue)) {
            firePropertyChange(CalUnitConstants.LAMP_PROP, oldValue, lamps);
        }
    }
    public void setLamps(Collection<Lamp> newValue) {
        final Set<Lamp> oldValue = lamps;
        if (!newValue.equals(oldValue)) {
            lamps = new TreeSet<>(newValue);
            firePropertyChange(CalUnitConstants.LAMP_PROP, oldValue, newValue);
        }
    }
    private void setLampsFromFormattedNames(String formattedList) {
        setLamps(Lamp.read(formattedList));
    }

    public boolean isArc() { return Lamp.containsArc(lamps); }

    public Shutter getShutter() {
        return shutter;
    }
    public void setShutter(Shutter newValue) {
        final Shutter oldValue = getShutter();
        if (!newValue.equals(oldValue)) {
            shutter = newValue;
            firePropertyChange(CalUnitConstants.SHUTTER_PROP, oldValue, newValue);
        }
    }

    public Filter getFilter() {
        return filter;
    }
    public void setFilter(Filter newValue) {
        final Filter oldValue = getFilter();
        if (!newValue.equals(oldValue)) {
            filter = newValue;
            firePropertyChange(CalUnitConstants.FILTER_PROP, oldValue, newValue);
        }
    }

    public Diffuser getDiffuser() {
        return diffuser;
    }
    public void setDiffuser(Diffuser newValue) {
        final Diffuser oldValue = getDiffuser();
        if (!newValue.equals(oldValue)) {
            diffuser = newValue;
            firePropertyChange(CalUnitConstants.DIFFUSER_PROP, oldValue, newValue);
        }
    }

    public ObsClass getDefaultObsClass() {
        if (isArc()) return ObsClass.PROG_CAL;
        return ObsClass.PARTNER_CAL;
    }

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, CalUnitConstants.LAMP_PROP,     Lamp.show(getLamps(), Lamp::name));
        Pio.addParam(factory, paramSet, CalUnitConstants.SHUTTER_PROP,  getShutter().name());
        Pio.addParam(factory, paramSet, CalUnitConstants.FILTER_PROP,   getFilter().name());
        Pio.addParam(factory, paramSet, CalUnitConstants.DIFFUSER_PROP, getDiffuser().name());

        return paramSet;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        ImOption.apply(Pio.getValue(paramSet, CalUnitConstants.LAMP_PROP)).
                forEach(this::setLampsFromFormattedNames);
        ImOption.apply(Pio.getEnumValue(paramSet, CalUnitConstants.SHUTTER_PROP, Shutter.class)).
                forEach(this::setShutter);
        ImOption.apply(Pio.getEnumValue(paramSet, CalUnitConstants.FILTER_PROP, Filter.class)).
                forEach(this::setFilter);
        ImOption.apply(Pio.getEnumValue(paramSet, CalUnitConstants.DIFFUSER_PROP, Diffuser.class)).
                forEach(this::setDiffuser);

        // Only set the obs class if null: otherwise we use the partner obs class.
        if (ImOption.apply(Pio.getEnumValue(paramSet, InstConstants.OBS_CLASS_PROP, getDefaultObsClass())).isEmpty())
            setObsClass(getDefaultObsClass());
    }
}
