package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPObsComponent;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;


/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type Phoenix.
 */
public final class InstPhoenixNI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return InstPhoenix.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new InstPhoenixCB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new InstPhoenix();
    }
}
