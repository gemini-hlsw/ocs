//
// $Id: ObsLogNI.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obslog;

import edu.gemini.pot.sp.*;
import java.util.logging.Logger;
import java.util.logging.Level;



/**
 * Initializes {@link edu.gemini.pot.sp.ISPObsExecLog} nodes.
 */
public class ObsExecLogNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(ObsExecLogNI.class.getName());

    public void initNode(ISPFactory factory, ISPNode node) {
        if (!(node instanceof ISPObsExecLog)) {
            throw new RuntimeException(String.format("Initializing a '%s' in ObsLogNI", node.getClass().getName()));
        }

        try {
            node.setDataObject(new ObsExecLog());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object for node.", ex);
        }
    }

    public void updateNode(ISPNode node)  {
    }
}
