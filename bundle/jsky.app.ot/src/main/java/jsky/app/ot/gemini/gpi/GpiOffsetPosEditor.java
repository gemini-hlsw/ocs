package jsky.app.ot.gemini.gpi;

import edu.gemini.spModel.gemini.gpi.GpiOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosEditor;
import jsky.util.gui.Resources;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.util.List;

/**
 * Control code for the GPI offset position UI.
 */
public final class GpiOffsetPosEditor extends AbstractOffsetPosEditor<GpiOffsetPos> {

    // Used to make sure values are in range (-1, 1)
    protected class RangeCheckingDoubleTextWidgetListener extends DoubleTextWidgetListener {
        public RangeCheckingDoubleTextWidgetListener(PosValueSetter<GpiOffsetPos, Double> setter) {
            super(setter);
        }

        public void textChanged(DocumentEvent docEvent, String newText) {
            double newVal;
            try {
                newVal = Double.valueOf(newText);
                if (newVal> 1.0) {
                    newText = "1.0";
                } else if (newVal < -1.0) {
                    newText = "-1.0";
                }
            } catch (Exception ex) {
                // ignore
            }
            super.textChanged(docEvent, newText);
        }
    }

    private final GpiOffsetPosPanel ui;

    private final DoubleTextWidgetListener pWidgetListener = new RangeCheckingDoubleTextWidgetListener(pSetter);
    private final DoubleTextWidgetListener qWidgetListener = new RangeCheckingDoubleTextWidgetListener(qSetter);

    public GpiOffsetPosEditor(final GpiOffsetPosPanel ui) {
        super(ui);
        this.ui = ui;
        addListeners();

        TextFieldActionListener al = new TextFieldActionListener();
        ui.getPOffsetTextBox().addActionListener(al);
        ui.getQOffsetTextBox().addActionListener(al);
        setOrientationLabel();
    }

    private void addListeners() {
        ui.getPOffsetTextBox().getDocument().addDocumentListener(pWidgetListener);
        ui.getQOffsetTextBox().getDocument().addDocumentListener(qWidgetListener);
    }

    private void removeListeners() {
        ui.getPOffsetTextBox().getDocument().removeDocumentListener(pWidgetListener);
        ui.getQOffsetTextBox().getDocument().removeDocumentListener(qWidgetListener);
    }

    private void showDoubleValue(JTextField tf, double val) {
         tf.setText(Double.toString(val));
    }

    private void setOrientationLabel() {
        String png = "xy.png";
        ui.getOrientationLabel().setIcon(Resources.getIcon(png));
    }


    // Update the widgets used to edit the position based upon the currently
    // selected positions.
    private void updatePosWidgets(List<GpiOffsetPos> selList) {
        JTextField ptf = ui.getPOffsetTextBox();
        JTextField qtf = ui.getQOffsetTextBox();

        removeListeners();

        try {
            if (selList.isEmpty()) {
                ptf.setEditable(false);
                qtf.setEditable(false);
                return;
            }
            ptf.setEditable(true);
            qtf.setEditable(true);
            showDoubleValue(ptf, selList.get(0).getXaxis());
            showDoubleValue(qtf, selList.get(0).getYaxis());
        } finally {
            addListeners();
        }
    }

    protected void selectionUpdated(List<GpiOffsetPos> selList) {
        super.selectionUpdated(selList);
        updatePosWidgets(selList);
    }
}
