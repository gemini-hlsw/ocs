package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairReaders;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public abstract class SpGmosNBlueprintBase extends SpBlueprint {
    public SPComponentType instrumentType() { return InstGmosNorth.SP_TYPE; }

    public final SpAltair altair;

    protected SpGmosNBlueprintBase(SpAltair altair) {
        this.altair = altair;
    }

    protected SpGmosNBlueprintBase(ParamSet paramSet) {
        this.altair = SpAltairReaders.read(paramSet);
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(paramSetName());
        paramSet.addParamSet(altair.toParamSet(factory));
        return paramSet;
    }

    protected String toString(String name, String suffix) {
        final String a = altair.shortName();

        return ("".equals(a)) ?
            String.format("%s %s", name, suffix) :
            String.format("%s %s %s", name, a, suffix);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SpGmosNBlueprintBase that = (SpGmosNBlueprintBase) o;
        return altair.equals(that.altair);
    }

    @Override
    public int hashCode() {
        // override hashCode since subclasses will be calling super.equals it'll
        // be easier to call super.hashCode always too
        int res = getClass().hashCode();
        res = 37*res + altair.hashCode();
        return res;
    }
}
