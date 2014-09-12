package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

/**
 * Blueprint base interface.
 */
public abstract class SpBlueprint implements Serializable {
    public abstract SPComponentType instrumentType();
    public abstract String paramSetName();
    public abstract ParamSet toParamSet(PioFactory factory);
}
