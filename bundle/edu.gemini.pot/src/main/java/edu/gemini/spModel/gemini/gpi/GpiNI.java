package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;


public final class GpiNI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return Gpi.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new GpiCB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new Gpi();
    }
}
