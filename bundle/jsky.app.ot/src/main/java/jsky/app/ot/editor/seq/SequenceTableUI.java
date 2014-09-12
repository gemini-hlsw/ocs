//
// $Id$
//

package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.gui.RotatedButtonUI;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.seqcomp.SeqBase;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.PrintableJTable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.util.Collections;
import java.util.Enumeration;

import static jsky.app.ot.util.OtColor.LIGHT_ORANGE;
import static jsky.app.ot.util.OtColor.VERY_LIGHT_GREY;

/**
 * Table displayed in the top level sequence node and in smart cal editors.
 */
public class SequenceTableUI extends JPanel {

    private PrintableJTable _dynamicTable;
    private DynamicSequenceTableModel _dynamicModel;
    private SequenceTableMessagePanel _msgPanel;

    private JTable _staticTable;
    private StaticConfigurationTableModel _staticModel;
    private JPanel _staticPanel;

    private OtItemEditor<?,?> _owner;

    public SequenceTableUI() {
        super(new GridBagLayout());

        _dynamicModel = new DynamicSequenceTableModel();
        _staticModel  = new StaticConfigurationTableModel();
        _msgPanel     = new SequenceTableMessagePanel();

        final JPanel sp = new JPanel(new BorderLayout(10, 10));
        sp.add(_createStepTablePanel(_dynamicModel), BorderLayout.CENTER);

        _staticPanel = _createStaticTablePanel(_staticModel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx    = 0;
        gbc.gridy    = 0;
        gbc.weightx  = 1.0;
        gbc.weighty  = 1.0;
        gbc.fill     = GridBagConstraints.BOTH;
        gbc.insets   = new Insets(0, 0, 5, 2);
        add(sp, gbc);

        JToggleButton staticButton = new JToggleButton("Static Configuration");

        // My Doggy Style to match the p2 checker
        staticButton.setUI(new RotatedButtonUI(RotatedButtonUI.Orientation.topToBottom));
        staticButton.setBackground(VERY_LIGHT_GREY);

        gbc.gridx    = 1;
        gbc.gridy    = 0;
        gbc.weightx  = 0.0;
        gbc.weighty  = 0.0;
        gbc.fill     = GridBagConstraints.NONE;
        gbc.anchor   = GridBagConstraints.NORTH;
        gbc.insets   = new Insets(0, 0, 5, 0);
        add(staticButton, gbc);

        staticButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JToggleButton tb = (JToggleButton) e.getSource();
                if (tb.isSelected()) {
                    tb.setBackground(LIGHT_ORANGE);
                    sp.add(_staticPanel, BorderLayout.EAST);
                    sp.validate();
                } else {
                    tb.setBackground(VERY_LIGHT_GREY);
                    sp.remove(_staticPanel);
                    sp.validate();
                }
            }
        });

        add(_msgPanel, new GridBagConstraints() {{
            gridx   = 0;
            gridy   = 1;
            weightx = 1.0;
            fill    = HORIZONTAL;
            insets  = new Insets(0, 0, 0, 2);
        }});
    }

    private JPanel _createStepTablePanel(DynamicSequenceTableModel tm) {
        JPanel pan = new JPanel(new BorderLayout());

        _dynamicTable = new PrintableJTable(tm) {{
            setAutoResizeMode(AUTO_RESIZE_OFF);
            setBackground(VERY_LIGHT_GREY);
            getTableHeader().setReorderingAllowed(false);
            setRowSelectionAllowed(false);
            setColumnSelectionAllowed(false);
            setFocusable(false);
        }};
        JScrollPane sp = new JScrollPane();
        sp.setViewportView(_dynamicTable);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);


        pan.add(sp, BorderLayout.CENTER);

        return pan;
    }

    private JPanel _createStaticTablePanel(StaticConfigurationTableModel tm) {
        JPanel pan = new JPanel(new BorderLayout());

        _staticTable = new JTable(tm) {{
            setAutoResizeMode(AUTO_RESIZE_OFF);
            setBackground(VERY_LIGHT_GREY);
            getTableHeader().setReorderingAllowed(false);
            setRowSelectionAllowed(false);
            setColumnSelectionAllowed(false);
            setFocusable(false);
        }};
        _staticTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        _staticTable.setBackground(VERY_LIGHT_GREY);
        _staticTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane();
        sp.setViewportView(_staticTable);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        pan.add(sp, BorderLayout.CENTER);

        return pan;
    }

    private void resetCellRenderers(JTable table, TableCellRenderer rend) {
        TableColumnModel colModel = table.getColumnModel();
        for (Enumeration<TableColumn> e = colModel.getColumns(); e.hasMoreElements(); ) {
            TableColumn tc = e.nextElement();
            tc.setCellRenderer(rend);
        }
    }

    /**
     * @param owner the owning editor
     */
    public void init(OtItemEditor owner) {
        _owner = owner;
    }

    public void showPrintDialog(String title) throws PrinterException {
        _dynamicTable.showPrintDialog(title, true);
    }

    public void update(boolean showAllSteps) {
        update(showAllSteps, Collections.<ItemKey>emptyList());

    }

    private boolean isRootSequenceNode() {
        return (_owner.getNode() instanceof ISPSeqComponent) &&
                (((ISPSeqComponent) _owner.getNode()).getType() == SeqBase.SP_TYPE);
    }

    public void update(boolean showAllSteps, java.util.List<ItemKey> alwaysDynamic) {
        try {

            final SPNodeKey _nodeKey = _owner.getNode().getNodeKey();

            ISPObservation obs = _owner.getContextObservation();
            ConfigSequence seq = (obs == null) ? new ConfigSequence() : ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP, false);

            ConfigSequence subSeq = seq;
            if (!showAllSteps) {
                subSeq = seq.filter(new MetaDataConfig.NodeKeySequencePredicate(_nodeKey));
            }

            final SPNodeKey filterKey = isRootSequenceNode() ? null : _nodeKey;
            _dynamicModel.setSequence(subSeq, filterKey, alwaysDynamic);
            _msgPanel.setSequence(subSeq, _nodeKey);
            _staticModel.setSequence(subSeq, alwaysDynamic);

            resetCellRenderers(_dynamicTable, new DynamicSequenceCellRenderer(_dynamicModel));
            resetCellRenderers(_staticTable,  new StaticConfigurationCellRenderer(_staticModel));

            SequenceTabUtil.resizeTableColumns(_dynamicTable, _dynamicModel);
            SequenceTabUtil.resizeTableColumns(_staticTable,  _staticModel);

            // Don't look at these lines.  After much dicking around trying to
            // get the static table to show up correctly, this seemed to work
            // more or less.  15 is a magic number that seemed to look okay.
            // Seems like preferred size should be sufficient, and that there
            // should be no need for any of this crap.
            Dimension dim = _staticTable.getPreferredSize();
            dim = new Dimension(dim.width + 15, dim.height);
            _staticTable.setPreferredScrollableViewportSize(dim);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }
}
