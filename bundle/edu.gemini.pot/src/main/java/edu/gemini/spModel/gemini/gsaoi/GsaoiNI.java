//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import java.util.Collection;
import java.util.Collections;

/**
 * Initializes {@link Gsaoi} nodes.
 */
public final class GsaoiNI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return Gsaoi.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new GsaoiCB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new Gsaoi();
    }

    @Override public Collection<ISPDataObject> createFriends() {
        return Collections.<ISPDataObject>singletonList(new Gems());
    }
}
