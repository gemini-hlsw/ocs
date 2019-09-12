package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.ags.gems.GemsStrehl;
import edu.gemini.spModel.core.SiderealTarget;
import jsky.util.gui.Resources;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.FontHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * OT-111: Displays the asterisms/guide groups found
 */
class CandidateAsterismsTreeTable extends JXTreeTable {

//    private static final Color HIGHLIGHTER_COLOR = Color.lightGray;
    private static final Color HIGHLIGHTER_COLOR = new Color(229,229,229);

    private static final Icon starIcon       = Resources.getIcon("eclipse/primaryGuideStar.gif");
    private static final Icon starIconDisabled   = Resources.getIcon("eclipse/primaryGuideStarDisabled.gif");

    // Custom tree cell renderer
    private static final class TreeCellRenderer extends DefaultTreeCellRenderer {
        private TreeCellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                       Object value,
                                       boolean selected,
                                       boolean expanded,
                                       boolean leaf,
                                       int row,
                                       boolean hasFocus) {
            JLabel lab = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            lab.setIcon(null);

            if (value instanceof CandidateAsterismsTreeTableModel.Row) {
                CandidateAsterismsTreeTableModel.Row tableDataRow = (CandidateAsterismsTreeTableModel.Row) value;
                if (tableDataRow.isTopLevel()) {
                    GemsStrehl strehl = tableDataRow.getGemsGuideStars().strehl();
                    if (strehl != null) {
                        String s = String.format("Strehl: %.1f avg", strehl.avg() * 100);
                        lab.setText(s);
                        lab.setFont(lab.getFont().deriveFont(Font.BOLD));
                    }
                } else {
                    lab.setText(tableDataRow.getGuideProbeTargets().getGuider().getKey());
                    lab.setFont(lab.getFont().deriveFont(Font.PLAIN));
                }
            }
            return lab;
        }
    }


    // Custom table cell renderer
    private class CustomTableCellRenderer extends DefaultTableCellRenderer {
        JCheckBox _checkBox = new JCheckBox();

        private CustomTableCellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setBorder(null);
            _checkBox.setHorizontalAlignment(JCheckBox.CENTER);
            _checkBox.setBorderPaintedFlat(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel lab = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lab.setIcon(null);
            if (value instanceof Boolean) {
                if (column == CandidateAsterismsTreeTableModel.Col.CHECK.ordinal()) {
                    _checkBox.setSelected((Boolean) value);
                    if (isSelected) {
                        _checkBox.setBackground(table.getSelectionBackground());
                        _checkBox.setForeground(table.getSelectionForeground());
                    } else {
                        _checkBox.setBackground(table.getBackground());
                        _checkBox.setForeground(table.getForeground());
                    }
                    return _checkBox;
                } else if (column == CandidateAsterismsTreeTableModel.Col.PRIMARY.ordinal()) {
                    lab.setText("");
                    if ((Boolean) value) {
                        lab.setIcon(starIcon);
                    } else {
                        lab.setIcon(starIconDisabled);
                    }
                }
            }
            lab.setBorder(null);
            return lab;
        }
    }

    // Custom cell editor
    private final class CheckBoxTableCellEditor extends DefaultCellEditor {
        private JCheckBox _checkBox;

        public CheckBoxTableCellEditor() {
            this(new JCheckBox());
            _checkBox.addActionListener(e -> selectRelatedRows());
        }

        public CheckBoxTableCellEditor(JCheckBox checkBox) {
            super(checkBox);
            _checkBox = checkBox;
            _checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        }
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     int row, int column) {
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }


    private CustomTableCellRenderer _tableCellRenderer = new CustomTableCellRenderer();
    private CheckBoxTableCellEditor _checkBoxTableCellEditor = new CheckBoxTableCellEditor();

    // List of unique GemsGuideStars used for highlighting
    private List<GemsGuideStars> _gemsGuideStarsList;

    private GemsGuideStarSearchController _controller;

    /**
     * Constructor
     */
    public CandidateAsterismsTreeTable() {
        setTreeCellRenderer(new TreeCellRenderer());

        // highlight groups or rows
        setHighlighters(getColorHighlighter(), getFontHighlighter());

        setShowsRootHandles(false);
        setCellSelectionEnabled(false);
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(true);

        // OT-111: double click sets group to be primary
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    CandidateAsterismsTreeTableModel.Row row = getSelectedModelRow();
                    if (row != null) {
                        CandidateAsterismsTreeTableModel.Row parentRow = row.getParent();
                        if (row.isCheckBoxSelected()) {
                            selectPrimary(row);
                        } else if (parentRow != null && parentRow.isCheckBoxSelected()) {
                            selectPrimary(parentRow);
                        }
                    }
                }
            }
        });
    }

    public void setController(GemsGuideStarSearchController controller) {
        _controller = controller;
    }

    // Highlights even/odd sets of related parent and child rows
    private ColorHighlighter getColorHighlighter() {
        HighlightPredicate predicate = (component, adapter) -> {
            GemsGuideStars gemsGuideStars = getGemsGuideStars(adapter.row);
            return gemsGuideStars != null && _gemsGuideStarsList != null && _gemsGuideStarsList.indexOf(gemsGuideStars) % 2 == 0;
        };
        return new ColorHighlighter(predicate, HIGHLIGHTER_COLOR, null, null, null);
    }

    // Sets bold font for top level rows
    private FontHighlighter getFontHighlighter() {
        HighlightPredicate predicate = new HighlightPredicate.DepthHighlightPredicate(1);
        return new FontHighlighter(predicate, getFont().deriveFont(Font.BOLD));
    }

    private void selectPrimary(CandidateAsterismsTreeTableModel.Row row) {
        getCandidateAsterismsTreeTableModel().setPrimary(row);
        selectRelatedRows();
        repaint();
    }
    /**
     * Selects all rows related to the currently selected row
     */
    public void selectRelatedRows() {
        for (int row : getSelectedRows()) {
            selectRelatedRows(getGemsGuideStars(row));
        }
    }

    /**
     * Selects all rows related to the given GemsGuideStars object
     */
    private void selectRelatedRows(GemsGuideStars gemsGuideStars) {
        int numRows = getRowCount();
        ListSelectionModel selectionModel = getSelectionModel();
        for (int i = numRows-1; i >= 0; i--) {
            TreePath path = getPathForRow(i);
            Object o = path.getLastPathComponent();
            if (o instanceof CandidateAsterismsTreeTableModel.Row) {
                CandidateAsterismsTreeTableModel.Row row = (CandidateAsterismsTreeTableModel.Row) o;
                if (row.getGemsGuideStars() == gemsGuideStars) {
                    int rowIndex = getRowForPath(path);
                    selectionModel.addSelectionInterval(rowIndex, rowIndex);
                }
            }
        }
    }

    /**
     * Returns a list of all of the GemsGuideStars objects referenced in the tree table Row objects
     */
    public List<GemsGuideStars> getGemsGuideStarsList() {
        int numRows = getRowCount();
        Set<GemsGuideStars> gemsGuideStarsSet = new TreeSet<>();
        for (int i = 0; i < numRows; i++) {
            TreePath path = getPathForRow(i);
            Object o = path.getLastPathComponent();
            if (o instanceof CandidateAsterismsTreeTableModel.Row) {
                gemsGuideStarsSet.add(((CandidateAsterismsTreeTableModel.Row)o).getGemsGuideStars());
            }
        }
        return new ArrayList<>(gemsGuideStarsSet);
    }

    /**
     * Returns the GemsGuideStars object corresponding to the given row
     */
    public GemsGuideStars getGemsGuideStars(int rowIndex) {
        TreePath path = getPathForRow(rowIndex);
        Object o = path.getLastPathComponent();
        if (o instanceof CandidateAsterismsTreeTableModel.Row) {
            return ((CandidateAsterismsTreeTableModel.Row) o).getGemsGuideStars();
        }
        return null;
    }

    /**
     * Returns the model Row object for the selected row
     */
    public CandidateAsterismsTreeTableModel.Row getSelectedModelRow() {
        int rowIndex = getSelectedRow();
        if (rowIndex != -1) {
            TreePath path = getPathForRow(rowIndex);
            Object o = path.getLastPathComponent();
            if (o instanceof CandidateAsterismsTreeTableModel.Row) {
                return (CandidateAsterismsTreeTableModel.Row) o;
            }
        }
        return null;
    }

    public void clear() {
        setTreeTableModel(new DefaultTreeTableModel());
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (getValueAt(row, column) instanceof Boolean) {
            return _checkBoxTableCellEditor;
        }
        return super.getCellEditor(row, column);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == getTreeTableModel().getHierarchicalColumn())
            return super.getCellRenderer(row, column);
        return _tableCellRenderer;
    }

    @Override
    public void setTreeTableModel(TreeTableModel treeModel) {
        super.setTreeTableModel(treeModel);
        _gemsGuideStarsList = getGemsGuideStarsList();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return null;
    }

    public CandidateAsterismsTreeTableModel getCandidateAsterismsTreeTableModel() {
        return (CandidateAsterismsTreeTableModel)getTreeTableModel();
    }

    /**
     * Adds the checked asterisms to the target environment
     */
    public void addCheckedAsterisms() {
        CandidateAsterismsTreeTableModel model = getCandidateAsterismsTreeTableModel();
        _controller.add(model.getCheckedAsterisms(), model.getPrimaryIndex(0));
    }
}
