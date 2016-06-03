package edu.gemini.ui.gface;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
public class GTableViewer<M, E, S> extends GViewer<M, E> {

    private static final Logger LOGGER = Logger.getLogger(GTableViewer.class
            .getName());
    private final JTable table;

    private volatile boolean pushingSelection;

    private S[] columns;

    public GTableViewer(final GTableController<M, E, S> controller) {
        this(controller, new JTable());
    }

    public GTableViewer(final GTableController<M, E, S> controller,
            final JTable table) {
        super(controller, table);
        this.table = table;

        // TODO: allow reordering. for now it won't work if there are column
        // decorators
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, renderer);
        table.setModel(new DefaultTableModel());
        table.getSelectionModel().addListSelectionListener(
                e -> {
                    if (pushingSelection || e.getValueIsAdjusting())
                        return;
                    pullSelection();
                });
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void pullSelection() {

        // Create a new selection from the table model.
        ImmutableTableModel model = (ImmutableTableModel) table.getModel();
        ArrayList<E> accum = new ArrayList<>();
        ListSelectionModel lsm = table.getSelectionModel();
        for (int i = lsm.getMinSelectionIndex(); i <= lsm
                .getMaxSelectionIndex(); i++) {
            if (lsm.isSelectedIndex(i))
                accum.add(model.getElementAt(i));
        }

        setPulledSelection(new GSelection(accum.toArray()));

    }

    public JTable getTable() {
        return table;
    }

    @Override
    public GTableController<M, E, S> getController() {
        return (GTableController<M, E, S>) super.getController();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh() {
        invokeLater(refreshTask);
    }

    public void setDecorator(GSubElementDecorator<M, E, S> decorator) {
        super.setDecorator(decorator);
    }

    public GSubElementDecorator<M, E, S> getDecorator() {
        return (GSubElementDecorator<M, E, S>) super.getDecorator();
    }

    public void setColumns(S... columns) {
        this.columns = columns;
        try {
            if (SwingUtilities.isEventDispatchThread())
                refreshTask.run();
            else
                SwingUtilities.invokeAndWait(refreshTask);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.WARNING, "Trouble setting columns.", e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    public S[] getColumns() {
        S[] ret = (S[]) Array.newInstance(
                columns.getClass().getComponentType(), columns.length);
        System.arraycopy(columns, 0, ret, 0, ret.length);
        return ret;
    }

    private void invokeLater(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private final Runnable refreshTask = new Runnable() {

        @SuppressWarnings("unchecked")
        public void run() {

            // Set up to copy the elements.
            GTableController<M, E, S> controller = getController();
            int size = controller.getElementCount();
            ArrayList<E> accum = new ArrayList<>(size);

            // Copy and filter.
            for (int i = 0; i < size; i++) {
                E e = controller.getElementAt(i);
                GFilter<M, E> filter = getFilter();
                if (filter == null || filter.accept(e))
                    accum.add(e);
            }

            // Sort.
            GComparator<M, E> comparator = getComparator();
            if (comparator != null)
                Collections.sort(accum, comparator);

            // Set the new model and restore the selection. Order here is
            // important.
            GSelection<E> oldSelection = getSelection();
            table.setIgnoreRepaint(true);
            pushingSelection = true;

            // We need to preserve the old column widths when the model changes.
            TableColumnModel tcm = table.getColumnModel();
            List<TableColumn> cols = new ArrayList<>();
            for (int i = 0; i < tcm.getColumnCount(); i++)
                cols.add(tcm.getColumn(i));

            table.setModel(new ImmutableTableModel(accum));

            // First time around we don't want to do this
            if (cols.size() > 0) {
                while (tcm.getColumnCount() > 0)
                    tcm.removeColumn(tcm.getColumn(0));

                for (TableColumn col : cols)
                    tcm.addColumn(col);
            }

            new SetSelectionTask(oldSelection).run();
            table.setIgnoreRepaint(false);

        }

    };

    private class SetSelectionTask implements Runnable {

        private final Set<Object> set = new HashSet<>();

        public SetSelectionTask(final GSelection<?> newSelection) {
            for (Object o : newSelection)
                set.add(o);
        }

        @SuppressWarnings("unchecked")
        public void run() {
            pushingSelection = true;
            ListSelectionModel lsm = table.getSelectionModel();
            lsm.setValueIsAdjusting(true);
            lsm.clearSelection();
            ImmutableTableModel lm = (ImmutableTableModel) table.getModel();
            Rectangle revealRect = null;
            for (int i = 0; i < lm.getRowCount(); i++) {
                if (set.contains(lm.getElementAt(i))) {
                    lsm.addSelectionInterval(i, i);
                    if (revealRect == null) // reveal the first selected element
                        revealRect = table.getCellRect(i, 0, true);
                }
            }
            if (revealRect != null)
                table.scrollRectToVisible(revealRect);
            pushingSelection = false;
            lsm.setValueIsAdjusting(false);
            pullSelection(); // selection may have changed
        }

    }

    private final TableCellRenderer renderer = new Renderer();

    public class Renderer extends DefaultTableCellRenderer {

        private RendererCustomizer rendererCustomizer;

        @Override
        @SuppressWarnings("unchecked")
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            rendererCustomizer = null;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                    value, isSelected, false, row, column);
            ImmutableTableModel model = (ImmutableTableModel) table.getModel();
            S subElement = columns[column];
            GSubElementDecorator<M, E, S> decorator = getDecorator();
            if (decorator != null) {
                E element = model.getElementAt(row);
                decorator.decorate(label, element, subElement, value);
            }
            return label;
        }

        @Override
        public void paint(Graphics g) {
            if (rendererCustomizer != null)
                rendererCustomizer.paint(this, g);
            else
                super.paint(g);
        }

    }

    public interface RendererCustomizer {
        void paint(JLabel label, Graphics g);
    }

    @SuppressWarnings("unchecked")
    class ImmutableTableModel implements TableModel {

        private final Object[] store;

        public ImmutableTableModel(Collection<E> elements) {
            store = elements.toArray();
        }

        public void addTableModelListener(TableModelListener l) {
        }

        public Class<?> getColumnClass(int columnIndex) {
            return Object.class;
        }

        public int getColumnCount() {
            return columns == null ? 0 : columns.length;
        }

        public String getColumnName(int columnIndex) {
            return columns[columnIndex].toString();
        }

        public int getRowCount() {
            return store.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return getController().getSubElement(getElementAt(rowIndex),
                    columns[columnIndex]);
        }

        public E getElementAt(int rowIndex) {
            return (E) store[rowIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public void removeTableModelListener(TableModelListener l) {
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

    }

    public void setSelectionMode(int selectionMode) {
        table.setSelectionMode(selectionMode);
    }

    /**
     * Convenience method to fix table column size.
     */
    public void setColumnSize(S subItem, int min, int max) {

        // RCN: this used to be a race-condition triggered bug which I think I
        // fixed, but I'm leaving this check in for now because the problem is
        // kind of hard to diagnose farther down the line. We can remove this
        // check for the 1.0.4 release if it hasn't appeared.
        if (table.getColumnCount() != columns.length)
            throw new RuntimeException("Column count is wrong.");

        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == subItem) {
                TableColumn col = table.getColumnModel().getColumn(i);
                col.setMinWidth(min);
                col.setMaxWidth(max);
                if (min == max)
                    col.setResizable(false);
                return;
            }
        }
        throw new NoSuchElementException(subItem.toString());
    }

    public void setColumnSize(S subItem, int fixedWidth) {
        setColumnSize(subItem, fixedWidth, fixedWidth);
    }

    @Override
    protected Runnable getSelectionTask(GSelection<?> newSelection) {
        return new SetSelectionTask(newSelection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getElementAt(Point p) {
        ImmutableTableModel tm = (ImmutableTableModel) table.getModel();
        int row = table.rowAtPoint(p);
        return row == -1 ? null : tm.getElementAt(row);
    }

}
