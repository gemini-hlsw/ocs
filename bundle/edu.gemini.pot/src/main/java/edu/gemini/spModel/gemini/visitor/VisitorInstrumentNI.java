package edu.gemini.spModel.gemini.visitor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;

/**
 * Node initializer for the Visitor Instrument
 */
public class VisitorInstrumentNI extends DefaultInstNodeInitializer {
    @Override
    public SPComponentType getType() {
        return VisitorInstrument.SP_TYPE;
    }

    @Override
    protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new VisitorInstrumentCB(node);
    }

    @Override
    public SPInstObsComp createDataObject() {
        return new VisitorInstrument();
    }
}
