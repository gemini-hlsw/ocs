package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.sp.SPTreeStateException;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class MoveAction extends AbstractViewerAction {

    public enum Op {
        UP("Move Up", KeyEvent.VK_UP),
        DOWN("Move Down", KeyEvent.VK_DOWN),
        TOP("Move to Top", KeyEvent.VK_PAGE_UP),
        BOTTOM("Move to Bottom", KeyEvent.VK_PAGE_DOWN);

        final String label;
        final int keyCode;

        private Op(String s, int keyCode) {
            label = s;
            this.keyCode = keyCode;
        }
    }

    private final Op op;

    public MoveAction(SPViewer viewer, Op op) {
        super(viewer, op.label);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(op.keyCode, platformEventMask() | InputEvent.SHIFT_DOWN_MASK));
        this.op = op;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            switch (op) {
                case UP:
                    _moveNode(true);
                    break;
                case DOWN:
                    _moveNode(false);
                    break;
                case TOP:
                    _moveNodeToEnd(true);
                    break;
                case BOTTOM:
                    _moveNodeToEnd(false);
                    break;
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Move the selected node up or down in the tree.
     */
    public void _moveNode(boolean up) throws SPNodeNotLocalException, SPTreeStateException {
        ISPNode node = viewer.getNode();
        if (node != null) {
            BusyWin.showBusy();
            SPTreeEditUtil.moveNode(node, up, false);
        }
    }

    /**
     * Move the selected node all the way up or down in the tree, as far as possible.
     */
    public void _moveNodeToEnd(boolean up) throws SPNodeNotLocalException, SPTreeStateException {
        ISPNode node = viewer.getNode();
        if (node != null) {
            BusyWin.showBusy();
            SPTreeEditUtil.moveNode(node, up, true);
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        // RCN: you can't in general tell if you can move something up or down because a node's
        // children are often broken into several categories and you can't move outside that category
        // in the parent's getChildren() list. So punt.
        return isEditableContext() && getContextNode(ISPNode.class) != null;
    }
}
