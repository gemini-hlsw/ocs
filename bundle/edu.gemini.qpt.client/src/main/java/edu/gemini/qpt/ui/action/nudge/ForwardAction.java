package edu.gemini.qpt.ui.action.nudge;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class ForwardAction extends NudgeAction {

    public ForwardAction(IShell shell) {
        super("Nudge Forward", shell);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Platform.MENU_ACTION_MASK));
    }

    protected long getNudgeDelta() {
        return Resolution.value();
    }
    
}
