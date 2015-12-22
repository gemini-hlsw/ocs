package edu.gemini.ui.workspace.util;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * A split pane that doesn't draw borders. 
 * <p>
 * The updateUI() technique was adapted from some JGoodies sample code.
 * @author rnorris
 *
 */
@SuppressWarnings("serial")
public class CleanSplitPane extends JSplitPane {
    
    {
        setBorder(BorderFactory.createEmptyBorder());
    }

    public CleanSplitPane(int alignment, Component one, Component two) {
        super(alignment, one, two);
    }

    @Override
    public void updateUI() {
        super.updateUI();        
        SplitPaneUI ui = getUI();
        if (ui instanceof BasicSplitPaneUI) {
            BasicSplitPaneDivider divider = ((BasicSplitPaneUI) ui).getDivider();
            divider.setBorder(BorderFactory.createEmptyBorder());
        }
    }
    
}
