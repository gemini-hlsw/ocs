//
// $
//

package edu.gemini.spModel.obsseq.parser;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ImmutableConfig;
import edu.gemini.spModel.config2.ItemEntry;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 */
public final class ConfigTreeNode {
    private Config _config;
    private ConfigTreeNode _parent;
    private List<ConfigTreeNode> _children;

    public ConfigTreeNode(Config conf) {
        _config = new ImmutableConfig(conf);
    }

    public Config getConfig() {
        return _config;
    }

    public void setConfig(Config config) {
        _config = new ImmutableConfig(config);
    }

    public ConfigTreeNode getParent() {
        return _parent;
    }

    public ConfigTreeNode getLastChild() {
        if (_children == null) return null;
        return _children.get(_children.size() - 1);
    }

    public void addChild(ConfigTreeNode child) {
        if (_children == null) _children = new ArrayList<ConfigTreeNode>();
        _children.add(child);

        if (child._parent != null) {
            throw new IllegalStateException("Child already attached to a parent");
        }
        child._parent = this;
    }

    public List<ConfigTreeNode> getChildren() {
        if (_children == null) return Collections.emptyList();
        return Collections.unmodifiableList(_children);
    }

    public void setChildren(List<ConfigTreeNode> children) {
        _children = new ArrayList<ConfigTreeNode>(children);
    }

    public List<ConfigTreeNode> detachChildren() {
        if (_children == null) {
            return Collections.emptyList();
        }

        List<ConfigTreeNode> res = _children;
        _children = null;
        for (ConfigTreeNode node : res) {
            node._parent = null;
        }
        return res;
    }

    private void appendConfig(StringBuilder buf) {
        buf.append('{');
        for (ItemEntry ie : _config.itemEntries()) {
            buf.append('(');
            buf.append(ie.getKey());
            buf.append("->");
            buf.append(ie.getItemValue());
            buf.append(')');
        }
        buf.append('}');
    }

    private void append(StringBuilder buf, String prefix) {
        buf.append(prefix);
        appendConfig(buf);
        buf.append('\n');
        if (_children != null) {
            for (ConfigTreeNode child : _children) {
                child.append(buf, prefix + "\t");
            }
        }
    }

    public String toString() {
        StringBuilder res = new StringBuilder();
        append(res, "");
        return res.toString();
    }

}
