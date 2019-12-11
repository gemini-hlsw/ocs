// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatDarkObs.java 38365 2011-11-03 20:37:20Z swalker $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obsclass.ObsClass;


/**
 * A simple "Dark" iterator that does a dark observe for X times.
 */
public class SeqRepeatDarkObs extends SeqRepeatCoaddExp
        implements ICoaddExpSeqComponent {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.OBSERVER_DARK;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatDarkObs> NI =
        new ComponentNodeInitializer<>(SP_TYPE, SeqRepeatDarkObs::new, SeqRepeatCoaddExpCB::new);

    public static final String OBSERVE_TYPE = InstConstants.DARK_OBSERVE_TYPE;

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SeqRepeatDarkObs() {
        super(SP_TYPE, ObsClass.DAY_CAL);
    }

    /**
     * Return the observe type property for this seq comp.
     */
    public String getObserveType() {
        return OBSERVE_TYPE;
    }
}
