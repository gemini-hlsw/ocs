//
// $Id: ObsLogNI.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obslog;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsQaLog;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Initializes {@link edu.gemini.pot.sp.ISPObsQaLog} nodes.
 */
public class ObsQaLogNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(ObsQaLogNI.class.getName());

    public void initNode(ISPFactory factory, ISPNode node) {
        if (!(node instanceof ISPObsQaLog)) {
            throw new RuntimeException(String.format("Initializing a '%s' in ObsLogQaNI", node.getClass().getName()));
        }

        try {
            node.setDataObject(new ObsQaLog());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object for node.", ex);
        }
    }

    public void updateNode(ISPNode node)  {
    }
}
