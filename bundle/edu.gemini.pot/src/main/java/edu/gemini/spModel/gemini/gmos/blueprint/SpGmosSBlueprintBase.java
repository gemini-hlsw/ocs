package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public abstract class SpGmosSBlueprintBase extends SpBlueprint {

    public SPComponentType instrumentType() { return InstGmosSouth.SP_TYPE; }

    public ParamSet toParamSet(PioFactory factory) {
        return factory.createParamSet(paramSetName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        // override hashCode since subclasses will be calling super.equals it'll
        // be easier to call super.hashCode always too
        return getClass().hashCode();
    }
}
