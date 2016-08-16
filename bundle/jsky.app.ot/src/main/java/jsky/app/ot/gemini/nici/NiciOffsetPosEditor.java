//
// $
//

package jsky.app.ot.gemini.nici;

import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.spModel.gemini.nici.NiciOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosEditor;
import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Control code for the NICI offset position UI.
 */
public final class NiciOffsetPosEditor extends AbstractOffsetPosEditor<NiciOffsetPos> {
    private static final Logger LOG = Logger.getLogger(NiciOffsetPosEditor.class.getName());

    private NiciOffsetPosPanel ui;

    // A class that listens to changes in the "d" text box and updates the
    // distance along the arc to match.
    private class DTextWidgetListener extends AbstractDocumentListener {

        public void textChanged(DocumentEvent docEvent, String newText) {
            double newVal = 0.0;
            try {
                newVal = Double.valueOf(newText);
                if ((newVal < NiciOffsetPos.minD) || (newVal > NiciOffsetPos.maxD)) {
                    ui.getDOffsetTextBox().setForeground(Color.RED);
                } else {
                    ui.getDOffsetTextBox().setForeground(Color.BLACK);
                }
            } catch (Exception ex) {
                ui.getDOffsetTextBox().setForeground(Color.RED);
            }

            try {
                // Update the distance along the arc, which will cause the
                // p,q value to be calculated in the position
                List<NiciOffsetPos> selList;
                selList = getAllSelectedPos();
                for (NiciOffsetPos pos : selList) {
                    pos.deleteWatcher(posWatcher);
                    pos.setOffsetDistance(newVal, getIssPort());
                    pos.addWatcher(posWatcher);
                }

                // Show the new p value in the editor
                JTextField ptf = ui.getPOffsetTextBox();
                ptf.getDocument().removeDocumentListener(pWidgetListener);
                showDoubleValue(ptf, selList.get(0).getXaxis());
                ptf.getDocument().addDocumentListener(pWidgetListener);

                // Show the new q value in the editor
                JTextField qtf = ui.getQOffsetTextBox();
                qtf.getDocument().removeDocumentListener(qWidgetListener);
                showDoubleValue(qtf, selList.get(0).getYaxis());
                qtf.getDocument().addDocumentListener(qWidgetListener);

            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
    }

    // A class that listens to the focal plane mask wheel tracking buttons and
    // updates the follow state to match.
    private class FPMWListener implements ActionListener {
        private final boolean isTracking;

        FPMWListener(boolean isTracking) {
            this.isTracking = isTracking;
        }

        public void actionPerformed(ActionEvent e) {
            for (NiciOffsetPos pos : getAllSelectedPos()) {
                if (pos.isFpmwTracking() == isTracking) continue;
                pos.deleteWatcher(posWatcher);
                pos.setFpmwTacking(isTracking, getIssPort());
                pos.addWatcher(posWatcher);
            }
            selectionUpdated(getAllSelectedPos());
        }
    }

    private DoubleTextWidgetListener pWidgetListener = new DoubleTextWidgetListener(pSetter);
    private DoubleTextWidgetListener qWidgetListener = new DoubleTextWidgetListener(qSetter);
    private DTextWidgetListener dWidgetListener = new DTextWidgetListener();
    private FPMWListener followListener = new FPMWListener(true);
    private FPMWListener freezeListener = new FPMWListener(false);

    public NiciOffsetPosEditor(final NiciOffsetPosPanel ui) {
        super(ui);
        this.ui = ui;
        addListeners();

        TextFieldActionListener al = new TextFieldActionListener();
        ui.getDOffsetTextBox().addActionListener(al);
        ui.getPOffsetTextBox().addActionListener(al);
        ui.getQOffsetTextBox().addActionListener(al);

        // The min and max "d" is constrained.  If the user types in a value
        // out of range, then the display can be out of sync.  It can show a
        // number out of range that doesn't correspond with the p and q values.
        // So when the focus is lost on the d entry field, update the widgets
        // to reflect the actual values, particularly the "d".
        ui.getDOffsetTextBox().addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }
            public void focusLost(FocusEvent e) {
                updatePosWidgets(getAllSelectedPos());
                ui.getDOffsetTextBox().setForeground(Color.BLACK);
            }
        });
    }

    private void addListeners() {
        ui.getDOffsetTextBox().getDocument().addDocumentListener(dWidgetListener);
        ui.getPOffsetTextBox().getDocument().addDocumentListener(pWidgetListener);
        ui.getQOffsetTextBox().getDocument().addDocumentListener(qWidgetListener);
        ui.getFollowButton().addActionListener(followListener);
        ui.getFreezeButton().addActionListener(freezeListener);
    }

    private void removeListeners() {
        ui.getDOffsetTextBox().getDocument().removeDocumentListener(dWidgetListener);
        ui.getPOffsetTextBox().getDocument().removeDocumentListener(pWidgetListener);
        ui.getQOffsetTextBox().getDocument().removeDocumentListener(qWidgetListener);
        ui.getFollowButton().removeActionListener(followListener);
        ui.getFreezeButton().removeActionListener(freezeListener);
    }

    private void disableDPQ() {
        ui.getDOffsetTextBox().setText("");
        ui.getDOffsetTextBox().setEditable(false);

        ui.getPOffsetTextBox().setText("");
        ui.getPOffsetTextBox().setEditable(false);

        ui.getQOffsetTextBox().setText("");
        ui.getQOffsetTextBox().setEditable(false);
    }

    private void disableAll() {
        disableDPQ();

        ui.getMixedButton().setSelected(true);
        ui.getFollowButton().setEnabled(false);
        ui.getFreezeButton().setEnabled(false);
    }

    protected void selectionUpdated(List<NiciOffsetPos> selList) {
        super.selectionUpdated(selList);
        updatePosWidgets(selList);
    }

    enum FpmwTrackingMode {
        follow,
        freeze,
        none,
        ;

        static FpmwTrackingMode get(NiciOffsetPos pos) {
            return pos.isFpmwTracking() ? follow : freeze;
        }
    }

    private FpmwTrackingMode getSelectedTrackingMode(List<NiciOffsetPos> selList) {
        if (selList.size() == 0) return FpmwTrackingMode.none;

        FpmwTrackingMode mode = FpmwTrackingMode.get(selList.get(0));
        for (NiciOffsetPos pos : selList.subList(1, selList.size())) {
            if (mode != FpmwTrackingMode.get(pos)) {
                return FpmwTrackingMode.none;
            }
        }

        return mode;
    }

    private void showDoubleValue(JTextField tf, double val) {
//         tf.setText(String.format(NiciOffsetPosTableModel.PQ_FORMAT, val));
         tf.setText(Double.toString(val));
    }

    private void setOrientationLabel(FpmwTrackingMode mode) {
        String png = "mixedPos.png";
        switch (mode) {
            case follow:
                png = "d.png";
                break;
            case freeze:
                png = "pq.png";
                break;
        }
        ui.getOrientationLabel().setIcon(Resources.getIcon(png));
    }

    // Update the widgets used to edit the position based upon the currently
    // selected positions.
    private void updatePosWidgets(List<NiciOffsetPos> selList) {
        JTextField dtf = ui.getDOffsetTextBox();
        JTextField ptf = ui.getPOffsetTextBox();
        JTextField qtf = ui.getQOffsetTextBox();

        removeListeners();

        try {
            // If nothing selected, disable everything.
            if (selList.isEmpty()) {
                setOrientationLabel(FpmwTrackingMode.none);
                disableAll();
                return;
            }

            ui.getFollowButton().setEnabled(true);
            ui.getFreezeButton().setEnabled(true);

            // If the FPMW tracking mode differs across the selection, then
            // they'll have to pick a single mode or they can't edit d,p,q
            FpmwTrackingMode mode = getSelectedTrackingMode(selList);
            setOrientationLabel(mode);
            if (mode == FpmwTrackingMode.none) {
                disableDPQ();
                ui.getMixedButton().setSelected(true);
                return;
            }

            // Now we know we have a single tracking mode.  So we can edit
            // the d,p,q fields.
            if (mode == FpmwTrackingMode.follow) {
                ui.getFollowButton().setSelected(true);
                dtf.setEditable(true);
                ptf.setEditable(false);
                qtf.setEditable(false);
            } else {
                ui.getFreezeButton().setSelected(true);
                dtf.setEditable(false);
                ptf.setEditable(true);
                qtf.setEditable(true);
            }


            boolean dMixed = false;
            boolean pMixed = false;
            boolean qMixed = false;
            double d = selList.get(0).getOffsetDistance();
            double p = selList.get(0).getXaxis();
            double q = selList.get(0).getYaxis();

            for (NiciOffsetPos pos : selList.subList(1, selList.size())) {
                double dTmp = pos.getOffsetDistance();
                if (dTmp != d) dMixed = true;

                double pTmp = pos.getXaxis();
                if (pTmp != p) pMixed = true;

                double qTmp = pos.getYaxis();
                if (qTmp != q) qMixed = true;
            }

            boolean isFollow = (FpmwTrackingMode.follow == mode);

            if (!isFollow || dMixed) {
                dtf.setText("");
            } else {
                dtf.setText(Double.toString(d));
            }

            if (pMixed) {
                ptf.setText("");
            } else {
                showDoubleValue(ptf, p);
            }

            if (qMixed) {
                qtf.setText("");
            } else {
                showDoubleValue(qtf, q);
            }
        } finally {
            addListeners();
        }
    }

//    @Override public void setPositionList(OffsetPosList<NiciOffsetPos> posList, boolean editable) {
//        super.setPositionList(posList, editable);
//        pWidgetListener.setPositionList(posList);
//        qWidgetListener.setPositionList(posList);
//    }
}
