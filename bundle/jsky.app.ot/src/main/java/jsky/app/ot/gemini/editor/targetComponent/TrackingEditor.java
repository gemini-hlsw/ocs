//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * Telescope editor for sidereal target tracking details.
 */
final class TrackingEditor implements TelescopePosEditor {

    private final JPanel pan;
    private final ImList<Row> rows;

    private final TelescopePosWatcher watcher = new TelescopePosWatcher() {
        @Override public void telescopePosLocationUpdate(WatchablePos tp) { }
        @Override public void telescopePosGenericUpdate(WatchablePos tp) {
            reinit();
        }
    };

    private interface Row {
        String getLabel();
        JComponent getComponent();
        Option<String> getUnits();

        void reinit();
    }

    private abstract class AbstractRow implements Row {
        private final String label;
        private final Option<String> units;

        AbstractRow(String label) {
            this.label = label;
            this.units = None.instance();
        }

        AbstractRow(String label, String units) {
            this.label = label;
            this.units = new Some<String>(units);
        }

        public String getLabel() { return label; }
        public Option<String> getUnits() { return units; }
    }

    private static interface UpdateFunction {
        void apply(SPTarget target, Number d);
    }

    private static interface InitFunction {
        void apply(SPTarget target, JFormattedTextField field);
    }

    private final class NumberFieldListener implements PropertyChangeListener {
        private final UpdateFunction function;

        NumberFieldListener(UpdateFunction function) {
            this.function = function;
        }

        @Override public void propertyChange(PropertyChangeEvent evt) {
            target.deleteWatcher(watcher);
            Number d = (Number) evt.getNewValue();
            function.apply(target, d);
            target.addWatcher(watcher);
        }
    }

    private final class NumberRow extends AbstractRow {
        private final InitFunction init;
        private final JFormattedTextField field;
        private final PropertyChangeListener listener;

        NumberRow(String label, String units, InitFunction init, UpdateFunction update) {
            super(label, units);
            this.init = init;
            listener = new NumberFieldListener(update);

            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMinimumIntegerDigits(1);
            nf.setGroupingUsed(false);

            field = new JFormattedTextField(nf) {{
                setColumns(5);
                ((DefaultFormatter) getFormatter()).setCommitsOnValidEdit(true);
            }};
        }

        public JComponent getComponent() { return field; }

        public void reinit() {
            field.removePropertyChangeListener("value", listener);
            init.apply(target, field);
            field.addPropertyChangeListener("value", listener);
        }
    }

    private SPTarget target;

    TrackingEditor() {
        pan = new JPanel(new GridBagLayout()) {{
            setOpaque(false);
        }};

        rows = DefaultImList.create(
            (Row) new NumberRow("Epoch", "years",
                    new InitFunction() {
                        @Override public void apply(SPTarget target, JFormattedTextField field) {
                            field.setText(Double.toString(target.getTrackingEpoch()));
                        }
                    },
                    new UpdateFunction() {
                        @Override public void apply(SPTarget target, Number d) {
                            target.setTrackingEpoch(d == null ? 2000.0 : d.doubleValue());
                        }
                    }),
            new NumberRow("Parallax", "arcsec",
                    new InitFunction() {
                        @Override public void apply(SPTarget target, JFormattedTextField field) {
                            field.setText(Double.toString(target.getTrackingParallax()));
                        }
                    },
                    new UpdateFunction() {
                        @Override public void apply(SPTarget target, Number d) {
                            target.setTrackingParallax(d == null ? 0.0 : d.doubleValue());
                        }
                    }),
            new NumberRow("Radial Vel", "km/sec",
                    new InitFunction() {
                        @Override public void apply(SPTarget target, JFormattedTextField field) {
                            field.setText(Double.toString(target.getTrackingRadialVelocity()));
                        }
                    },
                    new UpdateFunction() {
                        @Override public void apply(SPTarget target, Number d) {
                            target.setTrackingRadialVelocity(d == null ? 0.0 : d.doubleValue());
                        }
                    })
        );

        rows.zipWithIndex().foreach(new ApplyOp<Tuple2<Row, Integer>>() {
            @Override public void apply(Tuple2<Row, Integer> tup) {
                final Row row = tup._1();
                final int y   = tup._2();

                final int vgap = (y == rows.size()-1) ? 0 : 5;

                pan.add(new JLabel(row.getLabel()), new GridBagConstraints() {{
                    gridx=0; gridy=y; anchor=EAST; insets=new Insets(0, 0, vgap, 5);
                }});

                pan.add(row.getComponent(), new GridBagConstraints() {{
                    gridx=1; gridy=y; anchor=WEST; fill=HORIZONTAL; weightx=1.0; insets=new Insets(0, 0, vgap, 0);
                }});

                if (!row.getUnits().isEmpty()) {
                    String units = row.getUnits().getValue();
                    pan.add(new JLabel(units), new GridBagConstraints() {{
                        gridx=2; gridy=y; anchor=WEST; insets=new Insets(0, 5, vgap, 0);
                    }});
                }
            }
        });

        // push everything to the top
        pan.add(new JPanel() {{ setOpaque(false); }}, new GridBagConstraints() {{
            gridx=0; gridy=rows.size(); fill=HORIZONTAL; weighty=1.0;
        }});
    }

    @Override
    public Component getComponent() {
        return pan;
    }

    @Override
    public void edit(final Option<ObsContext> ctx, final SPTarget target) {
        if (this.target == target) return;
        if (this.target != null) this.target.deleteWatcher(watcher);
        if (target != null) target.addWatcher(watcher);

        this.target = target;
        reinit();
    }

    private void reinit() {
        rows.foreach(new ApplyOp<Row>() {
            @Override public void apply(Row row) { row.reinit(); }
        });

    }
}
