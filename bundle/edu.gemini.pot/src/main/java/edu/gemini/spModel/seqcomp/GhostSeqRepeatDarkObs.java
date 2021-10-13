package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;

final public class GhostSeqRepeatDarkObs extends GhostSeqRepeatExp {
    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_GHOST_DARK;

    public static final ISPNodeInitializer<ISPSeqComponent, GhostSeqRepeatDarkObs> NI =
            new ComponentNodeInitializer<>(SP_TYPE, GhostSeqRepeatDarkObs::new, GhostSeqRepeatExpCB::new);

    public static final String OBSERVE_TYPE = InstConstants.DARK_OBSERVE_TYPE;

    private static final long serialVersionUID = 1L;

    public GhostSeqRepeatDarkObs() {
        super(SP_TYPE, ObsClass.DAY_CAL);
    }

    @Override
    public String getObserveType() {
        return OBSERVE_TYPE;
    }
}
