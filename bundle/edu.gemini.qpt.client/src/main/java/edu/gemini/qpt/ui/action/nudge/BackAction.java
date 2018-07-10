package edu.gemini.qpt.ui.action.nudge;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class BackAction extends NudgeAction {

    public BackAction(IShell shell) {
        super("Nudge Back", shell);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Platform.MENU_ACTION_MASK));
    }
    
    protected long getNudgeDelta() {
        return -Resolution.value();
    }
    
}
