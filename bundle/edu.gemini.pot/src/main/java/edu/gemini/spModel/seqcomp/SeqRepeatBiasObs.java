// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatBiasObs.java 38365 2011-11-03 20:37:20Z swalker $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;

import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obsclass.ObsClass;

/**
 * A simple "Bias" iterator that does a bias observe for X times.
 */
public class SeqRepeatBiasObs extends SeqRepeatCoaddExp
        implements ICoaddExpSeqComponent {

    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_BIAS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatBiasObs> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeatBiasObs(), c -> new SeqRepeatBiasObsCB(c));

    public static final String OBSERVE_TYPE = InstConstants.BIAS_OBSERVE_TYPE;

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SeqRepeatBiasObs() {
        super(SP_TYPE, ObsClass.DAY_CAL);
    }

    /**
     * Return the observe type property for this seq comp.
     */
    public String getObserveType() {
        return OBSERVE_TYPE;
    }
}
