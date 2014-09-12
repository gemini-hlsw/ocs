package edu.gemini.spModel.gemini.altair.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.io.ObjectStreamException;

public final class SpAltairNone extends SpAltair {
    public static final SpAltairNone instance = new SpAltairNone();

    private SpAltairNone() {}

    public static final String PARAM_SET_NAME = "altairNone";

    public SPComponentType instrumentType() { return InstAltair.SP_TYPE; }
    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        return factory.createParamSet(PARAM_SET_NAME);
    }

    public String shortName() { return ""; }
    public boolean useAo() { return false; }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
