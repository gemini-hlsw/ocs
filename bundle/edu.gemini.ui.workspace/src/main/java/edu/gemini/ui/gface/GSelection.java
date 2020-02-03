package edu.gemini.ui.gface;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Immutable selection, which is basically a Set of E.
 * @author rnorris
 * @param <E>
 */
@SuppressWarnings("unchecked")
public final class GSelection<E> implements Iterable<E>, Transferable {

    private final E[] store;
    private final Map<Object, Object> properties = new HashMap<>();

    private static final Map<Class<?>, DataFlavor> FLAVORS = new HashMap<>();

    /**
     * This collection is initialized to the set of all types that are implemented
     * by all elements in the collection. Therefore it always contains Object.class,
     * but may contain any number of other classes and/or interfaces. The set of
     * DataFlavors for the Transferable implementation is built from this info.
     */
    private final Set<Class<?>> commonElementTypes = new HashSet<>();
    private final Set<DataFlavor> commonFlavors = new HashSet<>();

    public GSelection(E... elements) {
        store = (E[]) Array.newInstance(elements.getClass().getComponentType(), elements.length);
        System.arraycopy(elements, 0, store, 0, elements.length);

        // Determine the set of common element types
        commonElementTypes.add(Object.class); // always

        // Fill commonElementTypes with all of first()'s types
        if (store.length != 0) {
            LinkedList<Class<?>> queue = new LinkedList<>();
            queue.add(store[0].getClass());
            while (!queue.isEmpty()) {
                Class<?> c = queue.removeFirst();
                commonElementTypes.add(c);
                Class<?> sup = c.getSuperclass();
                if (sup != null)
                    queue.add(sup);
                Collections.addAll(queue, c.getInterfaces());
            }
        }

        // Now examine remaining elements, removing any classes that
        // aren't common to the rest.
        for (int i = 1; i < elements.length; i++)
            for (Iterator<Class<?>> it = commonElementTypes.iterator(); it.hasNext(); )
                if (!it.next().isInstance(elements[i]))
                    it.remove();

        // Create the data flavor set. This is an optimization so calls
        // isDataFlavorSupported() tend to be O(1)
        for (Class<?> c: commonElementTypes)
            commonFlavors.add(flavorForSelectionOf(c));

    }

    public boolean isEmpty() {
        return store.length == 0;
    }

    public E first() {
        return store[0];
    }

    public E last() {
        return store[store.length - 1];
    }

    public int size() {
        return store.length;
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private int i = 0;

            public boolean hasNext() {
                return i < store.length;
            }

            public E next() {
                return store[i++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public static <E> GSelection<E> emptySelection() {
        return new GSelection<>();
    }

    public boolean contains(Object o) {
        for (E e: store)
            if (o == e || (o != null && o.equals(e))) return true;
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GSelection && Arrays.equals(store, ((GSelection<E>) obj).store);
    }

    @Override
    public int hashCode() {
        return (store.length == 0 || store[0] == null) ? 0 : store[0].hashCode();
    }

    @Override
    public String toString() {
        return Arrays.toString(store);
    }

    /**
     * Returns a copy of this GSelection with the contents of toAdd appended to the end.
     */
    public GSelection<E> plus(GSelection<E> toAdd) {
        List<E> accum = new ArrayList<>();
        for (E o: this) accum.add(o);
        for (E o: toAdd) accum.add(o);
        return new GSelection<>(accum.toArray((E[]) Array.newInstance(store.getClass().getComponentType(), accum.size())));
    }

    /**
     * Returns a copy of this GSelection with the specified elements removed.
     */
    public GSelection<E> minus(GSelection<E> toRemove) {
        Set<E> set = new HashSet<>();
        for (E o: toRemove) set.add(o);
        ArrayList<E> accum = new ArrayList<>();
        for (E o: this) {
            if (!set.contains(o))
                accum.add(o);
        }
        return new GSelection<>(accum.toArray((E[]) Array.newInstance(store.getClass().getComponentType(), accum.size())));
    }

    public <M> GSelection<E> translate(GTranslator<?, ?> translator) {
        if (translator == null) return this;
        ArrayList ret = new ArrayList<>();
        for (E e: store) {
            ret.addAll(translator.translate(e));
        }
        return new GSelection(ret.toArray());
    }

    public <T> T[] toArray(Class<T> type) {
        T[] ret = (T[]) Array.newInstance(type, store.length);
        System.arraycopy(store, 0, ret, 0, ret.length);
        return ret;
    }

    public boolean isSelectionOf(Class<?> type) {
        return commonElementTypes.contains(type);
    }

    public void sort() {
        Arrays.sort(store);
    }

    public void sort(Comparator<E> comparator) {
        Arrays.sort(store, comparator);
    }

    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public static synchronized DataFlavor flavorForSelectionOf(Class<?> c) {
        DataFlavor flav = FLAVORS.get(c);
        if (flav == null) {
            flav = new DataFlavor(GSelection.class, "GSelection<" + c.getSimpleName() + ">");
            FLAVORS.put(c, flav);
        }
        return flav;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (commonFlavors.contains(flavor)) return this;
        throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return commonFlavors.toArray(new DataFlavor[commonFlavors.size()]);
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return commonFlavors.contains(flavor);
    }


}
