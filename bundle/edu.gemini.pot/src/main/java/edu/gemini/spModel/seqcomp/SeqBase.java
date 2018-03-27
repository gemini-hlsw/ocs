// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqBase.java 37893 2011-10-06 15:25:48Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

/**
 * This is the data object for the "dummy" base sequence iterator.
 * It has no executable data, only children.
 */
public final class SeqBase extends AbstractDataObject implements Serializable, ISPSeqObject {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_BASE;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqBase> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqBase(), c -> new SeqBaseCB(c));

    // for serialization
    private static final long serialVersionUID = 2L;


    public static final String VERSION = "2012A-1";

    public SeqBase() {
        super(SP_TYPE);
        setVersion(VERSION);
    }

    @Override public Object clone() {
        SeqBase that = (SeqBase) super.clone();
        return that;
    }

    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);
        return paramSet;
    }

    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

    }

    @Override public int getStepCount() {
        return 1;
    }
}

