//
// $
//

package jsky.app.ot.gemini.editor.offset;

import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.spModel.target.offset.OffsetPosBase;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import jsky.util.gui.Resources;

import javax.swing.*;


/**
 * Control code for the {@link OffsetPosUI}.
 */
public final class DefaultOffsetPosEditor<P extends OffsetPosBase> extends AbstractOffsetPosEditor<P> {
    private static final Logger LOG = Logger.getLogger(DefaultOffsetPosEditor.class.getName());

    private final DoubleTextWidgetListener pWidgetListener = new DoubleTextWidgetListener(pSetter);
    private final DoubleTextWidgetListener qWidgetListener = new DoubleTextWidgetListener(qSetter);

    public DefaultOffsetPosEditor(OffsetPosUI ui) {
        super(ui);

        ui.getPOffsetTextBox().getDocument().addDocumentListener(pWidgetListener);
        ui.getQOffsetTextBox().getDocument().addDocumentListener(qWidgetListener);
        ui.getOrientationLabel().setIcon(Resources.getIcon("pq.png"));

        final TextFieldActionListener al = new TextFieldActionListener();
        ui.getPOffsetTextBox().addActionListener(al);
        ui.getQOffsetTextBox().addActionListener(al);
    }

    private final MapOp<P, Double> GET_P = new MapOp<P, Double>() {
        @Override public Double apply(P p) { return p.getXaxis(); }
    };
    private final MapOp<P, Double> GET_Q = new MapOp<P, Double>() {
        @Override public Double apply(P p) { return p.getYaxis(); }
    };

    protected void selectionUpdated(List<P> selList) {
        super.selectionUpdated(selList);
        updateDoubleTextWidgets(selList, getEditorUI().getPOffsetTextBox(), pWidgetListener, GET_P);
        updateDoubleTextWidgets(selList, getEditorUI().getQOffsetTextBox(), qWidgetListener, GET_Q);
    }

    private void updateDoubleTextWidgets(List<P> selList, JTextField widget, DoubleTextWidgetListener listener, MapOp<P, Double> get) {
        widget.getDocument().removeDocumentListener(listener);
        try {
            if (selList.size() == 0) {
                widget.setText("");
                widget.setEditable(false);
                return;
            }

            boolean mixed = false;
            widget.setEditable(true);

            final double d = get.apply(selList.get(0));
            for (P p : selList.subList(1, selList.size())) {
                double tmp = get.apply(p);
                if (d != tmp) {
                    mixed = true;
                    break;
                }
            }
            if (mixed) {
                widget.setText("");
            } else {
                widget.setText(Double.toString(d));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } finally {
            widget.getDocument().addDocumentListener(listener);
        }
    }
}
