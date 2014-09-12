package edu.gemini.spModel.gemini.inst;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import java.util.Collection;

/**
 * A node initializer specialization for instruments.
 */
public interface InstNodeInitializer extends ISPNodeInitializer {
    public SPComponentType getType();
    public SPInstObsComp createDataObject();
    public Collection<ISPDataObject> createFriends();
}
