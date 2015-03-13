//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.system.HmsDegTarget;
import edu.gemini.spModel.target.system.ITarget;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;

/**
 * An editor for proper motion.
 */
public final class ProperMotionEditor implements TelescopePosEditor {
    private final JPanel pan;

    private final JFormattedTextField pmRa;
    private final JFormattedTextField pmDec;

    private SPTarget target;

    public ProperMotionEditor() {
        pan = new JPanel(new GridBagLayout()) {{
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }};

        // Create two text fields, one for PM in RA and one for PM in dec
        pmRa  = createTextField("Proper motion in RA");
        pmDec = createTextField("Proper motion in declination");

        // A list of tuples of label and widget
        final ImList<Pair<String,JFormattedTextField>> items =
                DefaultImList.create(
                        new Pair<>("RA", pmRa),
                        new Pair<>("Dec", pmDec)
                );

        // Place the items in the panel
        items.zipWithIndex().foreach(new ApplyOp<Tuple2<Pair<String,JFormattedTextField>, Integer>>() {
            @Override public void apply(Tuple2<Pair<String,JFormattedTextField>, Integer> tup) {
                // Index -- the gridy value.
                final int y = tup._2();

                // Gap to leave below widgets -- only for the first row.
                final int vpad = (y==0) ? 5 : 0;

                // Label
                pan.add(new JLabel(tup._1()._1()), new GridBagConstraints(){{
                    gridx=0; gridy=y; anchor=EAST; insets=new Insets(0, 0, vpad, 5);
                }});
                // Text Field widget
                pan.add(tup._1()._2(), new GridBagConstraints(){{
                    gridx=1; gridy=y; anchor=WEST; insets=new Insets(0, 0, vpad, 5);
                }});
                // Units label
                pan.add(new JLabel("mas/year"), new GridBagConstraints(){{
                    gridx=2; gridy=y; anchor=WEST; insets=new Insets(0, 0, vpad, 0);
                }});
            }
        });
    }

    private static JFormattedTextField createTextField(final String tip) {
        final NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);
        nf.setGroupingUsed(false);

        return new JFormattedTextField(nf) {{
            setColumns(5);
            ((DefaultFormatter) getFormatter()).setCommitsOnValidEdit(true);
            setToolTipText(tip);
            setMinimumSize(getPreferredSize());
        }};
    }

    public Component getComponent() {
        return pan;
    }

    private final TelescopePosWatcher watcher = new TelescopePosWatcher() {
        @Override public void telescopePosUpdate(WatchablePos tp) {
            reinit();
        }
    };

    private final PropertyChangeListener updatePmRaListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            try {
                final Number d = (Number) evt.getNewValue();
                target.deleteWatcher(watcher);
                final ITarget it = target.getTarget();
                if (it instanceof HmsDegTarget) {
                    ((HmsDegTarget) it).setPropMotionRA(d == null ? 0.0 : d.doubleValue());
                }
                target.notifyOfGenericUpdate(); // someone else may be watching
                target.addWatcher(watcher);
            } catch (Exception ex) {
                // do nothing
            }
        }
    };

    private final PropertyChangeListener updatePmDecListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            try {
                final Number d = (Number) evt.getNewValue();
                target.deleteWatcher(watcher);
                final ITarget it = target.getTarget();
                if (it instanceof HmsDegTarget) {
                    ((HmsDegTarget) it).setPropMotionDec(d == null ? 0 : d.doubleValue());
                }
                target.notifyOfGenericUpdate(); // someone else may be watching
                target.addWatcher(watcher);
            } catch (Exception ex) {
                // do nothing
            }
        }
    };

    public void edit(final Option<ObsContext> ctx, final SPTarget target) {
        if (this.target == target) return;
        if (this.target != null) this.target.deleteWatcher(watcher);
        if (target != null) target.addWatcher(watcher);

        this.target = target;
        reinit();
    }

    private void reinit() {
        pmRa.removePropertyChangeListener("value", updatePmRaListener);
        pmDec.removePropertyChangeListener("value", updatePmDecListener);
        final ITarget it = target == null ? null : target.getTarget();
        if (it instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget) target.getTarget();
            pmRa.setText(Double.toString(t.getPropMotionRA()));
            pmDec.setText(Double.toString(t.getPropMotionDec()));
        } else {
            pmRa.setText("0.0");
            pmDec.setText("0.0");
        }
        pmRa.addPropertyChangeListener("value", updatePmRaListener);
        pmDec.addPropertyChangeListener("value", updatePmDecListener);
    }
}
