//
// $
//

package jsky.app.ot.gemini.editor.offset;

import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.BevelBorder;

import java.awt.*;
import java.text.NumberFormat;

/**
 * A panel that contains widgets for displaying and manipulating a table of
 * offset positions.
 */
public final class OffsetPosTablePanel extends JPanel {

    private  static final NumberFormat NF = NumberFormat.getNumberInstance();
    static {
        NF.setMinimumFractionDigits(1);
        NF.setMaximumFractionDigits(3);
        NF.setMinimumIntegerDigits(1);
    }

    private class PaddedCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int i, int i1) {
            if (o instanceof Double) {
                o = NF.format(o);
            }
            JComponent comp;
            comp = (JComponent) super.getTableCellRendererComponent(jTable, o, b, b1, i, i1);
            comp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(1, 4, 1, 4),
                    comp.getBorder()
            ));
            return comp;
        }
    }

    private class IntPaddedCellRenderer extends PaddedCellRenderer {
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int i, int i1) {
            JLabel lab;
            lab = (JLabel) super.getTableCellRendererComponent(jTable, o, b, b1, i, i1);
            lab.setHorizontalAlignment(JLabel.RIGHT);
            return lab;
        }
    }

    // Renders the header with an error icon if using a guider that is not
    // available in the context.
    private class HeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;
        private final Icon errorIcon   = Resources.getIcon("eclipse/error.gif");
        private final Icon linkedIcon  = Resources.getIcon("eclipse/primaryGuideStar.gif");
        private final Icon unlinkedIcon= Resources.getIcon("eclipse/primaryGuideStarDisabled.gif");

        HeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lab = (JLabel) delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            AbstractOffsetPosTableModel mod = (AbstractOffsetPosTableModel) table.getModel();
            Icon icon = null;
            switch (mod.getGuideProbeState(column)) {
                case unavailable:
                    icon = errorIcon;
                    break;
                case unlinked:
                    icon = unlinkedIcon;
                    break;
                case linked:
                    icon = linkedIcon;
                    break;
            }
            lab.setIcon(icon);
            return lab;
        }
    }

    protected JTable offsetTable;

    protected JButton newButton;
    protected JButton removeButton;
    protected JButton topButton;
    protected JButton upButton;
    protected JButton downButton;
    protected JButton bottomButton;
    protected JButton removeAllButton;

    protected JButton gridButton;
    protected JButton randomButton;

    public OffsetPosTablePanel() {
        super(new BorderLayout(0, 0));

        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        offsetTable = new JTable() {
            private TableCellRenderer rend = new PaddedCellRenderer();
            private TableCellRenderer irend = new IntPaddedCellRenderer();

            public TableCellRenderer getCellRenderer(int row, int col) {
                Class c = getModel().getColumnClass(col);
                if (Integer.class.equals(c) || int.class.equals(c)) {
                    return irend;
                }
                return rend;
            }
        };

        offsetTable.getTableHeader().setDefaultRenderer(new HeaderRenderer(offsetTable.getTableHeader().getDefaultRenderer()));

        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        jScrollPane.setToolTipText("Offset positions and the associated guide probe settings");
        jScrollPane.setViewportView(offsetTable);

        add(jScrollPane, BorderLayout.CENTER);

        add(makePositionTableButtonPanel(), BorderLayout.SOUTH);
    }

    protected JPanel makePositionTableButtonPanel() {
        JPanel pan = new JPanel(new GridBagLayout());

        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy  = 0;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);

        newButton  = new JButton("New");
        newButton.setFocusable(false);
        gbc.gridx  = 0;
        pan.add(newButton, gbc);

        removeButton = new JButton("Remove");
        removeButton.setFocusable(false);
        gbc.gridx  = 1;
        pan.add(removeButton, gbc);

        removeAllButton = new JButton("Remove All");
        removeAllButton.setFocusable(false);
        gbc.gridx  = 2;
        pan.add(removeAllButton, gbc);

        topButton = new JButton("Top");
        topButton.setFocusable(false);
        gbc.gridx  = 3;
        gbc.insets = new Insets(0, 5, 0, 0);
        pan.add(topButton, gbc);

        gbc.insets = new Insets(0, 0, 0, 0);

        upButton = new JButton("Up");
        upButton.setFocusable(false);
        gbc.gridx  = 4;
        pan.add(upButton, gbc);

        downButton = new JButton("Down");
        downButton.setFocusable(false);
        gbc.gridx  = 5;
        pan.add(downButton, gbc);

        bottomButton = new JButton("Bottom");
        bottomButton.setFocusable(false);
        gbc.gridx  = 6;
        pan.add(bottomButton, gbc);

        // Push everything to the left.
        gbc.gridx   = 7;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        pan.add(new JPanel(), gbc);

        gridButton   = new JButton("Grid ...");
        randomButton = new JButton("Random ...");
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.add(gridButton);
        btnPanel.add(randomButton);

        gbc.gridx   = 8;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.weightx = 0;
        pan.add(btnPanel, gbc);

        return pan;
    }
}
