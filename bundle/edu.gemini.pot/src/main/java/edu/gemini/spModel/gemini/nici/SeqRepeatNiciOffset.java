//
// $
//

package edu.gemini.spModel.gemini.nici;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortSensitiveComponent;

/**
 * Data object for NICI offset iterator.
 */
public final class SeqRepeatNiciOffset extends SeqRepeatOffsetBase<NiciOffsetPos> implements IssPortSensitiveComponent {

    private static final String VERSION = "2009B-1";

    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_NICIOFFSET;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatNiciOffset> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeatNiciOffset(), c -> new SeqRepeatNiciOffsetCB(c));

    public SeqRepeatNiciOffset() {
        super(SP_TYPE, NiciOffsetPos.FACTORY);
        setVersion(VERSION);
    }

    public void handleIssPortUpdate(ISPNode providerNode, IssPort oldValue, IssPort newValue) {
        for (NiciOffsetPos pos : getPosList()) {
            if (pos.isFpmwTracking()) {
                double d = pos.getOffsetDistance();
                pos.setOffsetDistance(d, newValue);
            }
        }
    }
}
