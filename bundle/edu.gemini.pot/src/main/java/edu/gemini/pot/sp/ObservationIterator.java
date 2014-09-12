package edu.gemini.pot.sp;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over all the observations contained in a given node and its
 * descendants (including template observations).
 */
public final class ObservationIterator implements Iterable<ISPObservation> {
    private final ISPNode node;
    public ObservationIterator(ISPNode node) {
        this.node = node;
    }

    public Iterator<ISPObservation> iterator() {
        return apply(node);
    }

    public static Iterator<ISPObservation> apply(ISPNode node) {
        if (node instanceof ISPObservation) {
            return new SingleObservationIterator((ISPObservation) node);
        } else if (node instanceof ISPContainerNode) {
            return new ContainerObservationIterator((ISPContainerNode) node);
        } else {
            return EMPTY;
        }
    }

    private static abstract class BaseObservationIterator implements Iterator<ISPObservation> {
        @Override public void remove() { throw new UnsupportedOperationException(); }
    }

    private static final Iterator<ISPObservation> EMPTY = new BaseObservationIterator() {
        @Override public boolean hasNext() { return false; }
        @Override public ISPObservation next() { throw new NoSuchElementException();}
    };

    private static class SingleObservationIterator extends BaseObservationIterator {
        private final ISPObservation obs;
        private boolean hasNext = true;

        SingleObservationIterator(ISPObservation obs) { this.obs = obs; }
        @Override public boolean hasNext() { return hasNext; }

        @Override public ISPObservation next() {
            if (!hasNext) throw new NoSuchElementException();
            hasNext = false;
            return obs;
        }
    }

    private static class ContainerObservationIterator extends BaseObservationIterator {
        private final Iterator<ISPNode> childIterator;
        private Iterator<ISPObservation> cur;

        ContainerObservationIterator(ISPContainerNode c) {
            childIterator = c.getChildren().iterator();
            cur = advance(childIterator);
        }

        private static Iterator<ISPObservation> advance(Iterator<ISPNode> it) {
            if (!it.hasNext()) return EMPTY;
            ISPNode child = it.next();
            Iterator<ISPObservation> obsIt = apply(child);
            return (obsIt.hasNext()) ? obsIt : advance(it);
        }

        @Override public boolean hasNext() {
            if (cur.hasNext()) return true;
            cur = advance(childIterator);
            return cur.hasNext();
        }

        @Override public ISPObservation next() {
            return cur.next();
        }
    }
}
