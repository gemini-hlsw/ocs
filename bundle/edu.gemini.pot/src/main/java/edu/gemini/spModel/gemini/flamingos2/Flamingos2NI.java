/**
 * $Id: Flamingos2NI.java 39256 2011-11-22 17:42:49Z swalker $
 */

package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;


public final class Flamingos2NI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return Flamingos2.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new Flamingos2CB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new Flamingos2();
    }
}
