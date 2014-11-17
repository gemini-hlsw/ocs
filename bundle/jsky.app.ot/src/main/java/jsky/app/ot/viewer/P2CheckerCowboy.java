//
// $Id$
//

package jsky.app.ot.viewer;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.api.ProblemRollup;
import edu.gemini.p2checker.checker.P2Checker;
import edu.gemini.p2checker.util.P2CheckerUtil;
import edu.gemini.pot.sp.*;

import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Enumeration;
import java.util.logging.Logger;

/**
 *
 */
final class P2CheckerCowboy {
    private static final Logger LOG = Logger.getLogger(P2CheckerCowboy.class.getName());

    static P2CheckerCowboy INSTANCE = new P2CheckerCowboy();

    private P2CheckerCowboy() {
    }


    void check(ISPNode node, SPTree tree, AgsMagnitude.MagnitudeTable mt)  {
        IP2Problems probs = P2Checker.getInstance().check(node, mt);
        if (probs == null) return;

        // if an observation or greater, clear everything below this node
        // if an obs comp, find the obs (if any) and clear it

        DefaultMutableTreeNode treeNode = tree.getTreeNode(node);
        if (treeNode == null) {
            LOG.info("Missing tree node for: " + node);
            return;
        }

        DefaultMutableTreeNode parentTreeNode;
        parentTreeNode = getContainer(node, treeNode);
        if (parentTreeNode == null) {
            LOG.info("Missing parent tree node for: " + node);
            return;
        }

        clearProblems(parentTreeNode);
        clearParentProblems(parentTreeNode);

        // walk through the problems and set the info on the NodeData
        // at the same time, get the list of observations that were
        // updated and set their problem status summaries

        for (Problem prob : probs.getProblems()) {
            ISPNode probNode = prob.getAffectedNode();
            if (probNode == null) continue;

            DefaultMutableTreeNode probTreeNode = tree.getTreeNode(probNode);
            if (probTreeNode == null) continue;

            Object obj = probTreeNode.getUserObject();
            if (!(obj instanceof NodeData)) continue;

            NodeData probNodeData = (NodeData) obj;
            probNodeData.addProblem(prob);
        }

        // Set the container summary
        updateRollups((DefaultMutableTreeNode) parentTreeNode.getRoot());
    }

    void updateRollups(ISPNode node, SPTree tree) {
        DefaultMutableTreeNode treeNode = tree.getTreeNode(node);
        if (treeNode == null) return;
        clearParentProblems(treeNode);
        updateRollups((DefaultMutableTreeNode) treeNode.getRoot());
    }

    private DefaultMutableTreeNode getContainer(ISPNode node, DefaultMutableTreeNode treeNode) {
        if (node == null) return null;
        if ((node instanceof ISPContainerNode) && !(node instanceof ISPSeqComponent)) {
            return treeNode;
        }
        if (treeNode == null) return null;

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
        if (parent == null) return null;

        Object obj = parent.getUserObject();
        if (!(obj instanceof NodeData)) return null;

        return getContainer(((NodeData) obj).getNode(), parent);
    }

    private void clearProblems(DefaultMutableTreeNode treeNode) {
        Object obj = treeNode.getUserObject();
        if (obj instanceof NodeData) {
            NodeData nodeData = (NodeData) obj;
            nodeData.clearProblems();
            ISPNode remoteNode = nodeData.getNode();
            if (remoteNode instanceof ISPSeqComponent) return; // no need to go deeper
        }

        Enumeration childEnum = treeNode.children();
        while (childEnum.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) childEnum.nextElement();
            clearProblems(child);
        }
    }

    private void clearParentProblems(DefaultMutableTreeNode treeNode) {
        if (treeNode == null) return;
        Object obj = treeNode.getUserObject();
        if (obj instanceof NodeData) {
            NodeData nodeData = (NodeData) obj;
            nodeData.clearProblems();
        }
        clearParentProblems((DefaultMutableTreeNode) treeNode.getParent());
    }

    private void updateRollups(DefaultMutableTreeNode root) {
        if (root == null) return;

        NodeData uiv = (NodeData) root.getUserObject();
        ISPNode node = uiv.getNode();
        if (!(node instanceof ISPProgramNode)) {
            uiv.setProblems(P2CheckerUtil.NO_PROBLEMS);
            return;
        }

        if (node instanceof ISPObservation) {
            updateObsProblemSummary(root, uiv);
        } else {
            updateContainerProblemSummary(root, uiv);
        }
    }

    // Show all the problems contained in an observation in it's node.
    private void updateObsProblemSummary(DefaultMutableTreeNode obsTreeNode, NodeData uiv) {
        Enumeration childEnum = obsTreeNode.children();
        while (childEnum.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) childEnum.nextElement();
            NodeData childUiv = (NodeData) child.getUserObject();

            IP2Problems childProbs = childUiv.getProblems();
            for (Problem p : childProbs.getProblems()) {
                uiv.addProblem(p);
            }
        }
    }

    // For groups and the root program node, show counts of the problems.
    /*
    private void updateContainerProblemSummary(DefaultMutableTreeNode root, NodeData uiv) {
        ISPNode node = uiv.getProgramNode();
        List<String> ignoredProblems = SPProblemsViewer.getIgnoredProblems(node);
        int[] summaries = new int[Problem.Type.values().length];
        int[] ignoredSummaries = new int[Problem.Type.values().length];
        Enumeration childEnum = root.children();
        while (childEnum.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) childEnum.nextElement();
            NodeData childUiv = (NodeData) child.getUserObject();

            if (!childUiv.isCheckedForProblems()) {
                updateRollups(child);
            }

            IP2Problems childProbs = childUiv.getProblems();
            for (Problem p : childProbs.getProblems()) {
                if (p instanceof ProblemRollup) {
                    ProblemRollup pr = (ProblemRollup) p;
                    if (pr.getDescription().contains(" ignored ")) {
                        ignoredSummaries[pr.getType().ordinal()] += pr.getRepresentedCount();
                    } else {
                        summaries[pr.getType().ordinal()] += pr.getRepresentedCount();
                    }
                } else {
                    if (ignoredProblems.contains(p.getId())) {
                        ++ignoredSummaries[p.getType().ordinal()];
                    } else {
                        ++summaries[p.getType().ordinal()];
                    }
                }
            }
        }

        // REL-336, REL-337: Count top level errors
        if (node instanceof ISPProgram) {
            IP2Problems probs = uiv.getProblems();
            for (Problem p : probs.getProblems()) {
                if (!(p instanceof ProblemRollup)) {
                    if (ignoredProblems.contains(p.getId())) {
                        ++ignoredSummaries[p.getType().ordinal()];
                    } else {
                        ++summaries[p.getType().ordinal()];
                    }
                }
            }
        }

        for (Problem.Type type : Problem.Type.values()) {
            int count = summaries[type.ordinal()];
            if (count == 0) continue;

            String desc = String.format("Contains %d %s%s.", count, type.getDisplayValue(), (count>1) ? "s" : "");
            ProblemRollup pr = new ProblemRollup(type, type.getDisplayValue(), desc, (ISPProgramNode) node, count);
            uiv.addProblem(pr);
        }
        for (Problem.Type type : Problem.Type.values()) {
            int count = ignoredSummaries[type.ordinal()];
            if (count == 0) continue;

            String desc = String.format("Contains %d ignored %s%s.", count, type.getDisplayValue(), (count>1) ? "s" : "");
            ProblemRollup pr = new ProblemRollup(type, "ignored_"+ type.getDisplayValue(), desc, (ISPProgramNode) node, count);
            uiv.addProblem(pr);
        }
    }
    */
    private void updateContainerProblemSummary(DefaultMutableTreeNode root, NodeData uiv) {
        ISPNode node = uiv.getNode();
        int[] summaries = new int[Problem.Type.values().length];
        Enumeration childEnum = root.children();
        while (childEnum.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) childEnum.nextElement();
            NodeData childUiv = (NodeData) child.getUserObject();

            if (!childUiv.isCheckedForProblems()) {
                updateRollups(child);
            }

            IP2Problems childProbs = childUiv.getProblems();
            for (Problem p : childProbs.getProblems()) {
                if (p instanceof ProblemRollup) {
                    ProblemRollup pr = (ProblemRollup) p;
                    summaries[pr.getType().ordinal()] += pr.getRepresentedCount();
                } else {
                    ++summaries[p.getType().ordinal()];
                }
            }
        }

        for (Problem.Type type : Problem.Type.values()) {
            int count = summaries[type.ordinal()];
            if (count == 0) continue;

            String desc = String.format("Contains %d %s%s.", count, type.getDisplayValue(), (count>1) ? "s" : "");
            ProblemRollup pr = new ProblemRollup(type, type.getDisplayValue(), desc, (ISPProgramNode) node, count);
            uiv.addProblem(pr);
        }
    }
}
