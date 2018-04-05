package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;

/**
 * Data object for GPI offset iterator.
 * See OT-103.
 * @deprecated
 */
@Deprecated
public final class SeqRepeatGpiOffset extends SeqRepeatOffsetBase<GpiOffsetPos> {

    private static final String VERSION = "2012A-1";

    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_GPIOFFSET;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqRepeatGpiOffset> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqRepeatGpiOffset(), c -> new SeqRepeatGpiOffsetCB(c));

    public SeqRepeatGpiOffset() {
        super(SP_TYPE, GpiOffsetPos.FACTORY);
        setVersion(VERSION);
    }
}
