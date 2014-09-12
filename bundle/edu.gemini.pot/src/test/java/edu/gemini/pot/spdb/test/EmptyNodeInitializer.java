//
// $Id: EmptyNodeInitializer.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.pot.spdb.test;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;

/**
 * A node initializer that does nothing.
 */
public final class EmptyNodeInitializer implements ISPNodeInitializer {
    public static final EmptyNodeInitializer INSTANCE = new EmptyNodeInitializer();

    public void initNode(ISPFactory factory, ISPNode node) {
        // do nothing
    }

    public void updateNode(ISPNode node) {
        // do nothing
    }
}
