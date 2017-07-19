package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * Utility methods for working with immutable collections.
 */
public class ImCollections {

    @SuppressWarnings("rawtypes")
    public static final ImList EMPTY_LIST = new EmptyList();

    @SuppressWarnings({"unchecked"})
    private static class EmptyList implements ImList<Object>, Serializable {

        @Override
        public ImList<Object> cons(final Object o) {
            final List<Object> lst = new ArrayList<>();
            lst.add(o);
            return new DefaultImList<>(lst);
        }

        @Override
        public ImList<Object> append(final ImList<?> tail) {
            return (ImList<Object>) tail;
        }

        @Override
        public ImList<Object> append(final Object o) {
            return cons(o);
        }

        @Override
        public ImList<Object> remove(final Object o) {
            return this;
        }

        @Override
        public ImList<Object> remove(final Function1<? super Object, Boolean> f) {
            return this;
        }

        @Override
        public ImList<Object> updated(final int index, final Object o) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        @Override
        public Object head() {
            return null;
        }

        @Override
        public Option<Object> headOption() {
            return None.instance();
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
        public boolean contains(final Object o) {
            return false;
        }

        @Override
        public boolean containsAll(final ImList<?> c) {
            return c.size() == 0;
        }

        @Override
        public Object get(final int index) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        @Override
        public Option<Object> getOption(final int index) {
            return None.instance();
        }

        @Override
        public int indexOf(final Object o) {
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
        public <U> ImList<U> map(final Function1<? super Object, U> op) {
            return (ImList<U>) this;
        }

        @Override
        public <U> ImList<U> flatMap(final Function1<? super Object, ImList<U>> op) {
            return (ImList<U>) this;
        }

        @Override
        public void foreach(final ApplyOp<? super Object> op) {
        }

        @Override
        public Option<Object> find(final Function1<? super Object, Boolean> op) {
            return None.instance();
        }

        @Override
        public ImList<Object> filter(final Function1<? super Object, Boolean> op) {
            return this;
        }

        @Override
        public Tuple2<ImList<Object>, ImList<Object>> partition(final Function1<? super Object, Boolean> op) {
            return new Pair<>(this, this);
        }

        @Override
        public boolean forall(final Function1<? super Object, Boolean> op) {
            return true;
        }

        @Override
        public boolean exists(final Function1<? super Object, Boolean> op) {
            return false;
        }

        @Override
        public String mkString(final String prefix, final String separator, final String suffix) {
            return prefix + suffix;
        }

        @Override
        public <U> ImList<Tuple2<Object, U>> zip(final ImList<U> list) {
            return emptyList();
        }

        @Override
        public ImList<Tuple2<Object, Integer>> zipWithIndex() {
            return emptyList();
        }

        @Override
        public ImList<Object> sort(final Comparator<? super Object> c) {
            return this;
        }

        @Override
        public Object min(final Comparator<? super Object> c) {
            throw new NoSuchElementException();
        }

        @Override
        public Object max(final Comparator<? super Object> c) {
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
        public <U> U foldLeft(final U start, final Function2<U, ? super Object, U> op) {
            return start;
        }

        /**
         * @return <code>start</code>, since there are no other elements to
         * combine it with
         */
        @Override
        public <U> U foldRight(final U start, final Function2<? super Object, U, U> op) {
            return start;
        }

        @Override
        public Stream<Object> stream() {
            return Stream.empty();
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyList().iterator();
        }

        private Object readResolve() {
            return EMPTY_LIST;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof ImList)) return false;

            final ImList<?> that = (ImList) o;
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
    public static <T> ImList<T> singletonList(final T item) {
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
    public static <T, U> Tuple2<ImList<T>, ImList<U>> unzip(final ImList<Tuple2<T, U>> list) {

        // Kind of inefficient to go through the list twice ...
        final ImList<T> uz1 = list.map(Tuple2::_1);
        final ImList<U> uz2 = list.map(Tuple2::_2);
        return new Pair<>(uz1, uz2);
    }
}
