package edu.gemini.spModel.gemini.graces.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public final class SpGracesBlueprint extends SpBlueprint {

    public static final String PARAM_SET_NAME   = "GracesBlueprint";
    public static final String PARAM_READ_MODE  = "readMode";
    public static final String PARAM_FIBER_MODE = "fiberMode";

    // N.B. Because this maps to a visitor instrument there is no InstGraces and these parameters
    // are used only for blueprint expansion. So we define them here and we're fine.
    public enum ReadMode  { FAST, NORMAL, SLOW }
    public enum FiberMode { ONE_FIBER, TWO_FIBER }

    private ReadMode  readMode;
    private FiberMode fiberMode;

    public SpGracesBlueprint(ReadMode readMode, FiberMode fiberMode) {
        this.readMode  = readMode;
        this.fiberMode = fiberMode;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public FiberMode getFiberMode() {
        return fiberMode;
    }

    // found via reflection :/  must exist
    public SpGracesBlueprint(ParamSet paramSet) {
        readMode  = Pio.getEnumValue(paramSet, PARAM_READ_MODE, ReadMode.FAST);
        fiberMode = Pio.getEnumValue(paramSet, PARAM_FIBER_MODE, FiberMode.ONE_FIBER);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return VisitorInstrument.SP_TYPE; }

    @Override public String toString() {
        return String.format("GRACES(%s, %s)", readMode, fiberMode);
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet ps = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, ps, PARAM_READ_MODE,  readMode);
        Pio.addEnumParam(factory, ps, PARAM_FIBER_MODE, fiberMode);
        return ps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpGracesBlueprint that = (SpGracesBlueprint) o;
        if (fiberMode != that.fiberMode) return false;
        if (readMode != that.readMode) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = readMode.hashCode();
        result = 31 * result + fiberMode.hashCode();
        return result;
    }

 }
