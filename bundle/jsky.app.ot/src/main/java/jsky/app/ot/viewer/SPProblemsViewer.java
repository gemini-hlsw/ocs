package jsky.app.ot.viewer;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.util.P2CheckerUtil;
import jsky.app.ot.ui.util.UIConstants;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.*;
import java.awt.Component;

/**
 * Controller for the Problem Viewer, in charge of displaying the problems found
 * in any particular node
 */
public class SPProblemsViewer {
    private SPProblemsPanel _panel;
    private ProblemsDataModel _tableModel;

    // Popup menu to ignore a selected error/warning
    private JPopupMenu _menu;
    private boolean _ignoreMenuChange;


    public SPProblemsViewer(SPViewer viewer) {
        _panel = new SPProblemsPanel();
        _tableModel = new ProblemsDataModel(viewer);
        _panel.getTable().setModel(_tableModel);

        for (ProblemsDataModel.Col cols: ProblemsDataModel.Col.values()) {
            TableColumn tc = _panel.getTable().getColumn(cols.displayValue());
            tc.setCellRenderer(cols.getRenderer());
        }
        _panel.getTable().getColumnModel().getColumn(0).setMaxWidth(19);

        // UX-1520: OTR slow to load programs.  Turning off problem ignore
        // for now.  On cpoodb, one particular program takes 3 minutes to load
        // with this feature but only 8 seconds without it.
//        if (OTOptions.isStaff()) {
//            _addMenu();
//        }
    }
/* UX-1520
    private void _addMenu() {
        _menu = new JPopupMenu();
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem("Ignore");
        _menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                _ignoreMenuChange = true;
                try {
                    //noinspection EmptyCatchBlock
                    try {
                        int sel = _panel.getTable().getSelectedRow();
                        item.setVisible(sel != -1);
                        item.setSelected(_tableModel.isIgnored(sel));
                    } catch (RemoteException e1) {
                    }
                } finally {
                    _ignoreMenuChange = false;
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!_ignoreMenuChange) {
                    // show some feedback, since it can be slow for large programs
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BusyWin.setBusy(true);
                                _tableModel.setIgnored(_panel.getTable().getSelectedRow(), item.isSelected());
                            } catch (RemoteException e1) {
                                DialogUtil.error(e1);
                            } finally {
                                BusyWin.setBusy(false);
                            }
                        }
                    });
                }
            }
        });
        _menu.add(item);

        _panel.getTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int row = _panel.getTable().getSelectedRow();
                if (e.getButton() == MouseEvent.BUTTON1 || e.isPopupTrigger()) {
                    Problem p = _tableModel.getProblem(row);
                    if (p == null || p instanceof ProblemRollup || ((Problem)p).getType() != Problem.Type.WARNING) {
                        return; // can't change the top level summaries, only warnings (REL-383)
                    }
                    _menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
    }
    */

    /**
     * Set the Remote Node this problem viewer will be getting problems from
     * If the remote node is <code>null</code> the problem viewer won't show
     * any problems
     * @param node The remote node from where problems want to be reported. If
     * is <code>null</code> the problem viewer will be reset to be empty
     * (can be used to reset the problem viewer to a clean state for instance)
     */
    public void setNodeData(NodeData node) {
        _tableModel.setNodeData(node);
    }

    public void update() {
        _tableModel.update();
    }


    public static class MultiLineRenderer extends JTextArea implements TableCellRenderer {
        private final DefaultTableCellRenderer adaptee =
                new DefaultTableCellRenderer();

        public MultiLineRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        public Component getTableCellRendererComponent(JTable jTable, Object object,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            adaptee.getTableCellRendererComponent(jTable, object, isSelected, hasFocus, row, column);
            setForeground(adaptee.getForeground());
            setBackground(adaptee.getBackground());
            setBorder(adaptee.getBorder());
            setFont(adaptee.getFont());
            setText(adaptee.getText());

            // As suggested by BUG ID: 4760433: It is impossible to obtain
            // preferred height of JTextArea until its width is set.
            int pad = jTable.getIntercellSpacing().width*2;
            TableColumnModel columnModel = jTable.getColumnModel();
            setSize(columnModel.getColumn(column).getWidth()-pad, Integer.MAX_VALUE);

            int height_wanted = (int) getPreferredSize().getHeight();
            if (height_wanted != jTable.getRowHeight(row)) {
                jTable.setRowHeight(row, height_wanted);
            }
            return this;
        }
    }


    public static class ProblemTypeRenderer extends DefaultTableCellRenderer {

        protected void setValue(Object object) {
            if (object instanceof Problem) {
                Problem problem = (Problem)object;
                Problem.Type type = problem.getType();
                boolean ignored = isIgnored(problem);
                if (type == Problem.Type.WARNING) {
                    setIcon(ignored ? UIConstants.IGNORED_WARNING_ICON : UIConstants.WARNING_ICON);
                } else if (type == Problem.Type.ERROR) {
                    setIcon(ignored ? UIConstants.IGNORED_ERROR_ICON : UIConstants.ERROR_ICON);
                }
            }
        }

        private final boolean isIgnored(Problem problem) {
            return false;
            /* UX-1520: turning this off for now
            if (problem instanceof ProblemRollup && problem.getDescription().contains(" ignored ")) {
                return true; // program node summary
            }
            List<String> ignoredProblems = getIgnoredProblems(problem.getAffectedNode());
            return ignoredProblems != null && ignoredProblems.contains(problem.getId());
            */
        }
    }

    public static class ProblemsDataModel extends DefaultTableModel {

        public enum Col {
            TYPE(" ", new ProblemTypeRenderer(), Problem.class) {
                public Object getValue(Problem problem) {
                    return problem;
                }},
            DESCRIPTION("Description", new MultiLineRenderer(), String.class) {
                public Object getValue(Problem problem) {
                    return problem.getDescription();
                }};

            private String _displayValue;

            private TableCellRenderer _renderer;

            private Class _type;

            Col(String displayValue, TableCellRenderer renderer, Class type) {
                _displayValue = displayValue;
                _renderer = (renderer == null ) ? new DefaultTableCellRenderer() : renderer;
                _type = type;
            }

            public String displayValue() {
                return _displayValue;
            }

            public TableCellRenderer getRenderer() {
                return _renderer;
            }

            public Class getType() {
                return _type;
            }


            public abstract Object getValue(Problem problem);
        }

        /**
         * Reference to the viewer that's showing the observation being evaluated
         */
        private SPViewer _viewer;
        private IP2Problems _problems;
        private NodeData _node; //the node whose information is shown

        ProblemsDataModel(SPViewer viewer) {
            _viewer = viewer;
        }

        private IP2Problems _getProblems() {
            if (_problems == null) {
                _problems = P2CheckerUtil.NO_PROBLEMS;
            }
            return _problems;
        }

        public void setNodeData(NodeData node) {
                _node = node;
                _updateProblems();
                fireTableDataChanged();
        }

        public NodeData getNode() {
            return _node;
        }

        private void _updateProblems() {
            //Notify the viewer of the new problems being shown, only if
            //the engine is set up
            _problems = (_node == null) ? P2CheckerUtil.NO_PROBLEMS : _node.getProblems();
            _viewer.updateProblemToolWindow(_problems);
        }

        public int getColumnCount() {
            return Col.values().length;
        }

        public String getColumnName(int i) {
            return Col.values()[i].displayValue();
        }

        public int getRowCount() {
            return _getProblems().getProblemCount();
        }

        public Object getValueAt(int row, int col) {
            if (_getProblems().getProblemCount() < row) return null;
            Problem problem = _getProblems().getProblems().get(row);
            return Col.values()[col].getValue(problem);
        }

        public Class<?> getColumnClass(int i) {
            return Col.values()[i].getType();
        }


        public boolean isCellEditable(int row, int col) {
            return false;
        }


        public void update() {
            _updateProblems();
            fireTableDataChanged();
        }

        /**
         * Ignore (or stop ignoring) the problem at the specified row
         * @param row the selected row
         * @param ignore true if the problem should be ignored
         */
        /* UX-1520
        public void setIgnored(int row, boolean ignore)  {
            Problem problem = getProblem(row);
            if (problem != null && _node != null) {
                List<String> ignoredProblems = getIgnoredProblems(_viewer.getProgram());
                if (ignore) {
                    if (!ignoredProblems.contains(problem.getId())) {
                        ignoredProblems.add(problem.getId());
                    }
                } else {
                    ignoredProblems.remove(problem.getId());
                }
                try {
                    SPProgram spProgram = (SPProgram)DataObjectManager.getDataObject(_viewer.getProgram());
                    spProgram.setIgnoredProblems(ignoredProblems);
                    DataObjectManager.setDataObject(_viewer.getProgram(), spProgram);
                    fireTableDataChanged();
                    _viewer.getTree().getJTree().repaint();
//                    _viewer.checkCurrentProgram();
                } catch (RemoteException ignored) {
                }
            }
        }
        */

        /**
         * Returns true if the problem at the given row should be ignored
         * @param row selected table row
         */
        /* UX-1520
        public boolean isIgnored(int row)  {
            Problem problem = getProblem(row);
            if (problem != null) {
                List<String> ignoreProblems = getIgnoredProblems(_viewer.getProgram());
                return ignoreProblems.contains(problem.getId());
            }
            return false;
        }
        */

        public Problem getProblem(int row) {
            if (row < 0 || row >= _getProblems().getProblemCount()) return null;
            return _getProblems().getProblems().get(row);
        }
    }

    public JPanel getPanel() {
        return _panel;
    }

    /*  UX-1520
    public static List<String> getIgnoredProblems(ISPNode node) {
        List<String> result = null;
        try {
            ISPNode p = node;
            while (!(p instanceof ISPProgram)) {
                if (p == null) return new ArrayList<String>();
                p = p.getParent();
            }
            SPProgram spProgram = (SPProgram)DataObjectManager.getDataObject(p);
            result = spProgram.getIgnoredProblems();
            if (result == null) {
                result = new ArrayList<String>();
            }
        } catch (RemoteException ignored) {
        }
        return result;
    }
    */
}
