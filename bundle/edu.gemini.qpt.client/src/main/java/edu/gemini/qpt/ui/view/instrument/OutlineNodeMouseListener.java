package edu.gemini.qpt.ui.view.instrument;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.gemini.qpt.ui.view.instrument.OutlineNode.TriState;

class OutlineNodeMouseListener extends MouseAdapter {
    
    @Override
    public void mousePressed(MouseEvent e) {
        JTree tree  = (JTree) e.getSource();
        int row = tree.getRowForLocation(e.getX(), e.getY());
        TreePath path = tree.getPathForRow(row);
        if (path != null) {
            OutlineNode node = (OutlineNode) path.getLastPathComponent();
            node.setSelected(node.getSelected() == TriState.UNSELECTED);
            for (Object o: path.getPath()) {
                ((DefaultTreeModel) tree.getModel()).nodeChanged((TreeNode) o);
            }
        }
    }

}
