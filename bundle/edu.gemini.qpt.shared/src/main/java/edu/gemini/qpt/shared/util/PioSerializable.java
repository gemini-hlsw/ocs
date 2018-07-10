package edu.gemini.qpt.shared.util;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

public interface PioSerializable {

    ParamSet getParamSet(PioFactory factory, String name);
    
}
