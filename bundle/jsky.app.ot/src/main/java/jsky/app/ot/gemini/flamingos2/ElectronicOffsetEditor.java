//
// $
//

package jsky.app.ot.gemini.flamingos2;

import javax.swing.*;

/**
 * A helper class used to edit a Flamingos 2 electronic offsetting information.
 *
 * @deprecated was found to be insufficient for the requirements
 */
@Deprecated
final class ElectronicOffsetEditor extends JPanel {
/*
    private final class OffsetDistanceListener implements EditListener<Flamingos2, Double>, PropertyChangeListener {
        private final JLabel label;
        private final OffsetTableModel table;

        private Flamingos2 flam2;

        OffsetDistanceListener(JLabel label, OffsetTableModel model) {
            this.label = label;
            this.table = model;
        }

        public void valueChanged(EditEvent<Flamingos2, Double> event) {
            update(event.getNewValue());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void setDataObject(Flamingos2 flam2) {
            this.flam2 = flam2;
        }

        void update() {
            update((flam2 == null) ? null : flam2.getElectronicOffset());
        }

        void update(Double val) {
            Option<Double> tableVal = None.instance();

            Color fg = Color.black;
            String txt = "";
            if ((flam2 != null) && (val != null) && (flam2.getUseElectronicOffsetting())) {
                double max = flam2.getMaxElectronicOffsetDistance();
                if (Math.abs(val) > max) {
                    fg = ComponentEditor.FATAL_FG_COLOR;
                    txt = String.format("Offset limited to %.2f arcsec.", max);
                } else {
                    tableVal = new Some<Double>(val);
                }
            }

            // Update the position table view.
            table.setOffset(tableVal);

            // Update the offset position editor.
            eoffsetCtrl.getComponent().setEnabled((flam2 != null) && flam2.getUseElectronicOffsetting());

            // Update the warning label at the bottom.
            label.setText(txt);
            label.setForeground(fg);
        }
    }

    private static class RightJustifyRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lab.setHorizontalAlignment(SwingConstants.RIGHT);
            return lab;
        }
    }

    private final CheckboxPropertyCtrl<Flamingos2> useEoffsettingCtrl;
    private final TextFieldPropertyCtrl<Flamingos2, Double> eoffsetCtrl;
    private final OffsetDistanceListener offsetDistanceListener;
    private final JLabel warningMessage;

    ElectronicOffsetEditor() {
        super(new GridBagLayout());

        useEoffsettingCtrl = new CheckboxPropertyCtrl<Flamingos2>(Flamingos2.USE_ELECTRONIC_OFFSETTING_PROP);

        JCheckBox check = (JCheckBox) useEoffsettingCtrl.getComponent();
        check.setText("Enable Electronic Offsetting");
        add(check, new GridBagConstraints() {{gridwidth=3; anchor=EAST;}});


        JLabel lab = new JLabel("Offset \u00B1");
        JLabel units = new JLabel("arcsec");

        add(lab, new GridBagConstraints() {{
            gridx=0; gridy=1; insets=new Insets(ComponentEditor.PROPERTY_ROW_GAP, 0, 2, 0); anchor=EAST;
        }});
        eoffsetCtrl = TextFieldPropertyCtrl.createDoubleInstance(Flamingos2.ELECTRONIC_OFFSET_PROP, 2);
        eoffsetCtrl.setColumns(4);
        add(eoffsetCtrl.getTextField(), ComponentEditor.propWidgetGbc(1, 1));
        add(units, ComponentEditor.propUnitsGbc(2, 1));

        OffsetTableModel tableModel = new OffsetTableModel();

        // Warning label.
        final JLabel w = new JLabel("");
        offsetDistanceListener = new OffsetDistanceListener(w, tableModel);
        eoffsetCtrl.addEditListener(offsetDistanceListener);
        add(w, ComponentEditor.warningLabelGbc(0, 2, 3));

        // Spacer between the widgets and the offset table
        add(new JPanel(), ComponentEditor.colGapGbc(3, 0));

        // Offset position table.
        JTable tab = createOffsetTable(tableModel);
        add(tab.getTableHeader(), new GridBagConstraints() {{
            gridx=4; gridy=0; anchor=NORTHWEST;
        }});
        add(tab, new GridBagConstraints() {{
            gridx=4; gridy=1; gridheight=4; anchor=NORTHWEST;
        }});

        // Spacer between the widgets and the warning label at the bottom.
        // Pushes the warning label to the bottom.
        add(new JPanel(), new GridBagConstraints() {{
             gridx=5; gridy=3; weightx=1.0; weighty=1.0; fill=BOTH;
        }});

        warningMessage = new JLabel();
        warningMessage.setForeground(Color.black);
        warningMessage.setBackground(ComponentEditor.FATAL_BG_COLOR);
        Border b = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.gray, 1),
                BorderFactory.createEmptyBorder(5, 2, 5, 2)
        );
        warningMessage.setBorder(b);
        warningMessage.setOpaque(true);
        add(warningMessage, new GridBagConstraints() {{
            gridx=0; gridy=4; gridwidth=6; weightx=1.0; fill=HORIZONTAL; anchor=WEST;
        }});
    }

    private JTable createOffsetTable(OffsetTableModel model) {
        JTable tab = new JTable(model);
        tab.setRowSelectionAllowed(false);
        tab.setColumnSelectionAllowed(false);
        tab.setBackground(ComponentEditor.INFO_BG_COLOR);
        tab.setFocusable(false);
        tab.getTableHeader().setReorderingAllowed(false);
        tab.getTableHeader().setResizingAllowed(false);

        TableCellRenderer rend = new RightJustifyRenderer();
        TableColumnModel tcm = tab.getColumnModel();
        for (int i=0; i<OffsetTableModel.Column.values().length; ++i) {
            TableColumn tc = tcm.getColumn(i);
            tc.setPreferredWidth(50);
            tc.setCellRenderer(rend);
        }
        return tab;
    }

    void handlePreDataObjectUpdate(Flamingos2 inst, SPProgData progData) {
        if (inst == null) return;
        inst.removePropertyChangeListener(Flamingos2.USE_ELECTRONIC_OFFSETTING_PROP.getName(), offsetDistanceListener);
        inst.removePropertyChangeListener(Flamingos2.ELECTRONIC_OFFSET_PROP.getName(), offsetDistanceListener);
    }

    void handlePostDataObjectUpdate(Flamingos2 inst, SPProgData progData) {
        useEoffsettingCtrl.setBean(inst);
        eoffsetCtrl.setBean(inst);

        boolean enabled = true;
        if (otherOffsetComponentsExist(progData)) {
            warningMessage.setText("To enable, first remove existing 'Offset' components in the sequence.");
            enabled = false;
        } else if (!oiwfsExists(progData)) {
            warningMessage.setText("To enable, first add an OIWFS guide star to the Target Environment.");
            enabled = false;
        }

        useEoffsettingCtrl.getComponent().setEnabled(enabled);
        warningMessage.setVisible(!enabled);
        eoffsetCtrl.getComponent().setEnabled(enabled && inst.getUseElectronicOffsetting());

        inst.addPropertyChangeListener(Flamingos2.USE_ELECTRONIC_OFFSETTING_PROP.getName(), offsetDistanceListener);
        inst.addPropertyChangeListener(Flamingos2.ELECTRONIC_OFFSET_PROP.getName(), offsetDistanceListener);
        offsetDistanceListener.setDataObject(inst);
        offsetDistanceListener.update();
    }

    private boolean otherOffsetComponentsExist(SPProgData progData) {
        java.util.List<IOffsetPosListProvider> posLists;
        posLists = progData.getOffsetPosListProviders();

        for (IOffsetPosListProvider prov : posLists) {
            if (!(prov instanceof Flamingos2)) {
                return true;
            }
        }
        return false;
    }

    private boolean oiwfsExists(SPProgData progData) {
        TargetEnvironment env = progData.getTargetEnvironment();
        GuideTargets gt = env.getGuideTargets(Flamingos2OiwfsGuideProbe.instance);
        if (gt == null) return false;
        return gt.imList().size() > 0;
    }
    */
}
