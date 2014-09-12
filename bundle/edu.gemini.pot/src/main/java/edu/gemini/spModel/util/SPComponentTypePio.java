package edu.gemini.spModel.util;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public class SPComponentTypePio {

    private static final String PARAM_BROAD_TYPE = "broadType";
    private static final String PARAM_NARROW_TYPE = "narrowType";
    private static final String PARAM_READABLE = "readable";

    public static ParamSet toParamSet(PioFactory factory, SPComponentType type, String name) {
        final ParamSet ps = factory.createParamSet(name);
        Pio.addParam(factory, ps, PARAM_BROAD_TYPE, type.broadType.value);
        Pio.addParam(factory, ps, PARAM_NARROW_TYPE, type.narrowType);
        Pio.addParam(factory, ps, PARAM_READABLE, type.readableStr);
        return ps;
    }

    public static SPComponentType fromParamSet(ParamSet ps) {
        final String bt = Pio.getValue(ps, PARAM_BROAD_TYPE);
        final String nt = Pio.getValue(ps, PARAM_NARROW_TYPE);
        final String rs = Pio.getValue(ps, PARAM_READABLE);
        return SPComponentType.getInstance(bt, nt, rs);
    }

}
