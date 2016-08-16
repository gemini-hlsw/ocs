// Copyright 2001
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: NodeData.java 46768 2012-07-16 18:58:53Z rnorris $
//

package jsky.app.ot.viewer;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.util.P2CheckerUtil;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPTemplateGroup;
import edu.gemini.shared.gui.DecoratedIcon;
import edu.gemini.shared.gui.DoubleIcon;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.util.VersionToken;
import jsky.app.ot.OTOptions;
import jsky.app.ot.nsp.UIInfo;
import jsky.app.ot.ui.util.UIConstants;
import jsky.util.gui.Resources;

import javax.swing.Icon;

/**
 * Client data that is stored in each DefaultMutableTreeNode.
 * Used to map the tree node to the program node.
 * Also stores the ui open/closed state of the node
 * and the icon and text used in rendering the node.
 * This class is used for the standard tree nodes that have a
 * corresponding science program node.
 */
public final class NodeData {
    public static final class State {
        public static State apply(boolean open, IP2Problems problems) {
            return new State(
                    open,
                    (problems == null) ? null : P2CheckerUtil.unmodifiableP2Problems(new P2Problems(problems))
            );
        }

        public static State EMPTY = new State(false, null);

        public final boolean open;
        private final IP2Problems problems;

        private State(boolean open, IP2Problems problems) {
            this.open     = open;
            this.problems = problems;
        }

        public boolean isCheckedForProblems() {
            return problems != null;
        }

        public IP2Problems problems() {
            return (problems == null) ? P2CheckerUtil.NO_PROBLEMS : problems;
        }

        public State setOpen(boolean open) {
            return (this.open == open) ? this : new State(open, problems);
        }

        public State setProblems(IP2Problems problems) {
            final IP2Problems p = (problems == null) ? null : P2CheckerUtil.unmodifiableP2Problems(new P2Problems(problems));
            return new State(open, p);
        }

        public State addProblem(Problem problem) {
            final P2Problems mutable = (problems == null) ? new P2Problems() : new P2Problems(problems);
            mutable.append(problem);
            return new State(open, P2CheckerUtil.unmodifiableP2Problems(mutable));
        }

        public State clearProblems() {
            return new State(open, null);
        }
    }

    private final ISPNode _programNode; // never null
    public final UIInfo _uiInfo;
    private State state;
    private Icon _icon;

    public NodeData(ISPNode programNode, UIInfo uiInfo, State state) {
        if (programNode == null) throw new IllegalArgumentException("programNode cannot be null.");
        _programNode = programNode;
        _uiInfo      = uiInfo;
        this.state   = state;
        if (uiInfo != null) setIcon(Resources.getIcon(uiInfo.getImageKey()));
    }

    /**
     * If this object has an associated science program node, return it, otherwise null.
     */
    public ISPNode getNode() {
        return _programNode;
    }

    /**
     * Return the data object for the node
     */
    public ISPDataObject getDataObject() {
        return _programNode.getDataObject();
    }

    public State getState() { return state; }
    public void setState(State s) { state = s; }

    /**
     * Return an object with information for use by the user interface
     */
    public UIInfo getUIInfo() { return _uiInfo; }

    /**
     * Set to true if the node is open (expanded)
     */
    public boolean isOpen() { return state.open; }

    /**
     * Set to true if the node is open (expanded)
     */
    public void setOpen(boolean open) { state = state.setOpen(open); }

    public IP2Problems getProblems() { return state.problems(); }
    public void setProblems(IP2Problems problems) { state = state.setProblems(problems); }
    public void addProblem(Problem problem) { state = state.addProblem(problem); }
    public void clearProblems() { state = state.clearProblems(); }
    public boolean isCheckedForProblems() { return state.isCheckedForProblems(); }

    /**
     * Return the title to use for the tree node
     */
    public String getTitle() {

        // Node title (may remain null, oddly)
        final String title;
        final ISPDataObject dataObject = getDataObject();
        if (dataObject instanceof ISPDataObject)
            title = dataObject.getTitle();
        else
            title = "";

        final String _obsNumString;
        if (_programNode instanceof ISPObservation) {
            final int obsNum = ((ISPObservation) _programNode).getObservationNumber();
            _obsNumString = "[" + obsNum + "] ";
        } else {
            _obsNumString = "";
        }

        // Have to do this here because (unlike _obsNumString the token can change)
        if (_programNode instanceof ISPTemplateGroup) {
            final VersionToken token = ((TemplateGroup) dataObject).getVersionToken();
            return "[" + token + "] " + title;
        }

        return _obsNumString + title;

    }

    /**
     * Return the icon to use for the tree node
     */
    public Icon getIcon() {
        if (_programNode instanceof ISPObservation) {
            final ObservationStatus obsStatus = ObservationStatus.computeFor((ISPObservation) getNode());
            _icon = UIConstants.getObsIcon(obsStatus);  // ugh
        }

        // Decorate with the P2 checker problem indicator if applicable.
        final Icon pIcon;
        if (_icon == null) {
            pIcon = null;
        } else {
            final Icon dec = getProblemDecoration();
            pIcon = (dec == null) ? _icon : new DecoratedIcon(_icon, dec);
        }

        final Icon tIcon = pIcon == null ? null : (isInsideTemplateFolder() ? new TemplateIcon(pIcon) : pIcon);
        return tIcon == null ? null : (hasConflict() ? new ConflictIcon(tIcon) : tIcon);
    }

    /**
     * Set the icon to use for the tree node
     */
    public void setIcon(Icon icon) {
        _icon = (icon == null) ? UIConstants.UNKNOWN_ICON : icon;
    }

    private static class TemplateIcon extends DecoratedIcon {
        public TemplateIcon(Icon icon) {
            super(icon, Resources.getIcon("template_co.png"), DecoratedIcon.Location.UPPER_RIGHT);
        }
    }

    private static class ConflictIcon extends DoubleIcon {
        ConflictIcon(Icon icon) {
            super(icon, Resources.getIcon("vcs/vcs_up_conflict.png"), 2);
        }
    }

    private static boolean isInsideTemplateFolder(ISPNode node) {
        if (node instanceof ISPTemplateGroup) return true;
        final ISPNode parent = node.getParent();
        return (parent != null) && isInsideTemplateFolder(parent);
    }

    private boolean isInsideTemplateFolder() {
        return isInsideTemplateFolder(_programNode);
    }

    public boolean hasConflict() {
        return _programNode.hasConflicts();
    }

    private Icon getProblemDecoration() {
        //don't set decorations if the checking engine is disabled
        if (!OTOptions.isCheckingEngineEnabled()) return null;

        switch (getProblems().getSeverity()) {
            case ERROR:   return UIConstants.ERROR_DECORATION;
            case WARNING: return UIConstants.WARNING_DECORATION;
            default:      return null;
        }
    }

        /* UX-1520: turning this off for now.
        // Determine the most severe level
        Problem.Type type = Problem.Type.NONE;
        List<String> ignoreProblems = SPProblemsViewer.getIgnoredProblems(_programNode);
        for (Problem p : getProblems().getProblems()) {
            if (p instanceof ProblemRollup && p.getDescription().contains(" ignored ")) {
                continue;
            }
            if (p.getType() == Problem.Type.ERROR) {
                type = Problem.Type.ERROR;
                break;
            }
            type = Problem.Type.WARNING;
        }


        // REL-383: Use an icon with a gray warning sign to indicate that it has an overridden warning
        boolean allIgnored = true;
        for (Problem p : getProblems().getProblems()) {
            if ((p instanceof ProblemRollup && p.getDescription().contains(" ignored "))
                    || !ignoreProblems.contains(p.getId())) {
                allIgnored = false;
                break;
            }
        }

        // Note: currently only warnings can be ignored, although there would be support here for ignoring errors as well
        switch (type) {
            case ERROR:
                addDecoration(allIgnored ? UIConstants.IGNORED_ERROR_DECORATION : UIConstants.ERROR_DECORATION);
                break;
            case WARNING:
                addDecoration(allIgnored ? UIConstants.IGNORED_WARNING_DECORATION : UIConstants.WARNING_DECORATION);
                break;
            case NONE:
                removeDecoration();
                break;
        }
        */
}

