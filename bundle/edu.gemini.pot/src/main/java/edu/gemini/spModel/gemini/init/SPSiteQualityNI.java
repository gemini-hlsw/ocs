// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPSiteQualityNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQualityCB;
import java.util.logging.Logger;
import java.util.logging.Level;




/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type
 * SiteQuality.
 */
public class SPSiteQualityNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SPSiteQualityNI.class.getName());

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)
             {
        //LOG.log(Level.INFO, "Initing Observing Coditions Node");

        ISPObsComponent castNode = (ISPObsComponent) node;
        if (!castNode.getType().equals(SPSiteQuality.SP_TYPE)) {
            throw new InternalError();
        }

        // The data is stored in an SPSiteQuality object set as the
        // data object of this ObsComponent.
        try {
            node.setDataObject(new SPSiteQuality());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object for node.", ex);
        }

        updateNode(node);
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
        // Set the configuration builder
        node.putClientData(IConfigBuilder.USER_OBJ_KEY,
                           new SPSiteQualityCB((ISPObsComponent) node));
    }
}
