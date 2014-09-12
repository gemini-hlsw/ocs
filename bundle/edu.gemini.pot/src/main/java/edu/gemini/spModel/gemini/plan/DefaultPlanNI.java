//
// Gemini Observatory/AURA
// $Id: DefaultPlanNI.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.gemini.plan;

import edu.gemini.pot.sp.*;


public class DefaultPlanNI implements ISPNodeInitializer {

    public void initNode(ISPFactory factory, ISPNode node) {
        node.setDataObject(new NightlyRecord());
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
    }
}
