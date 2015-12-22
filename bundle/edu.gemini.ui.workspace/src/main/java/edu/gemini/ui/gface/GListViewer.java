package edu.gemini.ui.gface;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;

@SuppressWarnings("serial")
public class GListViewer<M, E> extends GViewer<M, E> {

    private final JList<E> list;

    private volatile boolean pushingSelection;

    public GListViewer(final GListController<M, E> controller) {
        this(controller, new JList<>());
    }

    public GListViewer(final GListController<M, E> controller, final JList<E> list) {
        super(controller, list);
        this.list = list;
        list.setCellRenderer(renderer);
        list.setModel(new DefaultListModel<>());
        list.addListSelectionListener(e -> {
            if (pushingSelection || e.getValueIsAdjusting()) return;
            pullSelection();
        });
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void pullSelection() {
        setPulledSelection(new GSelection<>((E[]) list.getSelectedValues()));
    }

    public JList<E> getList() {
        return list;
    }

    @Override
    public GListController<M, E> getController() {
        return (GListController<M, E>) super.getController();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh() {
        invokeLater(refreshTask);
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
            GListController<M, E> controller = getController();
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

            // Set the new model and restore the selection. Order here is important.
            GSelection<E> oldSelection = getSelection();
            list.setIgnoreRepaint(true);
            pushingSelection = true;
            list.setModel(new ImmutableListModel(accum));
            new SetSelectionTask(oldSelection).run();
            list.setIgnoreRepaint(false);
        }

    };

    private class SetSelectionTask implements Runnable {

        private final Set<E> set = new HashSet<>();

        public SetSelectionTask(final GSelection<E> newSelection) {
            for (E o: newSelection)
                set.add(o);
        }

        @SuppressWarnings("unchecked")
        public void run() {
            pushingSelection = true;
            ListSelectionModel lsm = list.getSelectionModel();
            lsm.setValueIsAdjusting(true);
            lsm.clearSelection();
            ImmutableListModel<E> lm = (ImmutableListModel<E>) list.getModel();
            for (int i = 0; i < lm.getSize(); i++)
                if (set.contains(lm.getElementAt(i))) {
                    lsm.addSelectionInterval(i, i);
                    list.scrollRectToVisible(list.getCellBounds(i, i));
                }
            pushingSelection = false;
            lsm.setValueIsAdjusting(false);
            pullSelection(); // selection may have changed
        }

    }

    private final ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
        @SuppressWarnings("unchecked")
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
            GElementDecorator<M, E> labelDecorator = getDecorator();
            if (labelDecorator != null)
                labelDecorator.decorate(label, (E) value);
            return label;
        }
    };

    public GElementDecorator<M, E> getDecorator() {
        return (GElementDecorator<M, E>) super.getDecorator();
    }

    public void setDecorator(GElementDecorator<M, E> decorator) {
        super.setDecorator(decorator);
    }

    static class ImmutableListModel<E> implements ListModel<E> {

        private final E[] store;

        @SuppressWarnings("unchecked")
        public ImmutableListModel(Collection<E> elements) {
            store = (E[])elements.toArray();
        }

        @SuppressWarnings("unchecked")
        public E getElementAt(int index) {
            return store[index];
        }

        public int getSize() {
            return store.length;
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }

    }

    @Override
    protected Runnable getSelectionTask(GSelection<E> newSelection) {
        return new SetSelectionTask(newSelection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E getElementAt(Point p) {
        ImmutableListModel<E> lm = (ImmutableListModel<E>) list.getModel();
        int row = list.locationToIndex(p);
        return row == -1 ? null : lm.getElementAt(row);
    }

}



