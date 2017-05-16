package jsky.app.ot.gemini.editor.sitequality;

import edu.gemini.shared.gui.ButtonFlattener;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.*;
import jsky.app.ot.editor.type.SpTypeUIUtil;
import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class SiteQualityPanel extends JPanel {

    private final EdCompSiteQuality owner;
    private JFormattedTextField elevMin, elevMax;

    SiteQualityPanel(final EdCompSiteQuality owner) {
        this.owner = owner;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JPanel(new GridBagLayout()) {{

            add(new JLabel("Sky Background:"), new GBC(0, 0));
            add(new JComboBox() {{
                SpTypeUIUtil.initListBox(this, SPSiteQuality.SkyBackground.class, e -> owner.getDataObject().setSkyBackground((SkyBackground)getSelectedItem()));
                owner.addPropertyChangeListener(e -> getModel().setSelectedItem(owner.getDataObject().getSkyBackground()));
            }}, new GBC(1, 0));

            add(new JLabel("Cloud Cover:"), new GBC(0, 1));
            add(new JComboBox() {{
                SpTypeUIUtil.initListBox(this, SPSiteQuality.CloudCover.class, e -> owner.getDataObject().setCloudCover((CloudCover)getSelectedItem()));
                owner.addPropertyChangeListener(e -> getModel().setSelectedItem(owner.getDataObject().getCloudCover()));
            }}, new GBC(1, 1));

            add(new JLabel("Image Quality:"), new GBC(0, 2));
            add(new JComboBox() {{
                SpTypeUIUtil.initListBox(this, SPSiteQuality.ImageQuality.class, e -> owner.getDataObject().setImageQuality((ImageQuality)getSelectedItem()));
                owner.addPropertyChangeListener(e -> getModel().setSelectedItem(owner.getDataObject().getImageQuality()));
            }}, new GBC(1, 2));

            add(new JLabel("Water Vapor:"), new GBC(0, 3));
            add(new JComboBox() {{
                SpTypeUIUtil.initListBox(this, SPSiteQuality.WaterVapor.class, e -> owner.getDataObject().setWaterVapor((WaterVapor)getSelectedItem()));
                owner.addPropertyChangeListener(e -> getModel().setSelectedItem(owner.getDataObject().getWaterVapor()));
            }}, new GBC(1, 3));

            // spacer
            add(new JPanel(), new GBC(0, 4, new Insets(10, 0, 0, 0)));

            final JLabel units = new JLabel() {{
                setPreferredSize(new Dimension(100, 0));
            }};

            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMinimumIntegerDigits(1);


            final JLabel warning = new JLabel(" ");
            warning.setForeground(Color.RED);

            elevMin = new JFormattedTextField(nf) {{
                setColumns(5);
                setEnabled(false);
                addPropertyChangeListener("value", evt -> {
                    try {
                        Number d = (Number) evt.getNewValue();
                        if (d != null) owner.getDataObject().setElevationConstraintMin(d.doubleValue());
                        warning.setText(getElevationWarning(owner.getDataObject()));
                    } catch (NumberFormatException nfe) {
                        //do nothing
                    }
                });
                ((DefaultFormatter) getFormatter()).setCommitsOnValidEdit(true);
            }};

            elevMax = new JFormattedTextField(nf) {{
                setColumns(5);
                setEnabled(false);
                addPropertyChangeListener("value", evt -> {
                    try {
                        Number d = (Number) evt.getNewValue();
                        if (d != null) owner.getDataObject().setElevationConstraintMax(d.doubleValue());
                        warning.setText(getElevationWarning(owner.getDataObject()));
                    } catch (NumberFormatException nfe) {
                        //do nothing
                    }
                });
                ((DefaultFormatter) getFormatter()).setCommitsOnValidEdit(true);
            }};

            add(new JLabel("Elevation Constraint:"), new GBC(0, 5));
            add(new JComboBox() {
                boolean pushing = false;
                {
                SpTypeUIUtil.initListBox(this, SPSiteQuality.ElevationConstraintType.class, e -> {
                    ElevationConstraintType chosenType = (ElevationConstraintType)getSelectedItem();
                    switch (chosenType) {
                    case AIRMASS: units.setText("airmass");
                        elevMin.setEnabled(true); if (!pushing) elevMin.setValue(chosenType.getDefaultMin());
                        elevMax.setEnabled(true); if (!pushing) elevMax.setValue(chosenType.getDefaultMax());
                        break;
                    case HOUR_ANGLE:
                        units.setText("hours");
                        elevMin.setEnabled(true); if (!pushing) elevMin.setValue(chosenType.getDefaultMin());
                        elevMax.setEnabled(true); if (!pushing) elevMax.setValue(chosenType.getDefaultMax());
                        break;
                    case NONE:
                        units.setText("");
                        elevMin.setEnabled(false); elevMin.setValue(null);
                        elevMax.setEnabled(false); elevMax.setValue(null);
                        break;
                    }
                    owner.getDataObject().setElevationConstraintType(chosenType);
                    warning.setText(getElevationWarning(owner.getDataObject()));
                });
                owner.addPropertyChangeListener(e -> {
                    ElevationConstraintType ect = owner.getDataObject().getElevationConstraintType();
                    pushing = true;
                    getModel().setSelectedItem(ect);
                    pushing = false;
                    if (ect == ElevationConstraintType.NONE) {
                        elevMin.setEnabled(false);
                        elevMax.setEnabled(false);
                        elevMin.setValue(null);
                        elevMax.setValue(null);
                    } else {
                        elevMin.setEnabled(true);
                        elevMax.setEnabled(true);
                        elevMin.setValue(owner.getDataObject().getElevationConstraintMin());
                        elevMax.setValue(owner.getDataObject().getElevationConstraintMax());
                    }
                    warning.setText(getElevationWarning(owner.getDataObject()));
                });

            }}, new GBC(1, 5));
            add(elevMin, new GBC(2, 5));
            add(new JLabel(" - "), new GBC(3, 5));
            add(elevMax, new GBC(4, 5));
            add(units, new GBC(5, 5));

            add(warning, new GBC(1, 6, 5, 1, new Insets(0, 0, 5, 0)));

            final TimingWindowTableModel model = new TimingWindowTableModel();
            final JTable table = new JTable(model) {{
                getColumnModel().getColumn(0).setMinWidth(175);
                owner.addPropertyChangeListener(e -> model.setSiteQuality(owner.getDataObject()));
            }};

            add(new JLabel("Timing Windows"), new GBC(0, 7));
            add(new JScrollPane(table) {{
                setPreferredSize(new Dimension(500, 116));
            }}, new GBC(0, 8, 6, 1, new Insets(5, 0, 0, 0)) {{
                weighty = 50;
            }});

            final Frame sqpFrame = (Frame) SwingUtilities.getWindowAncestor(this);

            add(new Box(BoxLayout.LINE_AXIS) {{
                add(new JButton(Resources.getIcon("eclipse/add.gif")) {{
                    setToolTipText("Add new timing window");
                    setFocusable(false);
                    addActionListener(e ->
                        new TimingWindowDialog(sqpFrame).openNew().foreach(tw -> {
                            owner.getDataObject().addTimingWindow(tw);
                            int index = table.getModel().getRowCount() - 1;
                            table.changeSelection(index, 0, false, false);
                        }));
                    ButtonFlattener.flatten(this);
                }});

                add(new JButton(Resources.getIcon("eclipse/openbrwsr.gif")) {{
                    setToolTipText("Import timing windows from text file");
                    setFocusable(false);
                    addActionListener(e -> {
                        final TimingWindowImporter importer = new TimingWindowImporter(SiteQualityPanel.this);
                        final TimingWindowParser.TimingWindowParseResults results = importer.openImport();

                        final int newIndex = table.getModel().getRowCount();
                        owner.getDataObject().addTimingWindows(results.successesAsJava());
                        if (newIndex < table.getModel().getRowCount())
                            table.changeSelection(newIndex, 0, false, false);

                        if (results.failures().nonEmpty())
                            new TimingWindowParseFailureDialog(sqpFrame, results.failures()).setVisible(true);
                    });
                    ButtonFlattener.flatten(this);
                }});

                add(new JButton(Resources.getIcon("eclipse/remove.gif")) {{
                    setToolTipText("Remove timing window");
                    setEnabled(false);
                    setFocusable(false);
                    table.getSelectionModel().addListSelectionListener(e -> setEnabled(table.getSelectedRowCount() > 0));

                    addActionListener(e -> {
                        final SPSiteQuality sq = owner.getDataObject();

                        final int oldFirstRow = table.getSelectedRow();
                        final int[] selectedIndices = table.getSelectedRows();
                        final List<TimingWindow> selected = new ArrayList<>();
                        final List<TimingWindow> all = sq.getTimingWindows();
                        for (final int idx : selectedIndices) {
                            selected.add(all.get(idx));
                        }
                        selected.forEach(sq::removeTimingWindow);

                        final int newFirstRow = (oldFirstRow < table.getRowCount()) ? oldFirstRow : table.getRowCount() - 1;
                        if (newFirstRow >= 0) table.changeSelection(newFirstRow, 0, false, false);
                    });
                    ButtonFlattener.flatten(this);
                }});

                add(new JButton(Resources.getIcon("eclipse/edit.gif")) {{
                    setToolTipText("Edit timing window");
                    setEnabled(false);
                    setFocusable(false);
                    table.getSelectionModel().addListSelectionListener(e -> setEnabled(table.getSelectedRowCount() == 1));
                    addActionListener(e -> {
                        final int firstRow = table.getSelectedRow();
                        final TimingWindow prev = owner.getDataObject().getTimingWindows().get(firstRow);
                        new TimingWindowDialog(sqpFrame).openEdit(prev).foreach(tw -> {
                            owner.getDataObject().removeTimingWindow(prev);
                            owner.getDataObject().addTimingWindow(tw);
                            table.changeSelection(table.getModel().getRowCount() - 1, 0, false, false);
                        });
                    });
                    ButtonFlattener.flatten(this);
                }});
            }}, new GBC(0, 9, 2, 1, new Insets(5 , 0, 0, 0)));
        }});
    }

    private String getElevationWarning(SPSiteQuality sq) {
        ElevationConstraintType ect = sq.getElevationConstraintType();
        if (ect == ElevationConstraintType.NONE) return null;
        if (sq.getElevationConstraintMin() < ect.getMin()) return "The minimum allowed value is " + ect.getMin() + ".";
        if (sq.getElevationConstraintMax() > ect.getMax()) return "The maximum allowed value is " + ect.getMax() + ".";
        if (sq.getElevationConstraintMax() <= sq.getElevationConstraintMin()) return "Minimum must be less than maximum.";
        return null;
    }

    public void updateEnabledState(boolean enabled) {
        final boolean e = enabled &&
                owner.getDataObject().getElevationConstraintType() != ElevationConstraintType.NONE;
        elevMin.setEnabled(e);
        elevMax.setEnabled(e);
    }

    private class GBC extends GridBagConstraints {
        GBC(int gridx, int gridy) {
            this.gridx = gridx;
            this.gridy = gridy;
            insets = new Insets(0, 3, 1, 3);
            fill = HORIZONTAL;
            anchor = EAST;
        }

        GBC(int gridx, int gridy, Insets insets) {
            this(gridx, gridy, 1, 1, insets);
        }

        GBC(int gridx, int gridy, int xspan, int yspan, Insets insets) {
            this(gridx, gridy);
            this.gridwidth = xspan;
            this.gridheight = yspan;
            Insets prev = this.insets;
            this.insets = new Insets(
                    prev.top + insets.top,
                    prev.left + insets.left,
                    prev.bottom + insets.bottom,
                    prev.right + insets.right);
        }
    }
}


class TimingWindowTableModel extends DefaultTableModel implements PropertyChangeListener {
    private enum Cols {
        Window, Duration, Repeats, Period
    }

    private static final long MS_PER_SECOND = 1000;
    private static final long MS_PER_MINUTE = MS_PER_SECOND * 60;
    private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));

    private SPSiteQuality sq;

    void setSiteQuality(final SPSiteQuality siteQuality) {
        if (sq != null) sq.removePropertyChangeListener(this);
        sq = siteQuality;
        if (sq != null) sq.addPropertyChangeListener(this);
        fireTableDataChanged();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return Cols.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return Cols.values()[column].name();
    }

    @Override
    public int getRowCount() {
        return sq == null ? 0 : sq.getTimingWindows().size();
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        try {
            final TimingWindow tw = sq.getTimingWindows().get(row);
            switch (Cols.values()[column]) {
                case Window: return formatWindow(tw);
                case Duration: return formatDuration(tw);
                case Repeats: return formatRepeat(tw);
                case Period: return formatPeriod(tw);
            }
            return null;
        } catch (final IndexOutOfBoundsException ioobe) {
            // can happen in rare race conditions. not a problem.
            return null;
        }
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return false;
    }

    private static String formatDuration(final TimingWindow tw) {
        final long ms = tw.getDuration();
        if (ms == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return "forever";
        return String.format("%02d:%02d", ms / MS_PER_HOUR, (ms % MS_PER_HOUR) / MS_PER_MINUTE);
    }

    private static String formatPeriod(final TimingWindow tw) {
        if (tw.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return null;
        if (tw.getRepeat() == TimingWindow.REPEAT_NEVER) return null;
        final long ms = tw.getPeriod();
        final long hh = ms / MS_PER_HOUR;
        final long mm = (ms % MS_PER_HOUR) / MS_PER_MINUTE;
        final long ss = (ms % MS_PER_MINUTE) / MS_PER_SECOND;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    private static String formatRepeat(final TimingWindow tw) {
        if (tw.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return null;

        final int repeats = tw.getRepeat();
        switch (repeats) {
            case TimingWindow.REPEAT_FOREVER: return "forever";
            case TimingWindow.REPEAT_NEVER:   return "never";
            default:                          return String.format("%d time%s", repeats, repeats > 1 ? "s" : "");
        }
    }

    private static String formatWindow(final TimingWindow tw) {
        return dateFormat.format(Instant.ofEpochMilli(tw.getStart()));
    }
}