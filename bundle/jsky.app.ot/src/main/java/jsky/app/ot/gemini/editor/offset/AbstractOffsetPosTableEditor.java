//
// $
//

package jsky.app.ot.gemini.editor.offset;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.gui.ButtonFlattener;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * Control code for the table of offset positions.
 */
public abstract class AbstractOffsetPosTableEditor<P extends OffsetPosBase> {

    /**
     * Keeps the table selection in sync with the position list.
     */
    private final class SelectionWatcher implements PropertyChangeListener, ListSelectionListener {
        private boolean updating;

        @Override public void propertyChange(PropertyChangeEvent evt) {
            if (updating) return;
            updating = true;

            try {
                final ListSelectionModel lsm = pan.offsetTable.getSelectionModel();
                lsm.clearSelection();
                final List<P> selList = getAllSelectedPos();
                for (P pos : selList) {
                    int index = opl.getPositionIndex(pos);
                    lsm.addSelectionInterval(index, index);
                }
            } finally {
                updating = false;
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            if (opl == null) return;

            if (updating) return;
            updating = true;

            try {
                ListSelectionModel lsm = pan.offsetTable.getSelectionModel();
                if (lsm.isSelectionEmpty()) {
                    clearSelection();
                } else {
                    int min = lsm.getMinSelectionIndex();
                    int max = lsm.getMaxSelectionIndex();

                    final Set<Integer> selection = new TreeSet<Integer>();
                    for (int i=min; i<=max; ++i) {
                        if (lsm.isSelectedIndex(i)) selection.add(i);
                    }

                    OffsetPosSelection.select(selection).commit(editor.getNode());
                }
            } finally {
                updating = false;
            }
        }
    }


    // Position list being edited
    private OtItemEditor<? extends ISPNode, ? extends IOffsetPosListProvider<P>> editor;
    private OffsetPosList<P> opl;
    private IssPort port;
    private boolean editable;

    private final OffsetPosTablePanel pan;

    private final AbstractOffsetPosTableModel<P> tableModel;

    private final Action removeAction;
    private final Action removeAllAction;
    private final Action posToTopAction;
    private final Action posUpAction;
    private final Action posDownAction;
    private final Action posToBottomAction;

    private final SelectionWatcher selWatcher = new SelectionWatcher();

    protected AbstractOffsetPosTableEditor(OffsetPosTablePanel pan, AbstractOffsetPosTableModel<P> model) {
        this.pan = pan;
        this.tableModel = model;

        pan.newButton.setAction(createNewButtonAction());
        ButtonFlattener.flatten(pan.newButton);

        removeAction = createRemoveAction();
        pan.removeButton.setAction(removeAction);
        ButtonFlattener.flatten(pan.removeButton);

        removeAllAction = createRemoveAllAction();
        pan.removeAllButton.setAction(removeAllAction);
        ButtonFlattener.flatten(pan.removeAllButton);

        posToTopAction = createPosToTopAction();
        pan.topButton.setAction(posToTopAction);
        ButtonFlattener.flatten(pan.topButton);

        posUpAction = createPosUpAction();
        pan.upButton.setAction(posUpAction);
        ButtonFlattener.flatten(pan.upButton);

        posDownAction = createPosDownAction();
        pan.downButton.setAction(posDownAction);
        ButtonFlattener.flatten(pan.downButton);

        posToBottomAction = createPosToBottomAction();
        pan.bottomButton.setAction(posToBottomAction);
        ButtonFlattener.flatten(pan.bottomButton);

        pan.gridButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showOffsetGridDialog();
            }
        });

        pan.randomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showRandomDialog();
            }
        });

        pan.offsetTable.setModel(model);
        pan.offsetTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pan.offsetTable.getSelectionModel().addListSelectionListener(selWatcher);

        // I realize this isn't the right way to do these things...
        pan.offsetTable.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent event) {
            }

            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    // Make the delete and backspace buttons delete selected
                    // positions.
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        AbstractOffsetPosTableEditor.this.pan.removeButton.doClick();
                        break;
                    // Make the cmd-a button work on the mac
                    case KeyEvent.VK_A:
                        if (System.getProperty("os.name").toLowerCase().indexOf("mac") != -1) {
                            if (event.isMetaDown()) selectedAllPos();
                        }
                        break;
                }
            }

            public void keyReleased(KeyEvent event) {
            }
        });
    }

    protected List<P> getAllSelectedPos() {
        return OffsetPosSelection.apply(editor.getNode()).selectedPositions(opl);
    }

    protected void clearSelection() {
        OffsetPosSelection.EMPTY.commit(editor.getNode());
    }

    protected void select(P pos) {
        OffsetPosSelection.select(opl, pos).commit(editor.getNode());
    }

    protected void select(List<P> lst) {
        OffsetPosSelection.select(opl, lst).commit(editor.getNode());
    }

    protected void selectedAllPos() {
        OffsetPosSelection.selectAll(opl).commit(editor.getNode());
    }

    protected OffsetPosTablePanel getPan() {
        return pan;
    }

    protected void showOffsetGridDialog() {
        Window w = SwingUtilities.windowForComponent(AbstractOffsetPosTableEditor.this.pan);
        OffsetPosGrid.showDialog((Frame) w, opl, 10);
    }

    protected void showRandomDialog() {
        Window w = SwingUtilities.windowForComponent(AbstractOffsetPosTableEditor.this.pan);
        OffsetPosRandom.showDialog((Frame) w, opl, 10.);
    }


    protected IssPort getPort() {
        return port;
    }

    public void syncGuideState(Set<GuideProbe> referencedGuiders, Set<GuideProbe> availableGuiders, Set<GuideProbe> noPrimaryGuiders) {
        tableModel.syncGuideState(referencedGuiders, availableGuiders, noPrimaryGuiders);
        pan.repaint();
    }

//    protected void apply() { editor.apply(); }

    private Action createNewButtonAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                addPosition();
//                apply();
            }
        };

        res.putValue(Action.SHORT_DESCRIPTION, "Create a new offset position");
        res.putValue(Action.SMALL_ICON, Resources.getIcon("eclipse/add.gif"));

        return res;
    }

    protected void addPosition() {
        List<P> selList = getAllSelectedPos();
        P pos;
        if (selList.size() == 0) {
            pos = opl.addPosition(0);
        } else {
            int i = opl.getPositionIndex(selList.get(selList.size()-1));
            pos = opl.addPosition(i + 1);
        }
        select(pos);
    }

    private Action createRemoveAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                removeSelectedPositions();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/remove.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Remove selected offset position(s)");
        res.putValue(Action.SMALL_ICON, enableIcon);
        return res;
    }

    protected void removeSelectedPositions() {
        List<P> selList = getAllSelectedPos();
        if (selList.size() == 0) return;

        // Figure out what to select once we do the removal.
        P firstPos = selList.get(0);
        P lastPos  = selList.get(selList.size()-1);
        int after  = opl.getPositionIndex(lastPos) + 1;
        int before = opl.getPositionIndex(firstPos) - 1;
        P nextSel = (after < opl.size()) ? opl.getPositionAt(after) :
                    (before >= 0) ? opl.getPositionAt(before) : null;

        for (P pos : selList) opl.removePosition(pos);

        if (nextSel == null) {
            clearSelection();
        } else {
            select(nextSel);
        }
    }

    private Action createRemoveAllAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                removeAllPositions();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/remove_all.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Remove all offset position(s)");
        res.putValue(Action.SMALL_ICON, enableIcon);

        return res;
    }

    protected void removeAllPositions() {
        opl.removeAllPositions();
    }

    private Action createPosToTopAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                moveSelectedPositionsToTop();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/top.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Move selected position(s) to the top");
        res.putValue(Action.SMALL_ICON, enableIcon);

        return res;
    }

    protected void moveSelectedPositionsToTop() {
        List<P> selList = getAllSelectedPos();
        if (selList.size() == 0) return;

        Collections.reverse(selList);
        for (P op : selList) {
            opl.positionToFront(op);
        }
        select(selList);
    }

    private Action createPosUpAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                moveSelectedPositionsUp();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/up.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Move selected position(s) up");
        res.putValue(Action.SMALL_ICON, enableIcon);

        return res;
    }

    protected void moveSelectedPositionsUp() {
        List<P> selList = getAllSelectedPos();
        if (selList.size() == 0) return;

        for (P op : selList) {
            opl.decrementPosition(op);
        }
        select(selList);
    }

    private Action createPosDownAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                moveSelectedPositionsDown();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/down.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Move selected position(s) down");
        res.putValue(Action.SMALL_ICON, enableIcon);

        return res;
    }

    protected void moveSelectedPositionsDown() {
        List<P> selList = getAllSelectedPos();
        if (selList.size() == 0) return;

        Collections.reverse(selList);
        for (P op : selList) {
            opl.incrementPosition(op);
        }
        select(selList);
    }

    private Action createPosToBottomAction() {
        Action res = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                moveSelectedPositionsToBottom();
//                apply();
            }
        };

        Icon enableIcon = Resources.getIcon("eclipse/bottom.gif");
        res.putValue(Action.SHORT_DESCRIPTION, "Move selected position(s) to the bottom");
        res.putValue(Action.SMALL_ICON, enableIcon);

        return res;
    }

    protected void moveSelectedPositionsToBottom() {
        List<P> selList = getAllSelectedPos();
        if (selList.size() == 0) return;

        for (P op : selList) {
            opl.positionToBack(op);
        }
        select(selList);
    }

    protected void selectionUpdated() {
        final List<P> selList = OffsetPosSelection.apply(this.editor.getNode()).selectedPositions(opl);
        final int     listSize  = opl.size();
        final boolean listEmpty = (listSize == 0);
        final boolean selEmpty  = (selList.size() == 0);

        int firstSel = -1;
        int lastSel  = -1;
        if (!selEmpty) {
            firstSel = opl.getPositionIndex(selList.get(0));
            lastSel  = opl.getPositionIndex(selList.get(selList.size() - 1));
        }

        removeAction.setEnabled(editable && !selEmpty);
        removeAllAction.setEnabled(editable && !listEmpty);
        posToTopAction.setEnabled(editable && !selEmpty && (firstSel > 0));
        posUpAction.setEnabled(editable && posToTopAction.isEnabled());
        posDownAction.setEnabled(editable && !selEmpty && (lastSel < (listSize-1)));
        posToBottomAction.setEnabled(editable && posDownAction.isEnabled());
    }

    protected void selectTableRowsToMatchPosList(ListSelectionModel lsm, OffsetPosList<P> opl) {
        List<P> selList = getAllSelectedPos();
        if (selList.isEmpty()) {
            lsm.clearSelection();
            return;
        }

        for (P pos : selList) {
            int i = opl.getPositionIndex(pos);
            lsm.addSelectionInterval(i, i);
        }
    }

    private PropertyChangeListener actionUpdateSelWatcher = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            selectionUpdated();
        }
    };

    public void setPositionList(OtItemEditor<? extends ISPNode, ? extends IOffsetPosListProvider<P>> editor, IssPort port, boolean isEditable) {
        if (this.editor != null) {
            OffsetPosSelection.deafTo(this.editor.getNode(), selWatcher);
            OffsetPosSelection.deafTo(this.editor.getNode(), actionUpdateSelWatcher);
        }

        this.editor   = editor;
        this.port     = port;
        this.editable = isEditable;
        pan.offsetTable.getSelectionModel().removeListSelectionListener(selWatcher);

        opl = this.editor.getDataObject().getPosList();

        tableModel.setPositionList(this.editor.getNode(), opl);

        ListSelectionModel lsm = pan.offsetTable.getSelectionModel();
        selectTableRowsToMatchPosList(lsm, opl);
        pan.offsetTable.getSelectionModel().addListSelectionListener(selWatcher);

        OffsetPosSelection.listenTo(this.editor.getNode(), selWatcher);
        OffsetPosSelection.listenTo(this.editor.getNode(), actionUpdateSelWatcher);
        selectionUpdated();
    }

    protected OffsetPosList<P> getPositionList() {
        return opl;
    }
}
