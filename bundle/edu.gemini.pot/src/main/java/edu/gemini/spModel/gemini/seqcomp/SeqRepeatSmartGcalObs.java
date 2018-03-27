// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatSmartGcalObs.java 37915 2011-10-07 03:29:28Z fnussber $
//
package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.ICoaddExpSeqComponent;
import edu.gemini.spModel.seqcomp.SeqRepeatCoaddExp;

/**
 * Iterator for smart calibrations.
 */
public abstract class SeqRepeatSmartGcalObs extends SeqRepeatCoaddExp
        implements ICoaddExpSeqComponent {

    private static final long serialVersionUID = 2L;

    // The BROAD_TYPE must be "Observer" for observe leaf
    public static final String BROAD_TYPE = "Observer";

    // Different subclasses in order to provide data objects for different smart calibration nodes.
    // The UIInfo class will read the class name and decide on the type of node to be used by using
    // reflection to access the field named SP_TYPE; do *not* remove or rename this field.
    public static class BaselineDay extends SeqRepeatSmartGcalObs {

        // Do not remove/rename!
        public static final SPComponentType SP_TYPE =
                SPComponentType.OBSERVER_BASELINEDAY;

        public static final ISPNodeInitializer<ISPSeqComponent, BaselineDay> NI =
            new ComponentNodeInitializer<>(SP_TYPE, () -> new BaselineDay(), c -> new SeqRepeatSmartGcalObsCB.BasecalDay(c));

        public BaselineDay() {
            super(SP_TYPE);
        }

        /**
         * Returns the observe type property for this seq comp.
         */
        @Override
        public String getObserveType() {
            return InstConstants.FLAT_OBSERVE_TYPE;
        }

        /**
         * Returns the node title for this seq comp.
         */
        @Override public String getTitle() {
            return SP_TYPE.readableStr;
        }

    }

    public static class BaselineNight extends SeqRepeatSmartGcalObs {

        // Do not remove/rename!
        public static final SPComponentType SP_TYPE =
                SPComponentType.OBSERVER_BASELINENIGHT;

        public static final ISPNodeInitializer<ISPSeqComponent, BaselineNight> NI =
            new ComponentNodeInitializer<>(SP_TYPE, () -> new BaselineNight(), c -> new SeqRepeatSmartGcalObsCB.BasecalNight(c));

        public BaselineNight() {
            super(SP_TYPE);
        }

        /**
         * Returns the observe type property for this seq comp.
         */
        @Override
        public String getObserveType() {
            return InstConstants.FLAT_OBSERVE_TYPE;
        }

        /**
         * Returns the node title for this seq comp.
         */
        @Override public String getTitle() {
            return SP_TYPE.readableStr;
        }

    }

    public static class Flat extends SeqRepeatSmartGcalObs {

        // Do not remove/rename!
        public static final SPComponentType SP_TYPE =
                SPComponentType.OBSERVER_SMARTFLAT;

        public static final ISPNodeInitializer<ISPSeqComponent, Flat> NI =
            new ComponentNodeInitializer<>(SP_TYPE, () -> new Flat(), c -> new SeqRepeatSmartGcalObsCB.Flat(c));

        public Flat() {
            super(SP_TYPE);
        }

        /**
         * Returns the observe type property for this seq comp.
         */
        @Override
        public String getObserveType() {
            return InstConstants.FLAT_OBSERVE_TYPE;
        }

        /**
         * Returns the node title for this seq comp.
         */
        @Override public String getTitle() {
            return "Flat";
        }
    }

    public static class Arc extends SeqRepeatSmartGcalObs {

        // Do not remove/rename!
        public static final SPComponentType SP_TYPE =
                SPComponentType.OBSERVER_SMARTARC;

        public static final ISPNodeInitializer<ISPSeqComponent, Arc> NI =
            new ComponentNodeInitializer<>(SP_TYPE, () -> new Arc(), c -> new SeqRepeatSmartGcalObsCB.Arc(c));

        public Arc() {
            super(SP_TYPE);
        }

        /**
         * Returns the observe type property for this seq comp.
         */
        @Override
        public String getObserveType() {
            return InstConstants.ARC_OBSERVE_TYPE;
        }

        /**
         * Returns the node title for this seq comp.
         */
        @Override public String getTitle() {
            return "Arc";
        }
    }


    /**
     * Default constructor.
     */
    public SeqRepeatSmartGcalObs(SPComponentType type) {
        super(type, null);
    }

    /**
     * Clones this object.
     * @return
     */
    public Object clone() {
        SeqRepeatSmartGcalObs copy = (SeqRepeatSmartGcalObs)super.clone();
        return copy;
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
    }
}
