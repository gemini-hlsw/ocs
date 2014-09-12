package edu.gemini.spModel.gemini.texes;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;


/**
 * Initializes <code>{@link edu.gemini.pot.sp.ISPObsComponent}</code> nodes of type Texes.
 */
public final class InstTexesNI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return InstTexes.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new InstTexesCB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new InstTexes();
    }
}
