package edu.gemini.spModel.gemini.inst;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.ISPDataObject;


import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple node initializer for instruments that simply set a data object
 * and config builder.
 */
public abstract class DefaultInstNodeInitializer implements InstNodeInitializer {
    private static final Logger LOG = Logger.getLogger(DefaultInstNodeInitializer.class.getName());

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link edu.gemini.pot.sp.ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)  {
        ISPObsComponent castNode = (ISPObsComponent) node;
        if (!castNode.getType().equals(getType())) throw new InternalError();

        try {
            node.setDataObject(createDataObject());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to set data object", ex);
        }

        // Set the configuration builder
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
        node.putClientData(IConfigBuilder.USER_OBJ_KEY, createConfigBuilder((ISPObsComponent) node));
    }

    /**
     * Create the config builder that is appropriate for the instrument.
     * @param node node to assign to the config builder
     */
    protected abstract IConfigBuilder createConfigBuilder(ISPObsComponent node);

    @Override public abstract SPComponentType getType();

    @Override public Collection<ISPDataObject> createFriends() { return Collections.emptyList(); }
}
