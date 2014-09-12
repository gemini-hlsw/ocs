//
// $
//

package edu.gemini.spModel.obsseq.parser;

import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public final class ConfigIteratorTree {
    private ConfigIterator _it;
    private List<ConfigIteratorTree> _children = new ArrayList<ConfigIteratorTree>();

    public ConfigIteratorTree(ConfigIterator it, List<ConfigIteratorTree> children) {
        _it = it;
        _children = new ArrayList<ConfigIteratorTree>(children);
    }

    public ConfigIterator getConfigIterator() {
        return _it;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ConfigIteratorTree)) return false;

        ConfigIteratorTree that = (ConfigIteratorTree) other;
        if (!_it.equals(that._it)) return false;
        return _children.equals(that._children);
    }

    public boolean isCompatible(ConfigIteratorTree tree) {
        if (!_it.isCompatible(tree._it)) return false;
        return _children.equals(tree._children);
    }

    public void mergeWith(ConfigIteratorTree tree) {
        _it.mergeWith(tree._it);
    }

    public int hashCode() {
        return 37*_it.hashCode() + _children.hashCode();
    }

    public static ConfigIteratorTree create(ConfigTreeNode root) {
        // Get child iterator trees.
        List<ConfigIteratorTree> children = new ArrayList<ConfigIteratorTree>();
        for (ConfigTreeNode childNode : root.getChildren()) {
            children.add(create(childNode));
        }

        // See if we can combine them.
        List<ConfigIteratorTree> mergedChildren = new ArrayList<ConfigIteratorTree>();
        if (children.size() > 0) {
            ConfigIteratorTree last = children.get(0);
            for (int i=1; i<children.size(); ++i) {
                ConfigIteratorTree cur = children.get(i);
                if (last.isCompatible(cur)) {
                    last.mergeWith(cur);
                } else {
                    mergedChildren.add(last);
                    last = cur;
                }
            }
            mergedChildren.add(last);
        }

        // Make an iterator tree node to hold the root node.
        ConfigIterator thisIt = new ConfigIterator(root.getConfig());
        return new ConfigIteratorTree(thisIt, mergedChildren);
    }

    private void append(StringBuilder buf, String prefix) {
        buf.append(prefix);
        buf.append(_it.toString()).append("\n");
        for (ConfigIteratorTree child : _children) {
            child.append(buf, prefix + "\t");
            buf.append("\n");
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        append(buf, "");
        return buf.toString();
    }
}
