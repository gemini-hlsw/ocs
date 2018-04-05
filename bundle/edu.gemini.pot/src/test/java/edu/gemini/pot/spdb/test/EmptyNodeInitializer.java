//
// $Id: EmptyNodeInitializer.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.pot.spdb.test;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.ISPDataObject;

/**
 * A node initializer that does nothing.
 */
public final class EmptyNodeInitializer<N extends ISPNode, D extends ISPDataObject> implements ISPNodeInitializer<N, D> {

    public SPComponentType getType() {
        return null;
    }

    public D createDataObject() {
        return null;
    }

    public void initNode(ISPFactory factory, N node) {
        // do nothing
    }
}
