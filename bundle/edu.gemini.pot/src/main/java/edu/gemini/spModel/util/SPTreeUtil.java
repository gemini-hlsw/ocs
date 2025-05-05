package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class with operations on the science program tree model.
 */
public class SPTreeUtil {

    private static final List<ISPSeqComponent> _EMPTY_LIST = new ArrayList<>(0);

    /**
     * Return true if ans contains des (if des is a descendant of ans).
     *
     * @param anc check if this node is an ancestor
     * @param des check if this node is a descendant of node1
     */
    public static boolean nodeContainsNode(ISPNode anc, ISPNode des) {
        return anc != null && contains(anc, des);
    }

    private static boolean contains(ISPNode anc, ISPNode des) {
        if (des == null) return false;
        final ISPNode parent = des.getParent();
        return anc.equals(parent) || contains(anc, parent);
    }

    /**
     * Finds the node with the given SPNodeKey if it is nested inside
     * <code>parent</code>.
     *
     * @param parent root of the subtree to search
     * @param key node key of the node to find
     *
     * @return matching ISPNode with the given <code>key</code> if it
     * exists inside of <code>parent</code>; <code>null</code> otherwise
     */
    public static ISPNode findByKey(ISPNode parent, SPNodeKey key) {
        if (key.equals(parent.getNodeKey())) return parent;

        if (parent instanceof ISPContainerNode) {
            for (final ISPNode child : ((ISPContainerNode) parent).getChildren()) {
                final ISPNode res = findByKey(child, key);
                if (res != null) return res;
            }
        }

        return null;
    }

    /**
     * Find and return the nearest observation container corresponding to the given SP tree node.
     *
     * @param node the science program tree node
     * @return a group or program node, or null if not found in the node's hierarchy
     */
    public static ISPObservationContainer findObservationContainer(ISPNode node) {
        if (node == null) return null;
        if (node instanceof ISPObservationContainer) return (ISPObservationContainer) node;

        do {
            ISPNode parent = node.getParent();
            if (parent instanceof ISPObservationContainer) {
                return (ISPObservationContainer)parent;
            }
            node = parent;
        } while(node != null);

        return null;
    }

    /**
     * Find and return the TargetEnv tree node corresponding to the given observation
     * (That is the node whose data object is a TargetEnv).
     */
    public static ISPObsComponent findTargetEnvNode(ISPObservation o) {
        for (ISPObsComponent obsComp : o.getObsComponents()) {
            if (isTargetEnv(obsComp)) {
                return obsComp;
            }
        }
        return null;
    }

    /**
     * Find and return the Observing Conditions (SPSiteQuality) tree node corresponding to the given observation
     */
    public static ISPObsComponent findObsCondNode(ISPObservation o) {
        for (ISPObsComponent obsComp : o.getObsComponents()) {
            if (obsComp.getType().equals(SPSiteQuality.SP_TYPE)) {
                return obsComp;
            }
        }
        return null;
    }


    /**
     * Return true if the given component is a TargetEnv component.
     */
    public static boolean isTargetEnv(ISPObsComponent obsComp) {
        return TargetObsComp.SP_TYPE.equals(obsComp.getType());
    }

    /**
     * Return true if the given component is an instrument component.
     */
    public static boolean isInstrument(ISPObsComponent obsComp) {
        return SPComponentBroadType.INSTRUMENT.equals(obsComp.getType().broadType);
    }

    /**
     * Return true if the given component is an Altair component.
     */
    public static boolean isAltair(ISPObsComponent obsComp) {
        return InstAltair.SP_TYPE.equals(obsComp.getType());
    }


    /**
     * Find and return the instrument nodes corresponding to the given observation.
     * The return value will be a list containing the instrument (if found) and an
     * Altair component (if found).
     */
    public static List<ISPObsComponent> findInstruments(ISPObservation o) {
        List<ISPObsComponent> result = new ArrayList<>();
        if (o != null) {
            Iterator<ISPObsComponent> iter = o.getObsComponents().iterator();
            while (iter.hasNext()) {
                ISPObsComponent obsComp = iter.next();
                if (isInstrument(obsComp)) {
                    result.add(obsComp);
                }
            }
            // do a second loop to make sure the Altair component comes after the instrument
            iter = o.getObsComponents().iterator();
            while (iter.hasNext()) {
                ISPObsComponent obsComp = iter.next();
                if (isAltair(obsComp)) {
                    result.add(obsComp);
                }
            }
        }
        return result;
    }

    /**
     * Find and return the instrument node corresponding to the given observation.
     */
    public static ISPObsComponent findInstrument(ISPObservation o) {
        if (o != null) {
            for (ISPObsComponent obsComp : o.getObsComponents()) {
                if (isInstrument(obsComp)) {
                    return obsComp;
                }
            }
        }
        return null;
    }

    /**
     * If there is an ISPObsComponent of the given type in the given observation,
     * return it, otherwise return null.
     */
    public static ISPObsComponent findObsComponent(ISPObservation o, SPComponentType type) {
        if (o == null || type == null) return null;

        for (ISPObsComponent obsComp : o.getObsComponents()) {
            if (obsComp.getType().equals(type))
                return obsComp;
        }
        return null;
    }

    /**
     * If there is an ISPObsComponent of the given broad type in the given observation,
     * return it, otherwise return null.
     */
    public static ISPObsComponent findObsComponentByBroadType(ISPObservation o, SPComponentBroadType broadType) {
        if (o == null || broadType == null) return null;

        for (ISPObsComponent obsComp : o.getObsComponents()) {
            if (obsComp.getType().broadType.equals(broadType))
                return obsComp;
        }
        return null;
    }


    /**
     * If there is an ISPObsComponent of the given narrow type in the given observation,
     * return it, otherwise return null.
     */
    public static ISPObsComponent findObsComponentByNarrowType(ISPObservation o, String narrowType) {
        if (o == null || narrowType == null) return null;

        for (ISPObsComponent obsComp : o.getObsComponents()) {
            if (obsComp.getType().narrowType.equals(narrowType))
                return obsComp;
        }
        return null;
    }


    /**
     * If there is an ISPSeqComponent of the given type in the given observation,
     * return it, otherwise return null.
     */
    public static ISPSeqComponent findSeqComponent(ISPObservation o, SPComponentType type) {
        if (o == null || type == null) return null;

        ISPSeqComponent sc = o.getSeqComponent();
        if (sc != null && sc.getType().equals(type))
            return sc;
        return findSeqComponent(sc, type);
    }

    /**
     * If there is an ISPSeqComponent of the given type under the given sequence node,
     * return it, otherwise return null.
     */
    public static ISPSeqComponent findSeqComponent(ISPSeqComponent sc, SPComponentType type) {
        if (sc == null || type == null) return null;

        List<ISPSeqComponent> l = DBSequenceNodeService.findSeqComponentsByType(sc, type, true);
        if (l != null && l.size() != 0) {
            return l.get(0);
        }

        return null;
    }

    /**
     * Return a list containing all of the sequence nodes with the given
     * type in the given observation.
     *
     * @param o the observation node
     * @param type the node type
     * @return a list of ISPSeqComponent objects with the given type
     */
    public static List<ISPSeqComponent> findSeqComponents(ISPObservation o, SPComponentType type) {
        return (o == null || type == null) ? _EMPTY_LIST : findSeqComponents(o.getSeqComponent(), type);
    }

    /**
     * Return a list of all of the sequence components with the given type,
     * starting the search at the given sequence component.
     */
    public static List<ISPSeqComponent> findSeqComponents(ISPSeqComponent sc, SPComponentType type) {
        if (sc == null || type == null) return _EMPTY_LIST;
        return DBSequenceNodeService.findSeqComponentsByType(sc, type, false);
    }

    /**
     * If there is an ISPSeqComponent with the given narrow type in the given sequence,
     * return it, otherwise return null.
     *
     * @param sc the starting sequence component
     * @param narrowType string that should match a a SPComponentType.narrowType value
     * @param noObserve if true, search only as far as the first observe node
     */
    public static ISPSeqComponent findSeqComponentByNarrowType(ISPSeqComponent sc, String narrowType,
                                                               boolean noObserve) {
        if (sc == null || narrowType == null) return null;

        List<ISPSeqComponent> l = DBSequenceNodeService.findSeqComponentsByNarrowType(sc, narrowType, false, noObserve);
        if (l != null && !l.isEmpty()) {
             return l.get(0);
        }

        return null;
    }

    /**
     * Returns the {@link edu.gemini.spModel.obsrecord.ObsExecRecord} for the given observation from the {
     * @link ObsLogDataObject} if found, otherwise null.
     */
    public static ObsExecRecord getObsRecord(ISPObservation obs)  {
        final ISPObsExecLog log = obs.getObsExecLog();
        return (log == null) ? null : ((ObsExecLog) log.getDataObject()).getRecord();
    }
}
