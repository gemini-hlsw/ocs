// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatFlatObs.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obsclass.ObsClass;

/**
 * A simple "Flat" iterator that does a flat observe for X times
 * with coadds and exposure time.
 */
public class SeqRepeatFlatObs extends SeqRepeatCoaddExp
        implements ICoaddExpSeqComponent {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.OBSERVER_FLAT;

    public static final String OBSERVE_TYPE = InstConstants.DARK_OBSERVE_TYPE;

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SeqRepeatFlatObs() {
        super(SP_TYPE, ObsClass.PARTNER_CAL);
    }

    /**
     * Return the observe type property for this seq comp.
     */
    public String getObserveType() {
        return InstConstants.FLAT_OBSERVE_TYPE;
    }
}
