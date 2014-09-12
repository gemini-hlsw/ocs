package edu.gemini.spModel.gemini.flamingos2.blueprint;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

import java.util.List;

public final class SpFlamingos2BlueprintLongslit extends SpFlamingos2BlueprintSpectroscopyBase {

    public static final String PARAM_SET_NAME = "flamingos2BlueprintLongslit";
    public static final String FPU_PARAM_NAME = "fpu";

    public final FPUnit fpu;

    public SpFlamingos2BlueprintLongslit(List<Filter> filters, Disperser disperser, FPUnit fpu) {
        super(filters, disperser);
        this.fpu = fpu;
    }

    public SpFlamingos2BlueprintLongslit(ParamSet paramSet) {
        super(paramSet);
        fpu = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, FPUnit.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("Flamingos2 Longslit %s %s %s", disperser.displayValue(), mkString(filters, "+"), fpu.displayValue());
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpFlamingos2BlueprintLongslit that = (SpFlamingos2BlueprintLongslit) o;
        return fpu == that.fpu;
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = res*31 + fpu.hashCode();
        return res;
    }
}
