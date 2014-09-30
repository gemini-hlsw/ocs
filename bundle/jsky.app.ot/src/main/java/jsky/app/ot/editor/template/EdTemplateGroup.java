package jsky.app.ot.editor.template;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.shared.gui.ButtonFlattener;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.NonSiderealTarget;
import edu.gemini.spModel.template.SpBlueprint;
import edu.gemini.spModel.template.TemplateFolder;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.spModel.util.ReadableNodeName;
import jsky.app.ot.OTOptions;
import jsky.app.ot.StaffBean;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.util.PropertyChangeMultiplexer;
import jsky.app.ot.util.Resources;
import jsky.app.ot.viewer.SPDragDropObject;
import jsky.app.ot.viewer.SPEventManager;
import jsky.app.ot.viewer.SPTree;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SingleSelectComboBox;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import static java.awt.GridBagConstraints.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class EdTemplateGroup extends OtItemEditor<ISPTemplateGroup, TemplateGroup> {

    private final TemplateGroupPanel panel = new TemplateGroupPanel();

    public JPanel getWindow() {
        return panel;
    }

    public void init() {
        panel.init(getProgram(), getNode(), getDataObject());
    }

    public void selectParameters(ISPTemplateParameters params) {
        panel.selectParameters(params);
    }
}

class TemplateGroupPanel extends JPanel {

    private final EdTemplateGroupHeader header = new EdTemplateGroupHeader();
    private final ParamsListTable table        = new ParamsListTable();
    private final EdTemplateGroupFooter footer = new EdTemplateGroupFooter(table);

    public TemplateGroupPanel() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void init(ISPProgram root, ISPTemplateGroup templateGroupNode, TemplateGroup templateGroup) {
        table.getModel().setTemplateGroupNode(templateGroupNode);
        header.init(templateGroupNode, templateGroup);
        footer.init(root, templateGroupNode);
    }

    public void selectParameters(ISPTemplateParameters params) {
        for (int row=0; row<table.getModel().getRowCount(); ++row) {
            final ISPTemplateParameters tp = table.getModel().getEntryAt(row).getKey();
            if (tp.getNodeKey().equals(params.getNodeKey())) {
                table.getSelectionModel().setSelectionInterval(row, row);
                break;
            }
        }
    }
}

class ParamsListTable extends JTable {

    public ParamsListTable() {

        // Non-default table model
        super(new ParamsListTableModel());

        // Set up drag and drop
        setTransferHandler(new ParamsListTableTransferHandler());
        setDragEnabled(true);

        // Allow multi-select. Why the hell not?
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Use the same renderer for all columns
        final TableCellRenderer renderer = new ParamsListTableCellRenderer();
        final Enumeration<TableColumn> cols = getColumnModel().getColumns();
        while (cols.hasMoreElements())
            cols.nextElement().setCellRenderer(renderer);

    }

    // Hooray contravariance!
    public ParamsListTableModel getModel() {
        return (ParamsListTableModel) super.getModel();
    }

    // Fake out the OT by making the drag appear to come from the SPTree. This is probably evil.
    private class ParamsListTableTransferHandler extends TransferHandler {

        protected Transferable createTransferable(JComponent c) {
            return new SPDragDropObject(getSelection(), getTree());
        }

        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        private SPTree getTree() {
            Container c = getParent();
            while (!(c instanceof SPViewer))
                c = c.getParent(); // will NPE if there is no such parent
            return ((SPViewer) c).getTree();
        }

        public ISPNode[] getSelection() {
            final ParamsListTableModel model = getModel();
            final int[] indices = getSelectedRows();
            final ISPNode[] nodes = new ISPNode[indices.length];
            for (int i = 0; i < indices.length; i++)
                nodes[i] = model.getEntryAt(indices[i]).getKey();
            return nodes;
        }

    }

}

class ParamsListTableModel extends AbstractTableModel implements PropertyChangeListener {

    private ISPTemplateGroup templateGroupNode;

    // We need to listen to remote events
    private PropertyChangeMultiplexer relay;
    private SPEventManager eventManager;

    // We keep a map from each node to its data object, for efficiency
    private final LinkedHashMap<ISPTemplateParameters, TemplateParameters> data =
            new LinkedHashMap<ISPTemplateParameters, TemplateParameters>();

    public void setTemplateGroupNode(ISPTemplateGroup tgn) {

        // For now don't handle null
        if (tgn == null)
            throw new IllegalArgumentException("Template group cannot be null.");

        // Set up remote cripe here if we need to
        if (relay == null) {
            relay = new PropertyChangeMultiplexer();
            relay.addPropertyChangeListener(this);
            eventManager = new SPEventManager(relay);
        }

        // New State
        templateGroupNode = tgn;
        eventManager.setRootNode(templateGroupNode);

        // Initialize our list
        refreshList();

    }

    private void refreshList() {
        data.clear();
        if (templateGroupNode != null) {
            for (ISPTemplateParameters psNode : templateGroupNode.getTemplateParameters()) {
                final TemplateParameters ps = (TemplateParameters) psNode.getDataObject();
                data.put(psNode, ps);
            }
        }
        fireTableDataChanged(); // Should this be on the event dispatch thread?
    }

    public void propertyChange(PropertyChangeEvent evt) {
        refreshList();
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Target";
            case 1:
                return "Conditions";
            case 2:
                return "Phase 1 Time";
            default:
                throw new IndexOutOfBoundsException("No such column: " + column);
        }
    }

    public Map.Entry<ISPTemplateParameters, TemplateParameters> getEntryAt(int rowIndex) {
        int i = 0;
        for (Map.Entry<ISPTemplateParameters, TemplateParameters> e : data.entrySet())
            if (i++ == rowIndex)
                return e;
        throw new ArrayIndexOutOfBoundsException(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        final TemplateParameters ps = getEntryAt(rowIndex).getValue();
        switch (columnIndex) {
            case 0:
                return ps.getTarget();
            case 1:
                return ps.getSiteQuality().conditions();
            case 2:
                return ps.getTime();
            default:
                throw new IndexOutOfBoundsException("No such column: " + columnIndex);
        }
    }

}

class ParamsListTableCellRenderer extends DefaultTableCellRenderer {

    private static final Icon ICON_SIDEREAL = Resources.getIcon("pit/sidereal.png");
    private static final Icon ICON_NONSIDEREAL = Resources.getIcon("pit/nonsidereal.png");
    private static final Icon ICON_CONDS = Resources.getIcon("pit/conds.png");
    private static final Icon ICON_TIME = Resources.getIcon("pit/clock.png");

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setIcon(null);

        if (value instanceof SPTarget) {

            // Cell for a target
            final SPTarget target = (SPTarget) value;
            label.setText(target.getName());

            // Two possible icons
            if (target.getTarget() instanceof NonSiderealTarget) {
                label.setIcon(ICON_NONSIDEREAL);
            } else {
                label.setIcon(ICON_SIDEREAL);
            }

        } else if (value instanceof SPSiteQuality.Conditions) {

            // Cell for site conditions
            final SPSiteQuality.Conditions conds = (SPSiteQuality.Conditions) value;
            label.setText(conds.toString());
            label.setIcon(ICON_CONDS);

        } else if (value instanceof TimeValue) {
            label.setText(String.format("%.2f hrs", ((TimeValue) value).getTimeAmount()));
            label.setIcon(ICON_TIME);
        }

        return label;
    }

}

class EdTemplateGroupHeader extends JPanel {

    private TemplateGroup templateGroup;

    private final JLabel resource = new JLabel();
    private final JComboBox status = new SingleSelectComboBox() {{
        setChoices(TemplateGroup.Status.values());
        setMaximumRowCount(TemplateGroup.Status.values().length);
    }};

    private final JTextField title = new JTextField() {{
        getDocument().addDocumentListener(new AbstractDocumentListener() {
            public void textChanged(DocumentEvent docEvent, String newText) {
                if (templateGroup != null) {
                    templateGroup.setTitle(newText);
                }
            }
        });
    }};

    public EdTemplateGroupHeader() {

        setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        setLayout(new GridBagLayout());

        add(new JLabel("Resource:") {{
            setFont(getFont().deriveFont(Font.BOLD));
        }}, new GBC(0, 0, false));
        add(resource, new GBC(1, 0, true));

        add(new JLabel("Title:") {{
            setFont(getFont().deriveFont(Font.BOLD));
        }}, new GBC(0, 1, false));
        add(title, new GBC(1, 1, false));

        add(new JLabel("Target/condition pairs may be dragged to other template groups.") {{
            setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        }}, new GBC(0, 3, 2, 1, null));

    }

    public void init(ISPTemplateGroup templateGroupNode, TemplateGroup templateGroup) {
        this.templateGroup = templateGroup;

        // Walk around and gather some information
        final ISPTemplateFolder templateFolderNode = (ISPTemplateFolder) templateGroupNode.getParent();
        final TemplateFolder templateFolder = (TemplateFolder) templateFolderNode.getDataObject();
        final SpBlueprint blueprint = templateFolder.getBlueprints().get(templateGroup.getBlueprintId());

        // Update our UI
        resource.setText(blueprint.toString());
        title.setText(templateGroup.getTitle());
        status.setSelectedItem(templateGroup.getStatus());

    }

}


final class EdTemplateGroupFooter extends JPanel {
    private final JTable paramTable;

    private ISPProgram program;

    private ISPTemplateGroup templateGroup;

    // Action to add a new template group parameter triplet.
    private final Action addAction = new AbstractAction("Add", Resources.getIcon("eclipse/add.gif")) {
        { putValue(Action.SHORT_DESCRIPTION, "Add a new empty template observation."); }

        public void actionPerformed(ActionEvent evt) {
            addNewParameters(new Function1<Option<TemplateParameters>, TemplateParameters>() {
                @Override public TemplateParameters apply(Option<TemplateParameters> ignored) {
                    // Always make a new blank template parameters object.
                    return TemplateParameters.newEmpty();
                }
            });
        }
    };

    // Action to duplicate a selected template group parameter triplet.
    private final Action dupAction = new AbstractAction("Duplicate", Resources.getIcon("eclipse/duplicate.gif")) {
        { putValue(Action.SHORT_DESCRIPTION, "Duplicate the selected template observation."); }

        public void actionPerformed(ActionEvent evt) {
            // Supply a function that will duplicate the provided prototypical
            // template parameters instance (if any) or make a new one otherwise
            addNewParameters(new Function1<Option<TemplateParameters>, TemplateParameters>() {
                @Override public TemplateParameters apply(Option<TemplateParameters> tp) {
                    return tp.map(new MapOp<TemplateParameters, TemplateParameters>() {
                        @Override public TemplateParameters apply(TemplateParameters proto) {
                            return new TemplateParameters(proto.getParamSet(new PioXmlFactory()));
                        }
                    }).getOrElse(TemplateParameters.newEmpty());
                }
            });
        }
    };

    private void addNewParameters(Function1<Option<TemplateParameters>, TemplateParameters> cons) {
        // Where? After the last selected row I suppose (if any) or else
        // at the end.
        final int[] sel = paramTable.getSelectedRows();
        final int where = (sel.length == 0) ?
                          templateGroup.getTemplateParameters().size() :
                          sel[sel.length-1] + 1;

        // If there is a single selected template parameter, it serves as the
        // prototype.
        final Option<TemplateParameters> proto = (sel.length == 1) ?
                new Some<TemplateParameters>((TemplateParameters) templateGroup.getTemplateParameters().get(sel[0]).getDataObject()) :
                None.<TemplateParameters>instance();

        // Make a new template parameters object with the provided constructor.
        final ISPTemplateParameters newParams;
        try {
            newParams = SPDB.get().getFactory().createTemplateParameters(program, null);
            newParams.setDataObject(cons.apply(proto));
            templateGroup.addTemplateParameters(where, newParams);
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    paramTable.getSelectionModel().setSelectionInterval(where, where);
                }
            });
        } catch (SPException ex) {
            DialogUtil.error(ex);
        }
    }

    // Action to remove a template group parameter triplet.
    private final Action deleteAction = new AbstractAction("Delete", Resources.getIcon("eclipse/remove.gif")) {
        { putValue(Action.SHORT_DESCRIPTION, "Delete the selected template observation(s)."); }

        // Confirm the delete with a horrible JOptionPane.  This may be annoying
        // but it would be easy to accidentally remove template parameters and
        // difficult to get them back.
        private boolean confirmDelete(java.util.List<ISPTemplateParameters> sel) {
            final String message = (sel.size() == 1) ?
                                   String.format("Delete the template observation %s?", ReadableNodeName.format(sel.get(0))) :
                                   String.format("Delete %d template observations?", sel.size());
            final String cancelOption = "Cancel";
            final String[] options    = {"Yes, delete", cancelOption};
            return JOptionPane.showOptionDialog(
                        paramTable,
                        message,
                        "Confirm Delete",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        cancelOption) == 0;
        }

        public void actionPerformed(ActionEvent evt) {
            final java.util.List<ISPTemplateParameters> sel = getSelectedParameters();
            if ((sel.size() > 0) && confirmDelete(sel)) {
                final java.util.List<ISPTemplateParameters> all = templateGroup.getTemplateParameters();
                if (all.removeAll(sel)) {
                    try {
                        templateGroup.setTemplateParameters(all);
                    } catch (SPException ex) {
                        DialogUtil.error(ex);
                    }
                }
            }
        }
    };

    // Action to instantiate templates
    private final Action instantiateAction = new AbstractAction("Apply...") {
        public void actionPerformed(ActionEvent evt) {
            try {
                InstantiationDialog.open(getParent(), program, templateGroup);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to fork a template group
    private final Action splitAction = new AbstractAction("Split...") {
        public void actionPerformed(ActionEvent evt) {
            try {
                SplitDialog.open(getParent(), templateGroup);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    public EdTemplateGroupFooter(JTable paramTable) {
        this.paramTable = paramTable;

        setLayout(new GridBagLayout());
        StaffBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                updateLayout();
            }
        });
    }

    // Handle selection updates in the editor for "staff".
    private final ListSelectionListener staffSelectionListener = new ListSelectionListener() {
        @Override public void valueChanged(ListSelectionEvent evt) {
            if (!evt.getValueIsAdjusting()) handleSelectionUpdate();
        }
    };

    // A list of ISPTemplateParameter corresponding to the selected rows, if any.
    private java.util.List<ISPTemplateParameters> getSelectedParameters() {
        final java.util.List<ISPTemplateParameters> sel = new ArrayList<ISPTemplateParameters>();
        for (int r : paramTable.getSelectedRows()) {
            sel.add(templateGroup.getTemplateParameters().get(r));
        }
        return sel;
    }

    private void handleSelectionUpdate() {
        final int selCount = paramTable.getSelectedRowCount();
        deleteAction.setEnabled(selCount > 0);
        dupAction.setEnabled(selCount == 1);
    }

    private void updateLayout() {
        cleanup();
        final SPProgramID pid = (program == null) ? null : program.getProgramID();
        if ((pid != null) && OTOptions.isStaff(pid)) staffLayout(); else piLayout();
    }

    private void cleanup() {
        paramTable.getSelectionModel().removeListSelectionListener(staffSelectionListener);
        removeAll();
    }

    private void piLayout() {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(new JButton(instantiateAction), new GBC(0, 0));
        add(new JButton(splitAction), new GBC(1, 0));
    }

    private JButton flatButton(Action a) {
        return new JButton(a) {{
            setHideActionText(true);
            ButtonFlattener.flatten(this);
        }};
    }

    private void staffLayout() {
        setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));

        final Insets zero = new Insets(0,0,0,0);

        final JPanel editActions = new JPanel(new GridBagLayout());
        editActions.add(flatButton(addAction), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, CENTER, NONE, zero, 0, 0
        ));
        editActions.add(flatButton(deleteAction), new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, CENTER, NONE, zero, 0, 0
        ));
        editActions.add(flatButton(dupAction), new GridBagConstraints(
            2, 0, 1, 1, 0.0, 0.0, CENTER, NONE, new Insets(0, 20, 0, 0), 0, 0
        ));
        editActions.add(new JPanel(), new GridBagConstraints(
            3, 0, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, zero, 0, 0
        ));

        final JPanel templateActions = new JPanel(new GridBagLayout());
        templateActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));
        templateActions.add(new JButton(instantiateAction), new GBC(0,0));
        templateActions.add(new JButton(splitAction),       new GBC(1,0));

        add(editActions,     new GridBagConstraints(
            0, 0, 1, 1, 1.0, 0.0, EAST, HORIZONTAL, zero, 0, 0
        ));
        add(templateActions, new GridBagConstraints(
            0, 1, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, zero, 0, 0
        ));

        paramTable.getSelectionModel().addListSelectionListener(staffSelectionListener);
        handleSelectionUpdate();
    }

    public void init(ISPProgram program, ISPTemplateGroup templateGroup) {
        this.program = program;
        this.templateGroup = templateGroup;
        updateLayout();
    }
}


// Trivial subclass for our specific local use.
class GBC extends GridBagConstraints {
    {
        fill = HORIZONTAL;
        anchor = EAST;
    }

    public GBC(int gridx, int gridy) {
        this(gridx, gridy, false);
    }

    public GBC(int gridx, int gridy, boolean grab) {
        this.gridx = gridx;
        this.gridy = gridy;
        insets = new Insets(0, 3, 1, 3);
        if (grab)
            this.weightx = 100;
    }

    public GBC(int gridx, int gridy, Insets insets) {
        this(gridx, gridy, 1, 1, insets);
    }

    public GBC(int gridx, int gridy, Insets insets, boolean grab) {
        this(gridx, gridy, 1, 1, insets);
        if (grab)
            this.weightx = 100;
    }

    public GBC(int gridx, int gridy, int anchor) {
        this(gridx, gridy);
        this.anchor = anchor;
    }

    public GBC(int gridx, int gridy, int xspan, int yspan, Insets insets) {
        this(gridx, gridy);
        this.gridwidth = xspan;
        this.gridheight = yspan;
        if (insets != null) {
            final Insets prev = this.insets;
            this.insets = new Insets(
                    prev.top + insets.top,
                    prev.left + insets.left,
                    prev.bottom + insets.bottom,
                    prev.right + insets.right);
        }
    }
}

