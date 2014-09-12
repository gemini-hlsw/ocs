package edu.gemini.spModel.target.offset;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPUtil;

import java.beans.PropertyChangeListener;
import java.util.*;

public final class OffsetPosSelection {
    public static final String KEY  = OffsetPosSelection.class.getName();
    public static final String PROP = SPUtil.getTransientClientDataPropertyName(KEY);

    public static final OffsetPosSelection EMPTY = new OffsetPosSelection(set());
    public static final OffsetPosSelection FIRST = new OffsetPosSelection(set(0));

    private static SortedSet<Integer> set(int ... indices) {
        final SortedSet<Integer> res = new TreeSet<Integer>();
        for (int index : indices) res.add(index);
        return Collections.unmodifiableSortedSet(res);
    }

    private static SortedSet<Integer> lookup(ISPNode node) {
        Object cd = node.getTransientClientData(KEY);
        if (cd == null) return EMPTY.indices;
        //noinspection unchecked
        return (SortedSet<Integer>) cd;
    }

    public static OffsetPosSelection apply(ISPNode node) {
        return new OffsetPosSelection(lookup(node));
    }

    public static OffsetPosSelection select(int ... indices) {
        return new OffsetPosSelection(set(indices));
    }

    public static OffsetPosSelection select(Collection<Integer> indices) {
        return new OffsetPosSelection(Collections.unmodifiableSortedSet(new TreeSet<Integer>(indices)));
    }

    public static <P extends OffsetPosBase> OffsetPosSelection select(OffsetPosList<P> posList, P pos) {
        return select(posList.getPositionIndex(pos));
    }

    public static <P extends OffsetPosBase> OffsetPosSelection select(OffsetPosList<P> posList, List<P> selList) {
        final SortedSet<Integer> res = new TreeSet<Integer>();
        for (P p : selList) {
            final int i = posList.getPositionIndex(p);
            if (i >= 0) res.add(i);
        }
        return new OffsetPosSelection(Collections.unmodifiableSortedSet(res));
    }

    public static <P extends OffsetPosBase> OffsetPosSelection selectAll(OffsetPosList<P> posList) {
        final SortedSet<Integer> res = new TreeSet<Integer>();
        int i = 0;
        while (i < posList.size()) {
            res.add(i);
            ++i;
        }
        return new OffsetPosSelection(Collections.unmodifiableSortedSet(res));
    }


    private final SortedSet<Integer> indices;

    private OffsetPosSelection(SortedSet<Integer> indices) {
        this.indices = indices;
    }

    public <P extends OffsetPosBase> SortedSet<Integer> selectedIndices(OffsetPosList<P> posList) {
        final SortedSet<Integer> filtered = indices.headSet(posList.size());
        return (filtered.size() == indices.size()) ? indices : filtered;
    }

    public <P extends OffsetPosBase> boolean isSelected(OffsetPosList<P> posList, P pos) {
        final int index = posList.getPositionIndex(pos);
        return (index != OffsetPosList.UNKNOWN_INDEX) && selectedIndices(posList).contains(index);
    }

    public <P extends OffsetPosBase> P firstSelectedPosition(OffsetPosList<P> posList) {
        SortedSet<Integer> s = selectedIndices(posList);
        if (s.size() == 0) {
            return null;
        } else {
            return posList.getPositionAt(s.first());
        }
    }

    public <P extends OffsetPosBase> List<P> selectedPositions(OffsetPosList<P> posList) {
        final List<P> res = new ArrayList<P>();
        for (Integer index : selectedIndices(posList)) res.add(posList.getPositionAt(index));
        return res;
    }

    public OffsetPosSelection add(int ... indices) {
        boolean allPresent = true;
        for (int index : indices) {
            if (!this.indices.contains(index)) {
                allPresent = false;
                break;
            }
        }

        final OffsetPosSelection res;
        if (allPresent) {
            res = this;
        } else {
            final SortedSet<Integer> s = new TreeSet<Integer>(this.indices);
            for (int index : indices) s.add(index);
            res = new OffsetPosSelection(Collections.unmodifiableSortedSet(s));
        }
        return res;
    }

    public <P extends OffsetPosBase> OffsetPosSelection add(OffsetPosList<P> posList, P pos) {
        int index = posList.getPositionIndex(pos);
        return (index < 0) ? this : add(index);
    }

    public OffsetPosSelection remove(int ... indices) {
        boolean nonePresent = true;
        for (int index : indices) {
            if (this.indices.contains(index)) {
                nonePresent = false;
                break;
            }
        }

        final OffsetPosSelection res;
        if (nonePresent) {
            res = this;
        } else {
            final SortedSet<Integer> s = new TreeSet<Integer>(this.indices);
            for (int index : indices) s.remove(index);
            res = new OffsetPosSelection(Collections.unmodifiableSortedSet(s));
        }
        return res;
    }

    public OffsetPosSelection remove(OffsetPosList<OffsetPosBase> posList, OffsetPosBase pos) {
        int index = posList.getPositionIndex(pos);
        return (index < 0) ? this : remove(index);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OffsetPosSelection that = (OffsetPosSelection) o;
        return indices.equals(that.indices);
    }

    @Override public int hashCode() {
        return indices.hashCode();
    }

    public void commit(ISPNode node) {
        if (node != null) {
            Set<Integer> s = lookup(node);
            if (!s.equals(indices)) {
                node.putTransientClientData(KEY, indices);
            }
        }
    }

    public static void listenTo(ISPNode node, PropertyChangeListener listener) {
        if (node != null) node.addTransientPropertyChangeListener(PROP, listener);
    }

    public static void deafTo(ISPNode node, PropertyChangeListener listener) {
        if (node != null) node.removeTransientPropertyChangeListener(PROP, listener);
    }
}
