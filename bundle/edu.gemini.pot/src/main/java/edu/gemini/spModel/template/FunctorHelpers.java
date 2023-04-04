package edu.gemini.spModel.template;

import edu.gemini.pot.sp.*;
import java.util.logging.Logger;

class FunctorHelpers {

    private static final Logger LOGGER = Logger.getLogger(FunctorHelpers.class.getName());

    /** Adds the specified obsComp if one of that type doesn't already exist. */
    static void addIfNotPresent(ISPObservation obs, ISPObsComponent comp) throws SPTreeStateException, SPNodeNotLocalException {
        if (comp != null) {
            if (!hasComponentByType(obs, comp.getType())) {
                obs.addObsComponent(comp);
            } else {
                LOGGER.info("A component of type " + comp.getType() + " already exists in " + obs.getObservationID());
            }
        }
    }

    /** Returns true if a component of the given type exists. */
    static boolean hasComponentByType(ISPObservation obs, SPComponentType type)  {
        return findComponentByType(obs, type) != null;
    }

    /** Returns the first component of the given type, or null. */
    static ISPObsComponent findComponentByType(ISPObservation obs, SPComponentType type)  {
        for (ISPObsComponent comp : obs.getObsComponents())
            if (comp.getType().equals(type))
                return comp;
        return null;
    }

    /** Returns the first instrument component, or null. */
    static ISPObsComponent findInstrument(ISPObservation obs)  {
        for (ISPObsComponent comp : obs.getObsComponents())
            if (comp.getType().broadType.equals(SPComponentBroadType.INSTRUMENT))
                return comp;
        return null;
    }

    /** Returns the node with the given key inside the specified node, recursively, if any. */
    static ISPNode lookupNode(SPNodeKey key, ISPNode node)  {
        if (node.getNodeKey().equals(key)) {
            return node;
        } else if (node instanceof ISPContainerNode) {
            final ISPContainerNode container = (ISPContainerNode) node;
            for (ISPNode child : container.getChildren()) {
                final ISPNode tryChild = lookupNode(key, child);
                if (tryChild != null)
                    return tryChild;
            }
        }
        return null;
    }

}
