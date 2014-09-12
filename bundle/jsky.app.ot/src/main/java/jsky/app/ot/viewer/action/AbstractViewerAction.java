package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.Platform;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.SPViewer;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: rnorris
 * Date: 1/17/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractViewerAction extends AbstractAction {

    /**
     * The Action.NAME key is used by default for user interface elements like
     * buttons and menu items, but SHORT_NAME provides an alternative shorter
     * name that can be used in a context in which the normal name is too long.
     * For example, tool bar buttons typically want a single word name.
     */
    public static final String SHORT_NAME = "shortNameKey";

    private static final Map<SPViewer, List<AbstractViewerAction>> INSTANCES = new HashMap<SPViewer, List<AbstractViewerAction>>();

    protected static SPNodeKey BOGUS_KEY = new SPNodeKey("F57FFD6F-83F8-49D8-AC70-91FDC0103662");

    /**
     * A hack to ensure that we can enable/disable all actions in one shot and not inadvertently miss one.
     * Instances are added to this collection on
     */
    public static Collection<AbstractViewerAction> getInstances(SPViewer viewer) {
        final Collection<AbstractViewerAction> actionList = INSTANCES.get(viewer);
        return (actionList == null) ? Collections.<AbstractViewerAction>emptyList() : Collections.unmodifiableCollection(actionList);
    }

    public static void forgetInstances(SPViewer viewer) {
        INSTANCES.remove(viewer);
    }

    protected final SPViewer viewer;

    protected AbstractViewerAction(SPViewer viewer) {
        this.viewer = viewer;
        init(viewer);
    }

    protected AbstractViewerAction(SPViewer viewer, String name) {
        super(name);
        this.viewer = viewer;
        init(viewer);
    }

    protected AbstractViewerAction(SPViewer viewer, String name, Icon icon) {
        super(name, icon);
        this.viewer = viewer;
        init(viewer);
    }

    // Post-construction initialization
    private void init(SPViewer viewer) {
//        System.out.println("-------------- " + getClass().getSimpleName());
        final List<AbstractViewerAction> existingActions = INSTANCES.get(viewer);
        final List<AbstractViewerAction> actionList;
        if (existingActions == null) {
            actionList = new ArrayList<AbstractViewerAction>();
            INSTANCES.put(viewer, actionList);
        }  else {
            actionList = existingActions;
        }
        actionList.add(this);
        setEnabled(false); // is this ok?
    }

    /**
     * Subtypes must implement to compute enabled state based on whatever's
     * happening in the current viewer. Assume for the moment that this
     * method will be called whenever anything relevant happens.
     */
    public abstract boolean computeEnabledState() throws Exception;

//    /**
//     * Returns the selected node. Subclasses may override to return, for example, a parent of the selected
//     * node, if that makes sense for the action.
//     */
//    protected ISPNode getContextNode() {
//        return getContextNode(ISPNode.class);
//    }

    /**
     * Returns the viewer's currently selected node if it is of the specified type, otherwise return
     * a parent node of the specified type, if any (otherwise null).
     */
    protected <T extends ISPNode> T getContextNode(Class<T> clazz) {
//        final NodeData viewable = viewer.getViewable();
        ISPNode n = viewer == null ? null : viewer.getNode(); // (getProgram() != null && viewable != null) ? viewable.getNode() : null;
        while (n != null && !clazz.isInstance(n))
            n = n.getParent();
        return (T) n;
    }

    protected <T extends ISPNode> T nodeIf(Class<T> clazz) {
        final ISPNode n = viewer == null ? null : viewer.getNode();
        return clazz.isInstance(n) ? (T) n : null;
    }

    protected ISPProgram getProgram() {
        return viewer == null ? null : viewer.getProgram();
    }

    protected boolean isEditableContext() {
        final ISPProgram root = viewer == null ? null : viewer.getRoot();
        final ISPObservation obs = viewer == null ? null : viewer.getContextObservation();
        return OTOptions.areRootAndCurrentObsIfAnyEditable(root, obs);
    }

    public void uninstall(SPViewer viewer) {
        System.out.println("Uninstall menu action" + getValue(Action.NAME));
        final List<AbstractViewerAction> actionList = INSTANCES.get(viewer);
        if (actionList != null) actionList.remove(this);
        setEnabled(false);
    }

    public static int platformEventMask() {
        return (Platform.get() == Platform.osx) ? InputEvent.META_MASK : InputEvent.CTRL_DOWN_MASK;
    }
}
