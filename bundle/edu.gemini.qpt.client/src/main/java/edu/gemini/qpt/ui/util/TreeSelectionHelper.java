package edu.gemini.qpt.ui.util;

import java.util.Arrays;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TreeSelectionHelper {

    private final JTree tree;

    private boolean adjusting = false;
    
    public TreeSelectionHelper(JTree tree) {
        this.tree = tree;
    }
    
    public Object[] getSelection() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            Object[] ret = new Object[paths.length];
            for (int i = 0; i < paths.length; i++) {
                Object obj = paths[i].getLastPathComponent();
                if (obj instanceof DefaultMutableTreeNode) obj = ((DefaultMutableTreeNode) obj).getUserObject();
                ret[i] = obj;
            }
            return ret;
        }
        return new Object[0];
    }

    public void setSelection(Object[] sel) {        
        if (adjusting) return;
        
        // Only do this if the selection is different
        if (Arrays.equals(getSelection(), sel)) return;

        
        try {
            adjusting = true;
            tree.clearSelection();
            Object root = tree.getModel().getRoot();
            if (sel != null && sel.length > 0 && root != null)
                setSelection(sel, new TreePath(root));
        } finally {
            adjusting = false;
        }
    }
    
    private void setSelection(Object[] sel, TreePath path) {
        Object last = path.getLastPathComponent();
        Object target = last;
        if (target instanceof DefaultMutableTreeNode) target = ((DefaultMutableTreeNode) target).getUserObject();
        for (Object o: sel) {
            if (o == target) {
                tree.addSelectionPath(path);
                tree.expandPath(path);
                tree.scrollPathToVisible(path);
            }
        }
        TreeModel model = tree.getModel();
        for (int i = 0; i < model.getChildCount(last); i++) {
            setSelection(sel, path.pathByAddingChild(model.getChild(last, i)));
        }
    }

    public boolean isSettingSelection() {
        return adjusting;
    }
    
}
