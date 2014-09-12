package edu.gemini.spModel.gemini.altair.blueprint;

import edu.gemini.spModel.pio.ParamSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SpAltairReaders {
    private SpAltairReaders() {}

    public static final Map<String, SpAltairReader> READER_MAP;

    static {
        Map<String, SpAltairReader> m = new HashMap<String, SpAltairReader>();

        m.put(SpAltairNone.PARAM_SET_NAME, new SpAltairReader() {
            @Override public SpAltair read(ParamSet paramSet) {
                return SpAltairNone.instance;
            }
        });
        m.put(SpAltairLgs.PARAM_SET_NAME, new SpAltairReader() {
            @Override public SpAltair read(ParamSet paramSet) {
                return new SpAltairLgs(paramSet);
            }
        });
        m.put(SpAltairNgs.PARAM_SET_NAME, new SpAltairReader() {
            @Override public SpAltair read(ParamSet paramSet) {
                return new SpAltairNgs(paramSet);
            }
        });

        READER_MAP = Collections.unmodifiableMap(m);
    }

    public static SpAltair read(ParamSet parent) {
        for (ParamSet paramSet : parent.getParamSets()) {
            SpAltairReader rdr = READER_MAP.get(paramSet.getName());
            if (rdr != null) return rdr.read(paramSet);
        }
        return SpAltairNone.instance;
    }
}
