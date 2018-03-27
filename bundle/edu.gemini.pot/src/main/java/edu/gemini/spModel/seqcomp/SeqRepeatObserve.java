// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatObserve.java 7987 2007-08-02 23:16:52Z swalker $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obsclass.ObsClass;

/**
 * A simple "Observe" iterator that does a science object for X times.
 */
public class SeqRepeatObserve extends SeqRepeat
        implements IObserveSeqComponent {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_OBSERVE;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatObserve> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeatObserve(), c -> new SeqRepeatObsCB(c));

    public static final String OBSERVE_TYPE = InstConstants.SCIENCE_OBSERVE_TYPE;

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SeqRepeatObserve() {
        super(SP_TYPE, ObsClass.SCIENCE);
        setStepCount(InstConstants.DEF_REPEAT_COUNT);
    }

    /**
     * Return the observe type property for this seq comp.
     */
    public String getObserveType() {
        return OBSERVE_TYPE;
    }
}
