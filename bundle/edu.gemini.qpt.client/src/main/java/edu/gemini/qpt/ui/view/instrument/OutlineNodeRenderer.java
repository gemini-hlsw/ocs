package edu.gemini.qpt.ui.view.instrument;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.gemini.qpt.ui.util.SharedIcons;

@SuppressWarnings("serial")
public class OutlineNodeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        OutlineNodeRenderer ret = (OutlineNodeRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);        
        switch (((OutlineNode) value).getSelected()) {
        
        case INDEFINITE:
            setIcon(SharedIcons.CHECK_INDETERMINATE);
            break;
            
        case SELECTED:
            setIcon(SharedIcons.CHECK_SELECTED);
            break;
        
        case UNSELECTED:
            setIcon(SharedIcons.CHECK_UNSELECTED);
            break;
        
        }    
        return ret;
    }
    
}
