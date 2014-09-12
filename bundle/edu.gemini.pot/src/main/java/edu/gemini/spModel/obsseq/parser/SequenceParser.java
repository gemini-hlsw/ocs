//
// $
//

package edu.gemini.spModel.obsseq.parser;

import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemKey;

/**
 *
 */
public class SequenceParser {

    public static ConfigTreeNode parse(ConfigSequence seq) {
        ConfigTreeNode rootNode = new ConfigTreeNode(new DefaultConfig());
        for (Config step : seq.getAllSteps()) {
            addStep(rootNode, step);
        }
        return rootNode;
    }

    private static void addStep(ConfigTreeNode curNode, Config newConfig) {
        Config curConfig = curNode.getConfig();

        // Is the current config is a subset of the new config?
        if (newConfig.matches(curConfig)) {
            // Yes, so it should be a child of this node.
            ConfigTreeNode lastChild = curNode.getLastChild();
            if (lastChild == null) {
                curNode.addChild(new ConfigTreeNode(newConfig));
            } else {
                // Get part of the newConfig that is distinct from the
                // curConfig.
                Config distinct = new DefaultConfig(newConfig);
                distinct.removeAll(curConfig);
                addStep(lastChild, distinct);
            }
            return;
        }

        // So the current config is not a proper subset of the newConfig.
        // In other words, it contains one or more key/value pairs that don't
        // match the newConfig.

        // What is common between the two configs, if anything.
        Config intersection = new DefaultConfig(newConfig);
        intersection.retainAll(curConfig);

        if (intersection.size() == 0) {
            // nothing in common, make a new sibling
            ConfigTreeNode parent = curNode.getParent();
            parent.addChild(new ConfigTreeNode(newConfig));
        } else {
            // What is left in curConfig that isn't in the intersection?
            Config curRemaining = new DefaultConfig(curConfig);
            curRemaining.removeAll(intersection);

            // What is left in newConfig that isn't in the intersection?
            Config newRemaining = new DefaultConfig(newConfig);
            newRemaining.removeAll(intersection);

            // set curNode's config to be the intersection
            curNode.setConfig(intersection);

            // make a new node containing the remaining items
            ConfigTreeNode newCurSubset = new ConfigTreeNode(curRemaining);

            // move the curNode's children to the new node
            newCurSubset.setChildren(curNode.detachChildren());

            // make the new node the only child of curNode
            curNode.addChild(newCurSubset);

            // add a new node to hold the new remaining configuration
            ConfigTreeNode newNode = new ConfigTreeNode(newRemaining);

            // made this node a child of curNode
            curNode.addChild(newNode);
        }
    }



    private static final ItemKey FILTER_KEY = new ItemKey("filter");
    private static final ItemKey FILTER_PRIME_KEY = new ItemKey("filter'");
    private static final ItemKey FPU_KEY = new ItemKey("fpu");
    private static final ItemKey P_KEY = new ItemKey("p");
    private static final ItemKey Q_KEY = new ItemKey("q");

    public static void main(String[] args) {
        ConfigSequence seq = new ConfigSequence();

        String[] fpus = {"fpu1", "fpu2"};
        String[] filters = {"r", "g"};
        String[] ps = {"0", "7", "15"};

        for (String fpu : fpus) {
            for (String filter : filters) {
                for (String p : ps) {
                    Config step = new DefaultConfig();
                    step.putItem(FPU_KEY, fpu);
                    step.putItem(FILTER_KEY, filter);
                    step.putItem(FILTER_PRIME_KEY, filter + "'");
                    step.putItem(P_KEY, p);
                    step.putItem(Q_KEY, p);
                    seq.addStep(step);
                }
            }
        }


        /*
        Config step = new DefaultConfig();
        step.putItem(FPU_KEY, "fpuX");
        step.putItem(FILTER_KEY, "filterX");
        step.putItem(P_KEY, "pX");
        seq.addStep(6, step);
        */

        ConfigTreeNode root = SequenceParser.parse(seq);
        System.out.println(root.toString());

        ConfigIteratorTree res = ConfigIteratorTree.create(root);
        System.out.println("----");
        System.out.println(res);
    }
}
