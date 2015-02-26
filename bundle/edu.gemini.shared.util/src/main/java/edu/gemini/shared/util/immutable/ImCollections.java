//
// $
//

package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.*;

/**
 * Utility methods for working with immutable collections.
 */
public class ImCollections {

    public static final ImList EMPTY_LIST = new EmptyList();

    @SuppressWarnings({"unchecked"})
    private static class EmptyList implements ImList<Object>, Serializable {

        @Override
        public ImList<Object> cons(Object o) {
            List<Object> lst = new ArrayList<Object>();
            lst.add(o);
            return new DefaultImList<Object>(lst);
        }

        @Override
        public ImList<Object> append(ImList<?> tail) {
            return (ImList<Object>) tail;
        }

        @Override
        public ImList<Object> append(Object o) {
            return cons(o);
        }

        @Override
        public ImList<Object> remove(Object o) {
            return this;
        }

        @Override
        public ImList<Object> remove(Function1<? super Object, Boolean> f) {
            return this;
        }

        @Override
        public ImList<Object> updated(int index, Object o) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        @Override
        public Object head() {
            return null;
        }

        @Override
        public Option<Object> headOption() {
            return None.INSTANCE;
        }

        @Override
        public ImList<Object> tail() {
            return this;
        }

        @Override
        public Object last() {
            return null;
        }

        @Override
        public ImList<Object> initial() {
            return this;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(ImList<?> c) {
            return c.size() == 0;
        }

        @Override
        public Object get(int index) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        @Override
        public int indexOf(Object o) {
            return -1;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean nonEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public List<Object> toList() {
            return Collections.emptyList();
        }

        @Override
        public <U> ImList<U> map(Function1<? super Object, U> op) {
            return (ImList<U>) this;
        }

        @Override
        public <U> ImList<U> flatMap(Function1<? super Object, ImList<U>> op) {
            return (ImList<U>) this;
        }

        @Override
        public void foreach(ApplyOp<? super Object> op) {
        }

        @Override
        public Option<Object> find(Function1<? super Object, Boolean> op) {
            return None.instance();
        }

        @Override
        public ImList<Object> filter(Function1<? super Object, Boolean> op) {
            return this;
        }

        @Override
        public Tuple2<ImList<Object>, ImList<Object>> partition(Function1<? super Object, Boolean> op) {
            return new Pair(this, this);
        }

        @Override
        public boolean forall(Function1<? super Object, Boolean> op) {
            return true;
        }

        @Override
        public boolean exists(Function1<? super Object, Boolean> op) {
            return false;
        }

        @Override
        public String mkString(String prefix, String separator, String suffix) {
            return prefix + suffix;
        }

        @Override
        public <U> ImList<Tuple2<Object, U>> zip(ImList<U> list) {
            return emptyList();
        }

        @Override
        public ImList<Tuple2<Object, Integer>> zipWithIndex() {
            return emptyList();
        }

        @Override
        public ImList<Object> sort(Comparator<? super Object> c) {
            return this;
        }

        @Override
        public Object min(Comparator<? super Object> c) {
            throw new NoSuchElementException();
        }

        @Override
        public Object max(Comparator<? super Object> c) {
            throw new NoSuchElementException();
        }

        @Override
        public ImList<Object> reverse() {
            return this;
        }

        /**
         * @return <code>start</code>, since there are no other elements to
         * combine it with
         */
        @Override
        public <U> U foldLeft(U start, Function2<U, ? super Object, U> op) {
            return start;
        }

        /**
         * @return <code>start</code>, since there are no other elements to
         * combine it with
         */
        @Override
        public <U> U foldRight(U start, Function2<? super Object, U, U> op) {
            return start;
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyList().iterator();
        }

        private Object readResolve() {
            return EMPTY_LIST;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ImList)) return false;

            ImList that = (ImList) o;
            return (that.size() == 0);
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    /**
     * Returns an empty immutable list.
     *
     * @param <T> type of elements associated with the list
     *
     * @return an empty immutable list
     */
    public static <T> ImList<T> emptyList() {
        //noinspection unchecked
        return (ImList<T>) EMPTY_LIST;
    }

    /**
     * Returns an immutable list containing a single item.
     *
     * @param item the item contained in the list
     *
     * @param <T> type of the elements associated with the list
     *
     * @return a list consisting of a single element
     */
    public static <T> ImList<T> singletonList(T item) {
        return DefaultImList.create(Collections.singletonList(item));
    }

    /**
     * Takes an immutable list of tuples and returns a tuple of two lists.
     * Essentially this method undoes the work of the {@link ImList#zip} method.
     *
     * @param list list of tuples to unzip
     *
     * @param <T> type of the first element of the tuple
     * @param <U> type of the second element of the tuple
     *
     * @return a tuple containing two lists, one for each element of the
     * tuples contained in the input list
     */
    public static <T, U> Tuple2<ImList<T>, ImList<U>> unzip(ImList<Tuple2<T, U>> list) {

        // Kind of inefficient to go through the list twice ...
        ImList<T> uz1 = list.map(new MapOp<Tuple2<T, U>, T>() {
            @Override public T apply(Tuple2<T, U> tup) { return tup._1(); }
        });
        ImList<U> uz2 = list.map(new MapOp<Tuple2<T, U>, U>() {
            @Override public U apply(Tuple2<T, U> tup) { return tup._2(); }
        });
        return new Pair<ImList<T>, ImList<U>>(uz1, uz2);
    }
}
