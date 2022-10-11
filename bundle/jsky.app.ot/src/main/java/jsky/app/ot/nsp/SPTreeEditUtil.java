// Copyright 1999-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPTreeEditUtil.java 46768 2012-07-16 18:58:53Z rnorris $
//

package jsky.app.ot.nsp;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.validator.NodeCardinality;
import edu.gemini.pot.sp.validator.NodeType;
import edu.gemini.pot.sp.validator.Validator$;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.seqcomp.SeqBase;
import edu.gemini.spModel.util.DBCopyService;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.SPTree;
import jsky.app.ot.viewer.UIInfoXML;
import jsky.util.gui.DialogUtil;
import scala.Option;

import javax.swing.*;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Utility class with operations on the science program tree model.
 */
public class SPTreeEditUtil {
    private static final Logger LOG = Logger.getLogger(SPTreeEditUtil.class.getName());

    // Used to mark the initialized state of the dialog asking the user to confirm an operation
     private static final int USER_CONFIRMATION_INITIAL_STATE = -37;

    /**
     * Set the ISPFactory object used to create new science program components
     */
    // TODO: pass this in; make class non-static
    public static ISPFactory getFactory() {
        return SPDB.get().getFactory();
    }


    /**
     * Remove the given node from the science program tree and return true if all okay.
     *
     * @param node the science program tree node to remove
     */
    public static boolean removeNode(ISPNode node) {

        ISPNode parent = node.getParent();
        if (parent == null) {
            return false;
        }

        if (parent instanceof ISPConflictFolder) {
            final ISPConflictFolder cf = (ISPConflictFolder) parent;
            cf.removeChild(node);
            if (cf.getChildren().size() == 0) cf.getParent().removeConflictFolder();
            return true;
        } else if (node instanceof ISPSeqComponent) {
            if (parent instanceof ISPSeqComponent) {
                ((ISPSeqComponent)parent).removeSeqComponent((ISPSeqComponent)node);
                return true;
            }
            if (parent instanceof ISPObservation) {
                ((ISPObservation)parent).removeSeqComponent();
                return true;
            }
        } else if (node instanceof ISPObservation) {
            ((ISPObservationContainer)parent).removeObservation((ISPObservation) node);
            return true;
        } else if (node instanceof ISPObsComponent) {
            ((ISPObsComponentContainer)parent).removeObsComponent((ISPObsComponent) node);
            return true;
        } else if (node instanceof ISPGroup) {
            ((ISPGroupContainer)parent).removeGroup((ISPGroup) node);
            return true;
        } else if (node instanceof ISPTemplateParameters) {
            ((ISPTemplateGroup) parent).removeTemplateParameters((ISPTemplateParameters) node);
            return true;
        } else if (node instanceof  ISPTemplateGroup) {
            ((ISPTemplateFolder) parent).removeTemplateGroup((ISPTemplateGroup) node);
            return true;
        } else if (node instanceof ISPConflictFolder) {
            ((ISPContainerNode) parent).removeConflictFolder();
        }

        return false;
    }


    /**
     * Add the given science program node to the given parent node and return
     * true if successful.
     *
     * @param prog   the science program tree root
     * @param parent the parent node
     * @param node   the node to add to the parent
     */
    public static boolean addNode(ISPProgram prog, ISPNode parent,
                                  ISPNode node)
            throws SPNodeNotLocalException, SPTreeStateException {

        if (node instanceof ISPProgram) {
            // special case: replace the contents of the existing program
            return copyProg(prog, (ISPProgram)node);
        }

        if (node instanceof ISPSeqComponent) {
            ISPSeqComponent sc = (ISPSeqComponent)node;
            if (parent instanceof ISPSeqComponent) {
                ((ISPSeqComponent)parent).addSeqComponent(sc);
                return true;
            }
            if (parent instanceof ISPObsComponent) {
                parent = parent.getParent();
            }
            if (parent instanceof ISPObservation && sc.getType().equals(SeqBase.SP_TYPE)) {
                ((ISPObservation)parent).setSeqComponent(sc);
                return true;
            }
            return false;
        } else if (node instanceof ISPObservation &&
                parent instanceof ISPObservationContainer) {
            ((ISPObservationContainer)parent).addObservation((ISPObservation)node);
            return true;
        }
        if (node instanceof ISPObsComponent &&
                parent instanceof ISPObsComponentContainer) {
            ((ISPObsComponentContainer)parent).addObsComponent((ISPObsComponent)node);
            ImOption.apply(parent.getContextObservation()).foreach(o -> AsterismEditUtil.matchAsterismToInstrument(o));
            return true;
        }
        if (node instanceof ISPGroup && parent instanceof ISPGroupContainer) {
            ((ISPGroupContainer)parent).addGroup((ISPGroup)node);
            return true;
        }
        if (node instanceof ISPTemplateGroup && parent instanceof ISPTemplateFolder) {
            ((ISPTemplateFolder)parent).addTemplateGroup((ISPTemplateGroup)node);
            return true;
        }

        // If we got here, the child node could not be inserted: try to guess the correct parent node
        if (node instanceof ISPObservation) {
            ISPObservation obs = parent.getContextObservation();
            if (obs != null && !obs.equals(node)) {
                return insertObservationBefore((ISPObservation)node, obs);
            }
            prog.addObservation(0, (ISPObservation)node);
            return true;

        }
        if (node instanceof ISPGroup) {
            prog.addGroup(0, (ISPGroup)node);
            return true;
        }

        if (node instanceof ISPTemplateParameters && parent instanceof ISPTemplateGroup) {
            ((ISPTemplateGroup) parent).addTemplateParameters((ISPTemplateParameters) node);
            return true;
        }

        return false;
    }

    /**
     * Insert the given observation in the program tree just before the given target observation
     * and return true if successful.
     *
     * @param obs    the observation node to insert
     * @param target insert obs before this node
     */
    public static boolean insertObservationBefore(ISPObservation obs,
                                                  ISPObservation target)
            throws SPNodeNotLocalException, SPTreeStateException {

        if (obs.equals(target)) {
            return false;
        }

        final ISPObservationContainer parent = (ISPObservationContainer)target.getParent();
        final List<ISPObservation> l = parent.getObservations();
        int i = l.indexOf(target);
        if (i == -1) {
            parent.addObservation(obs);
        } else {
            parent.addObservation(i, obs);
        }
        return true;
    }

    private static boolean wouldCreateCycleIfAdded(ISPProgram prog, ISPNode node, ISPNode parent) {
        // Sanity check
        // TODO: when would parent be null?
        if (prog == null || node == null || parent == null) {
            LOG.fine(String.format("bogus call: doHierarchyCheckForAddition(%s, %s, %s)", prog, node, parent));
            return true;
        }

        // Hierarchy check
        for (ISPNode n = parent; n != null; n = n.getParent()) {
            if (n == node) {
                LOG.fine("Hierarchy failure");
                return true;
            }
        }

        return false;
    }

    private static boolean wouldCreateCycleIfAdded(ISPProgram prog, ISPNode[] nodes, ISPNode parent) {
        for (ISPNode n : nodes) {
            if (wouldCreateCycleIfAdded(prog, n, parent)) return true;
        }
        return false;
    }

    private static boolean isEditableAddLocation(ISPProgram prog, ISPNode parent) {
                // Is the program editable?
        if (!OTOptions.isProgramEditable(prog)) {
            LOG.fine("Program is not editable");
            return false;
        }

        // Is the target obs editable?
        final ISPObservation o = parent.getContextObservation();
        if (o != null && !OTOptions.isObservationEditable(o)) {
            LOG.fine("Obs is not editable");
            return false;
        }
        return true;
    }

    private static boolean isValidUpdateLocation(ISPProgram prog, ISPNode[] node, ISPNode parent) {
        return !wouldCreateCycleIfAdded(prog, node, parent) && isEditableAddLocation(prog, parent);
    }

    public static boolean isOkayToAdd(ISPProgram prog, ISPNode[] nodes, ISPNode parent, ISPNode context) {
        if (!isValidUpdateLocation(prog, nodes, parent)) return false;

        // Cardinality check: does it make sense to add node to parent?
        if (!Validator$.MODULE$.canAdd(prog, nodes, parent, Option.apply(context))) {
            LOG.fine("Cardinality failure");
            return false;
        }

        return true;
    }

    public static boolean isOkayToAdd(ISPProgram prog, ISPNode node, ISPNode parent, ISPNode context) {
        return isOkayToAdd(prog, new ISPNode[] { node }, parent, context);
    }

    /**
     *
     * Return true if it is allowed to delete the given node from the tree.
     * It is allowed if the ot -del option was specified.
     * If there is no -del option:
     * Removal of observations with an Observing Log component that has data is not allowed.
     * Removal of an Observing Log component from an Observation is not allowed if the
     * OL component has datasets.
     *
     * @param node the node to be deleted
     * @return true if the node may be deleted
     */
    public static boolean isOkayToDelete(IDBDatabaseService db, ISPNode node) {
        return IsOkayToDeleteFunctor.check(db, node);
    }

    /**
     *
     * Return true if it is allowed to move the given node within the tree.
     * This returns true, except for ObsLog components, in which case it returns
     * true only if there are no datasets. If there are datasets, the component
     * should not be moved away from its observation.
     *
     * @param node the node to be moved
     * @return true if the node may be moved
     */
    public static boolean isOkayToMove(IDBDatabaseService db, ISPNode node, ISPNode parent) {
        if (node instanceof ISPObsComponent) {
            // If parent is a sibling, its just a change in order, which is ok
            if (!node.getParent().equals(parent.getParent())) {
                return isOkayToDelete(db, node);
            }
        }
        return true;
    }

    /**
     * Copy the contents of the given source program to the
     * given target program. If the target program is not empty,
     * get user confirmation to delete the contents first.
     *
     * @param targetProg the target of the copy
     * @param sourceProg the source of the copy
     */
    public static boolean copyProg(ISPProgram targetProg, ISPProgram sourceProg) {

        // check if overwrite is needed
        List<ISPNode> childList = targetProg.getChildren();
        if (childList.size() != 0) {
            int ans = DialogUtil.confirm(
                    "Do you want to overwrite the contents of this program?");
            if (ans != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        try {
            DBCopyService.copy(getFactory(), targetProg, sourceProg);
        } catch (SPException ex) {
            LOG.log(Level.WARNING, "Could not copy prog", ex);
            return false;
        }

        return true;
    }


    /**
     * Return a deep copy of the given science program node, with the progID field
     * set for the given program node. This should be used when copying a node from
     * one SP tree to another.
     *
     * @param prog the science program tree root
     * @param node the science program tree node to copy
     */
    public static ISPNode copyNode(ISPProgram prog, ISPNode node)
            throws SPUnknownIDException {

        // Copying the program is handled in copyProg() above
        if (node instanceof ISPProgram) {
            return node;
        }

        if (node instanceof ISPContainerNode) {
            // use a functor, since there could be many subnodes
            try {
                return DBCopyService.copy(getFactory(), prog, node);
            } catch (SPException ex) {
                LOG.log(Level.WARNING, "Could not copy node", ex);
                return node; // ??
            }
        } else {
            // if its just one node, do it here
            if (node instanceof ISPObsComponent) {
                return getFactory().createObsComponentCopy(prog, (ISPObsComponent) node,
                        false);
            } else {
                //throw new RuntimeException("Unexpected node type: " + node.getClass().toString());
                return node;
            }
        }
    }

    /**
     * Return the index of the next visible SP tree node in the given direction
     * in the list. Basically, we just want to add or subtract 1 from the index,
     * but we need to skip over invisible nodes.
     *
     * @param l     a list of nodes
     * @param index an index in the list
     * @param up    true if the node should be moved up in the tree, otherwise down
     */
    private static <T extends ISPNode> int _getNextVisibleIndex(List<T> l, int index, boolean up) {
        int incr = (up ? -1 : 1);
        int i = index + incr;
        int n = l.size();
        if (i >= n) {
            return n;
        }
        if (i <= 0) {
            return 0;
        }
        while (true) {
            ISPNode nextNode = l.get(i);
            UIInfo uiInfo = UIInfoXML.getUIInfo(nextNode);
            if (uiInfo != null && uiInfo.isVisible()) {
                break;
            }
            int j = i + incr;
            if (j < 0 || j >= n) {
                break;
            }
            i = j;
        }
        return i;
    }

    /**
     * Move the given node up or down in the list, if allowed, and return true if it was done.
     *
     * @param l     a list of nodes
     * @param node  the node to move
     * @param up    true if the node should be moved up in the tree, otherwise down
     * @param toEnd true if the node should be moved all the way up or down to the start or end of the list
     */
    private static <T extends ISPNode> boolean _moveNode(List<T> l, T node, boolean up,
                                     boolean toEnd) {
        int n = l.size();
        if (n <= 1
                || (up && l.get(0).equals(node))
                || (!up && l.get(n - 1).equals(node))
                || !l.contains(node)) {
            return false;
        }

        if (toEnd) {
            l.remove(node);
            if (up) {
                l.add(0, node);
            } else {
                l.add(node);
            }
        } else {
            int i = l.indexOf(node);
            l.remove(node);
            l.add(_getNextVisibleIndex(l, i, up), node);
        }
        return true;
    }

    /**
     * Move the given node up or down in the tree, if allowed, and return true if it was done.
     *
     * @param node  the node to move
     * @param up    true if the node should be moved up in the tree, otherwise down
     * @param toEnd true if the node should be moved all the way up or down to the start or end of the list
     */
    public static boolean moveNode(ISPNode node, boolean up, boolean toEnd)
            throws SPNodeNotLocalException, SPTreeStateException {

        if (node instanceof ISPProgram) {
            return false;
        }

        ISPNode parent = node.getParent();

        if (node instanceof ISPObservation) {
            if (parent instanceof ISPObservationContainer) {
                List<ISPObservation> l = ((ISPObservationContainer)parent).getObservations();
                if (_moveNode(l, (ISPObservation) node, up, toEnd)) {
                    ((ISPObservationContainer)parent).setObservations(l);
                    return true;
                }
            }
        } else if (node instanceof ISPObsComponent) {
            if (parent instanceof ISPObsComponentContainer) {
                List<ISPObsComponent> l = ((ISPObsComponentContainer)parent).getObsComponents();
                if (_moveNode(l, (ISPObsComponent) node, up, toEnd)) {
                    ((ISPObsComponentContainer)parent).setObsComponents(l);
                    return true;
                }
            }
        } else if (node instanceof ISPGroup) {
            if (parent instanceof ISPGroupContainer) {
                List<ISPGroup> l = ((ISPGroupContainer)parent).getGroups();
                if (_moveNode(l, (ISPGroup) node, up, toEnd)) {
                    ((ISPGroupContainer)parent).setGroups(l);
                    return true;
                }
            }
        } else if (node instanceof ISPSeqComponent) {
            if (parent instanceof ISPSeqComponent) {
                List<ISPSeqComponent> l = ((ISPSeqComponent)parent).getSeqComponents();
                if (_moveNode(l, (ISPSeqComponent) node, up, toEnd)) {
                    ((ISPSeqComponent)parent).setSeqComponents(l);
                    return true;
                }
            }
        } else if (node instanceof ISPTemplateParameters) {
            if (parent instanceof ISPTemplateGroup) {
                List<ISPTemplateParameters> l = ((ISPTemplateGroup)parent).getTemplateParameters();
                if (_moveNode(l, (ISPTemplateParameters) node, up, toEnd)) {
                    ((ISPTemplateGroup)parent).setTemplateParameters(l);
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Return the given parameter value from the given data object.
     *
     * @param dataObject usually an instrument data object
     * @return the parameter value, if found, otherwise null
     */
    public static Object getDefaultParamValue(ISPDataObject dataObject,
                                              PropertyDescriptor pd) {
        if (dataObject == null) return null;

        try {
            return pd.getReadMethod().invoke(dataObject);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return null;
        }
    }

    //Removes the existing node from the given parent, if it's an ObsComponent (except notes) or
    //a SeqComponent.
    //This is used by the addOrReplace and moveOrReplace mechanisms to allow overriding of existing nodes in the tree
    //Ask the user for confirmation. The previous answer provided by the user is passed as
    //an argument, and the new selected option is returned as a result of the operation
    //(useful for remembering answer provided by the user in case of iterations)

    protected static int _removeExistingNode(ISPNode node, ISPNode parent, int answer)  {

        if (node == null || parent == null) return answer;

        if (node.getParent() == parent) return answer; //don't bother removing it if it's myself.

        //if this is a obscomponent or a sequence component, let's
        //see if we have already one present. If so, replace it.
        //Special case are the notes, that will be appended all the time.
        if (parent instanceof ISPObservation && (node instanceof ISPObsComponent || node instanceof ISPSeqComponent)) {
            SPComponentType compType;
            if (node instanceof ISPObsComponent) {
                ISPObsComponent component = (ISPObsComponent) node;
                compType = component.getType();
            } else {
                ISPSeqComponent component = (ISPSeqComponent) node;
                compType = component.getType();
            }

            //notes will be appended, not overriden.
            if (!compType.broadType.equals(SPNote.SP_TYPE.broadType)) {
                ISPObsComponent obsComp = SPTreeUtil.findObsComponentByNarrowType((ISPObservation) parent, compType.narrowType);
                ISPSeqComponent seqComp = SPTreeUtil.findSeqComponent((ISPObservation) parent, compType);
                if (obsComp != null || seqComp != null) { //the parent has a node present already, ask the user
                    if (answer == USER_CONFIRMATION_INITIAL_STATE) {
                        //ask the user for confirmation
                        answer = DialogUtil.confirm(null, "A node of the same type already exists in this location.\n" +
                                "Do you want to replace it with the one you're moving?");
                    }
                    if (answer == JOptionPane.OK_OPTION) {
                        if (obsComp != null) {
                            removeNode(obsComp);
                        }

                        if (seqComp != null) {
                            removeNode(seqComp);
                        }
                    }
                }
            }
        }
        return answer;
    }

    public interface PendingUpdate {
        void apply() throws SPException;
    }

    public static class PendingPasteUpdate implements PendingUpdate {
        public final ISPObsComponent target;
        public final ISPObsComponent source;

        PendingPasteUpdate(ISPObsComponent target, ISPObsComponent source) {
            this.target = target;
            this.source = source;
        }

        public void apply() {
            target.setDataObject(source.getDataObject());
            ImOption.apply(target.getContextObservation()).foreach(o -> AsterismEditUtil.matchAsterismToInstrument(o));
        }
    }

    public static class PendingAddUpdate implements PendingUpdate {
        public final ISPProgram prog;
        public final ISPNode parent;
        public final List<ISPNode> nodes;

        PendingAddUpdate(ISPProgram prog, ISPNode parent, List<ISPNode> nodes) {
            this.prog   = prog;
            this.parent = parent;
            this.nodes  = nodes;
        }

        public void apply() throws SPException {
            for (ISPNode node : nodes) {
                addNode(prog, parent, copyNode(prog, node));
            }
        }
    }

    private static ISPObsComponent findObsComp(ISPObsComponentContainer parent, SPComponentType type) {
        for (ISPObsComponent child : parent.getObsComponents()) {
            if (child.getType().equals(type)) return child;
        }
        return null;
    }

    public static List<PendingUpdate> getUpdates(ISPProgram prog, ISPNode parent, ISPNode[] nodes) {
        final List<PendingUpdate> ups = new ArrayList<>();
        final List<ISPNode> other = new ArrayList<>(Arrays.asList(nodes));

        // Okay, separate out any ISPObsComponents that have cardinality 1 and
        // that already exist in the parent.  These will be pasted in. :/
        if (parent instanceof ISPObsComponentContainer) {
            final ISPObsComponentContainer occ = (ISPObsComponentContainer) parent;
            final ListIterator<ISPNode> lit = other.listIterator();
            while (lit.hasNext()) {
                final ISPNode node = lit.next();
                if (node instanceof ISPObsComponent) {
                    final ISPObsComponent src = (ISPObsComponent) node;
                    final NodeCardinality nc = NodeType.forNode(parent).cardinalityOf(NodeType.forNode(node));
                    if (nc.toInt() == 1) {
                        final ISPObsComponent target = findObsComp(occ, src.getType());
                        if (target != null) {
                            lit.remove();
                            ups.add(new PendingPasteUpdate(target, src));
                        }
                    }
                }
            }
        }

        // For everything else, if it is okay to add them, make an add update.
        final ISPNode[] nodes0 = (ups.size() == 0) ? nodes : other.toArray(new ISPNode[other.size()]);
        if (isOkayToAdd(prog, nodes0, parent, parent)) {
            ups.add(new PendingAddUpdate(prog, parent, other));
        }

        return ups;
    }

    private static ISPNode getUniqueSelectedNodeOrNull(SPTree tree) {
        final ISPNode[] selectedNodes = tree.getSelectedNodes();
        if (selectedNodes == null) {
            DialogUtil.error("There is nothing selected in the program tree.");
            return null;
        }
        if (selectedNodes.length > 1) {
            DialogUtil.error("Select a single node in the program tree and try again.");
            return null;
        }
        return selectedNodes[0];
    }

    /**
     * Add the given ISPNodes to the given program. The target location
     * is the current selected nodes in the tree.  There must be one selected
     * node.
     *
     * @param tree SPTree where the nodes will be added or replaced
     * @param prog The program where the nodes are
     * @param nodes the nodes that will be added or replaced in the tree.
     */
    public static void addACopy(SPTree tree, ISPProgram prog, ISPNode[] nodes) throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        // Selected node is the new parent for the nodes (target location)
        final ISPNode parent = getUniqueSelectedNodeOrNull(tree);
        if (parent == null) return;

        if (!isOkayToAdd(prog, nodes, parent, parent)) {
            DialogUtil.error("Can't place the items at the selected location.");
            return;
        }

        for (ISPNode node : nodes) {
            tree.addNode(copyNode(prog, node), parent);
        }
    }

    /**
     * Move the given ISPNodes to the new parent ISPNode.
     *
     * If the nodes are obsComponents (excepting Notes) or sequenceComponents that already exists in the new
     * destination, the user will be prompted if they should be overriden.
     * @param tree SPTree where the nodes will be moved
     * @param rnodes  Nodes to be moved
     * @param parent new parent of the nodes to be moved
     */
    public static void moveOrReplaceTo(SPTree tree, ISPNode[] rnodes, ISPNode parent) throws SPTreeStateException, SPNodeNotLocalException, SPUnknownIDException {
        int answer = USER_CONFIRMATION_INITIAL_STATE;
        for (ISPNode node : rnodes) {
            answer = _removeExistingNode(node, parent, answer);
            if (answer == JOptionPane.OK_OPTION || answer == USER_CONFIRMATION_INITIAL_STATE) {
                tree.moveNode(node, parent);
            }
        }
    }



}

