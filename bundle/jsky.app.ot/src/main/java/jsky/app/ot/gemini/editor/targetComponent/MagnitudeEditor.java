package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.MagnitudeSystem;
import edu.gemini.spModel.core.MagnitudeSystem$;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import jsky.app.ot.ui.util.FlatButtonUtil;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Component holding magnitude information for a target position.  Provides
 * controls for editing the magnitude values.
 */
public class MagnitudeEditor implements TelescopePosEditor {

    private static final DecimalFormat MAG_FORMAT = new DecimalFormat("0.0##");

    // Note -- synchronized though I guess this should always be called from
    // gui thread...
    public static synchronized String formatBrightness(Magnitude mag) {
        return MAG_FORMAT.format(mag.getBrightness());
    }

    /**
     * Whether simply editing existing magnitude values or working on adding
     * a new magnitude value.
     */
    enum Mode {
        add,
        edit,
        ;
    }

    /**
     * An interface for a row in the scrollable "table" or list of magnitude
     * values.  Provides access to the widgets in each row and a method to
     * accomodate the widgets to the current target.  There should be one
     * {@link MagEditRow} for each {@link Magnitude.Band} and a single
     * {@link jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor.MagNewRow}.
     */
    private interface MagWidgetRow {
        JButton getButton();
        Option<Magnitude.Band> getMagnitudeBand();
        Option<JComboBox<Magnitude.Band>> getBandCombo();
        Option<JComboBox<MagnitudeSystem>> getSystemCombo();
        Option<JTextField> getTextField();
        void setTarget(SPTarget target, Mode mode);
    }

    // A predicate that is used for filtering out magnitude values associated
    // with a particular band.
    private static final class NotBand implements PredicateOp<Magnitude> {
        private final Magnitude.Band band;
        NotBand(Magnitude.Band band) { this.band = band; }
        @Override public Boolean apply(Magnitude magnitude) {
            return !magnitude.getBand().equals(band);
        }
    }

    /**
     * Container for all the widgets used to edit a magnitude value.
     */
    private final class MagEditRow implements MagWidgetRow {
        private final Magnitude.Band band;
        private final JButton rmButton;
        private final JComboBox<Magnitude.Band> cb;
        private final JComboBox<MagnitudeSystem> systemCb;
        private final JFormattedTextField tf;

        // Action invoked when the remove button is pressed.  Removes the
        // associated magnitude value.
        private final ActionListener rmButtonAction = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                removeBand(band);
            }
        };

        // Action invoked when the magnitude band is edited.  Removes the
        // existing magnitude value associated with the band with which this
        // row was created and adds a new one that is identical but with the
        // new magnitude band.
        private final ActionListener changeBandAction = new ActionListener() {

            @Override public void actionPerformed(ActionEvent e) {
                final Magnitude.Band newBand = (Magnitude.Band) cb.getSelectedItem();
                if (newBand == null) return;
                if (newBand == MagEditRow.this.band) return;
                changeBand(MagEditRow.this.band, newBand);
                // OCSADV-355 If the bands is one on the AB list, switch the magnitude system to AB or else to VEGA
                changeSystem(newBand, newBand.defaultSystem);
            }
        };

        // Action invoked when the magnitude system is edited.  Removes the
        // existing magnitude value associated with the system with which this
        // row was created and adds a new one that is identical but with the
        // new magnitude system.
        private final ActionListener changeSystemAction = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final Magnitude.Band band = (Magnitude.Band) cb.getSelectedItem();
                if (band == null) return;
                final MagnitudeSystem system = (MagnitudeSystem) systemCb.getSelectedItem();
                if (system == null) return;
                changeSystem(band, system);
            }
        };

        // Action invoked when the brightness value itself is edited.
        private final PropertyChangeListener updateMagnitudeListener = new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                final double d;
                try {
                    final Number n = (Number) evt.getNewValue();
                    d = n.doubleValue();
                } catch (Exception ex) {
                    // do nothing
                    return;
                }
                updateMagnitudeValue(MagEditRow.this.band, d);
            }
        };


        MagEditRow(Magnitude.Band band) {
            this.band = band;
            rmButton = FlatButtonUtil.createSmallRemoveButton();
            rmButton.addActionListener(rmButtonAction);
            rmButton.setToolTipText("Remove magnitude value");

            cb = new JComboBox<Magnitude.Band>() {{
                setToolTipText("Set magnitude band");
            }};
            systemCb = new JComboBox<MagnitudeSystem>(MagnitudeSystem$.MODULE$.allForOTAsJava()) {{
                setRenderer((list, value,  index,  isSelected,  cellHasFocus)  -> new JLabel(value.name()));
                setToolTipText("Set magnitude system");
            }};

            final NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMinimumIntegerDigits(1);
            nf.setGroupingUsed(false);

            tf = new JFormattedTextField(nf) {{
                setColumns(5);
                ((DefaultFormatter) getFormatter()).setCommitsOnValidEdit(true);
                setToolTipText("Set brightness value");
                setMinimumSize(getPreferredSize());
            }};
        }

        public void setTarget(SPTarget target, Mode mode) {
            Option<Magnitude> magOpt = None.instance();
            if (target != null) magOpt = target.getMagnitude(band);

            // Update visibility based upon whether the target has a
            // corresponding magnitude value for this band.
            final boolean visible = !magOpt.isEmpty();
            rmButton.setVisible(visible);
            cb.setVisible(visible);
            systemCb.setVisible(visible);
            tf.setVisible(visible);
            if (!visible) return;

            // Update the combo-box options to show this band and exclude the
            // other used bands.
            final Set<Magnitude.Band> options = new TreeSet<>(Magnitude.Band.WAVELENGTH_COMPARATOR);
            options.addAll(Arrays.asList(Magnitude.Band.values()));
            //noinspection ConstantConditions
            options.removeAll(target.getTarget().getMagnitudeBands());
            options.add(band);
            cb.removeActionListener(changeBandAction);
            cb.setModel(new DefaultComboBoxModel<>(options.toArray(new Magnitude.Band[options.size()])));
            cb.setSelectedItem(band);
            cb.setMaximumRowCount(options.size());
            cb.addActionListener(changeBandAction);

            systemCb.removeActionListener(changeSystemAction);
            systemCb.setSelectedItem(magOpt.getValue().getSystem());
            systemCb.addActionListener(changeSystemAction);

            // Update the text field to show the magnitude value.
            tf.removePropertyChangeListener("value", updateMagnitudeListener);
            tf.setText(formatBrightness(magOpt.getValue()));
            tf.addPropertyChangeListener("value", updateMagnitudeListener);
        }

        @Override public JButton getButton() { return rmButton; }

        @Override
        public Option<Magnitude.Band> getMagnitudeBand() {
            return new Some<>(band);
        }

        @Override public Option<JComboBox<Magnitude.Band>> getBandCombo() { return new Some<>(cb); }
        @Override public Option<JComboBox<MagnitudeSystem>> getSystemCombo() { return new Some<>(systemCb); }
        @Override public Option<JTextField> getTextField() { return new Some<>(tf); }
    }

    /**
     * Container for all the widgets used to add a new magnitude value.
     */
    private final class MagNewRow implements MagWidgetRow {
        private final JButton rmButton;
        private final JTextField tf;
        private final JComboBox<Magnitude.Band> cb;


        // Action invoked when the combo box is used to select a magnitude
        // value.  Adds a new Magnitude object initialized with a super-bright
        // value of "0"...
        private final ActionListener addAction = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final Magnitude.Band newBand = (Magnitude.Band) cb.getSelectedItem();
                if (newBand == null) return;
                addBand(newBand);
            }
        };

        MagNewRow() {
            rmButton = FlatButtonUtil.createSmallRemoveButton();
            rmButton.addActionListener(e -> cancelAdd());
            rmButton.setToolTipText("Stop adding a new magnitude value");

            // The text field is essentially a prompt.  It points to the
            // passband combo box.  It is never editable but appears to become
            // editable after the user selects a passband and the MagAddRow
            // is replaced with a MagEditRow.  \u2192 is an right pointing arrow
            tf = new JTextField("Select \u2192") {
                {
                    setColumns(5);
                    setEnabled(false);
                    setMinimumSize(getPreferredSize());
                }
                @Override public void setEnabled(boolean enabled) {
                    super.setEnabled(false); // never enable
                }
            };
            cb = new JComboBox<Magnitude.Band>() {{
                setToolTipText("Set passband for the new magnitude value");
            }};
        }

        public void setTarget(SPTarget target, Mode mode) {
            final boolean visible = (target != null) && mode == Mode.add;

            rmButton.setVisible(visible);
            tf.setVisible(visible);
            cb.setVisible(visible);
            if (!visible) return;

            // Make the remove button active if there are existing magnitude
            // values.
            rmButton.setEnabled(target.getMagnitudes().size()>0);

            // Update the combo-box options to contain the unused magnitude
            // bands and show that nothing is selected.
            final Set<Magnitude.Band> options = new TreeSet<>(Magnitude.Band.WAVELENGTH_COMPARATOR);
            options.addAll(Arrays.asList(Magnitude.Band.values()));
            options.removeAll(target.getTarget().getMagnitudeBands());
            cb.setMaximumRowCount(options.size());
            cb.removeActionListener(addAction);
            cb.setModel(new DefaultComboBoxModel<>(options.toArray(new Magnitude.Band[options.size()])));
            cb.setSelectedItem(null);
            cb.addActionListener(addAction);
        }

        @Override public JButton getButton() { return rmButton; }

        @Override public Option<Magnitude.Band> getMagnitudeBand() {
            return None.instance();
        }

        @Override public Option<JComboBox<Magnitude.Band>> getBandCombo() { return new Some<>(cb); }
        @Override public Option<JComboBox<MagnitudeSystem>> getSystemCombo() { return None.instance(); }
        @Override public Option<JTextField> getTextField() { return new Some<>(tf); }
    }

    /**
     * Container for all the widgets used to house the + button
     */
    private final class MagPlusRow implements MagWidgetRow {
        private final JButton addButton;

        MagPlusRow() {
            addButton = FlatButtonUtil.createSmallAddButton();
            addButton.addActionListener(e -> enableAdd());
            addButton.setToolTipText("Add a new magnitude value");
        }

        public void setTarget(SPTarget target, Mode mode) {
            final boolean visible = (target != null) && mode == Mode.edit;
            addButton.setVisible(visible);
        }

        @Override public JButton getButton() { return addButton; }
        @Override public Option<JComboBox<Magnitude.Band>> getBandCombo() { return None.instance(); }
        @Override public Option<JComboBox<MagnitudeSystem>> getSystemCombo() { return None.instance(); }
        @Override public Option<JTextField> getTextField() { return None.instance(); }

        @Override public Option<Magnitude.Band> getMagnitudeBand() {
            return None.instance();
        }
    }

    private final JPanel pan;
    private final JScrollPane scroll;
    private final ImList<MagWidgetRow> rows;
    private final MagNewRow newRow;

    private final TelescopePosWatcher watcher = tp -> reinit((SPTarget)tp);

    private SPTarget target = null;

    public MagnitudeEditor() {
        pan = new JPanel(new GridBagLayout());

        // Create the panel that will hold the magnitude editing widgets.
        final JPanel content = new JPanel(new GridBagLayout()) {{
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        }};

        // Create the rows needed to edit magnitude information.
        final List<Magnitude.Band> bandList = Arrays.asList(Magnitude.Band.values());
        Collections.sort(bandList, Magnitude.Band.WAVELENGTH_COMPARATOR);
        newRow = new MagNewRow();
        rows = DefaultImList.create(bandList).map(
                (MapOp<Magnitude.Band, MagWidgetRow>) MagEditRow::new
        ).append(newRow).append(new MagPlusRow());

        // Place them in the content panel.
        rows.zipWithIndex().foreach(tup -> {
            final MagWidgetRow row = tup._1();
            final Integer  y = tup._2();
            content.add(row.getButton(), new GridBagConstraints() {{
                gridx=0; gridy=y; insets=new Insets(0, 0, 5, 5); fill=VERTICAL;
            }});

            final Option<JTextField> tf = row.getTextField();
            if (!tf.isEmpty()) {
                content.add(tf.getValue(), new GridBagConstraints() {{
                    gridx=1; gridy=y; insets=new Insets(0, 0, 5, 5);
                }});
            }

            final Option<JComboBox<Magnitude.Band>> cb = row.getBandCombo();
            if (!cb.isEmpty()) {
                content.add(cb.getValue(), new GridBagConstraints() {{
                    gridx=2; gridy=y; insets=new Insets(0, 0, 5, 5); fill=HORIZONTAL;
                }});
            }

            final Option<JComboBox<MagnitudeSystem>> system = row.getSystemCombo();
            if (!system.isEmpty()) {
                content.add(system.getValue(), new GridBagConstraints() {{
                    gridx=3; gridy=y; insets=new Insets(0, 0, 5, 0); fill=HORIZONTAL;
                }});
            }
        });

        // Add a panel to push everything to the top right of the content panel
        content.add(new JPanel(), new GridBagConstraints() {{
            gridx=10; gridy=rows.size(); weightx=1.0; weighty=1.0; fill=BOTH;
        }});

        // Wrap the content in a scroll pane.
        scroll = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {{
            setMaximumSize(new Dimension(215, 1));
            setPreferredSize(new Dimension(215, 1));
            setMinimumSize(new Dimension(215, 1));
            setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        }};

        pan.add(scroll, new GridBagConstraints() {{
            gridx=0; gridy=0; fill=VERTICAL; weightx=0.0; weighty=1.0; anchor=WEST;
        }});
    }

    public Component getComponent() {
        return pan;
    }

    public void edit(final Option<ObsContext> ctx, final SPTarget target, ISPNode node) {
        if (this.target == target) return;
        if (this.target != null) this.target.deleteWatcher(watcher);
        if (target != null) target.addWatcher(watcher);

        reinit(target);
    }

    private void reinit(SPTarget target) {
        // Figure out whether to use "edit" mode or "add" mode.  By default,
        // we just want to edit the magnitude info that is already there.
        // If there is no magnitude info though, switch to add mode so that
        // the user doesn't have to push the + button.
        Mode mode = Mode.edit;
        if ((target != null) && (target.getMagnitudes().size() == 0)) {
            mode = Mode.add;
        }
        reinit(target, mode);
    }

    /**
     * Initializes the widgets to edit this target.  Turns off widgets for
     * editing magnitude bands that don't exist in this target, turns on and
     * initializes those that do exist.
     *
     * @param target target to use for the initialization (if any)
     * @param mode whether to show the widgets for adding a new magnitude band
     */
    private void reinit(final SPTarget target, final Mode mode) {
        this.target = target;

        rows.foreach(row -> row.setTarget(target, mode));

        if (mode == Mode.add) {
            // Scroll to the bottom to show the new row in the scroll pane, but
            // don't do it in this event cycle.  Wait until this event has
            // finished executing so that the widgets for adding a new magnitude
            // value are visible.
            SwingUtilities.invokeLater(() -> {
                final JScrollBar sb = scroll.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());

                if (target.getMagnitudes().size() > 0) {
                    newRow.getBandCombo().getValue().requestFocusInWindow();
                }
            });
        }

        pan.getParent().repaint();
    }

    private void focusOn(final Magnitude.Band b) {
        final Option<MagWidgetRow> row = rows.find(tmp -> {
            final Option<Magnitude.Band> tmpBand = tmp.getMagnitudeBand();
            return !tmpBand.isEmpty() && tmpBand.getValue() == b;
        });
        if (!row.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                if (pan.isVisible()) {
                    final JTextField tf = row.getValue().getTextField().getValue();
                    tf.requestFocusInWindow();
                }
            });
        }
    }

    public void replaceMagnitudes(ImList<Magnitude> magnitudes) {
        target.setMagnitudes(magnitudes);
    }

    // Editing operations
    void cancelAdd() {
        reinit(target);
    }

    void enableAdd() {
        reinit(target, Mode.add);
    }

    void addBand(Magnitude.Band b) {
        final Option<Magnitude> magOpt = target.getMagnitude(b);
        if (!magOpt.isEmpty()) return; // shouldn't happen ...

        final Magnitude newMag = new Magnitude(b, Magnitude.UNDEFINED_MAG, 0, b.defaultSystem);

        target.setMagnitudes(target.getMagnitudes().cons(newMag));
        focusOn(b);
    }

    void removeBand(Magnitude.Band b) {
        target.setMagnitudes(target.getMagnitudes().filter(new NotBand(b)));
    }

    void changeBand(Magnitude.Band from, Magnitude.Band to) {
        final Option<Magnitude> oldMagOpt = target.getMagnitude(from);
        if (oldMagOpt.isEmpty()) return; // shouldn't happen ...
        final Magnitude oldMag = oldMagOpt.getValue();
        final Magnitude newMag = new Magnitude(to, oldMag.getBrightness(), oldMag.getError(), oldMag.getSystem());

        target.setMagnitudes(
               target.getMagnitudes().filter(new NotBand(from)).cons(newMag)
        );
        focusOn(to);
    }

    void changeSystem(Magnitude.Band band, MagnitudeSystem system) {
        final Option<Magnitude> oldMagOpt = target.getMagnitude(band);
        if (oldMagOpt.isEmpty()) return; // shouldn't happen ...
        final Magnitude oldMag = oldMagOpt.getValue();
        if (system == oldMag.getSystem()) return;
        final Magnitude newMag = new Magnitude(band, oldMag.getBrightness(), oldMag.getError(), system);

        target.setMagnitudes(
               target.getMagnitudes().filter(new NotBand(band)).cons(newMag)
        );
        focusOn(band);
    }

    void updateMagnitudeValue(Magnitude.Band b, double d) {
        final Option<Magnitude> oldMagOpt = target.getMagnitude(b);
        if (oldMagOpt.isEmpty()) return;
        final Magnitude oldMag = oldMagOpt.getValue();

        try {
            final Magnitude newMag = new Magnitude(oldMag.getBand(), d, oldMag.getError(), oldMag.getSystem());
            target.deleteWatcher(watcher);
            target.setMagnitudes(
                   target.getMagnitudes().filter(new NotBand(b)).cons(newMag)
            );
            target.addWatcher(watcher);
        } catch (Exception ex) {
            // do nothing
        }
    }
}
