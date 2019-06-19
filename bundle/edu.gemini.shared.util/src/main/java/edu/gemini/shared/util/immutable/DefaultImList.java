package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the {@link ImList} interface.  This implementation
 * is backed with a <code>java.util.ArrayList</code>, which results in quicker
 * random access but wasteful construction and appending operations than a
 * more traditional immutable list implementation.
 */
public final class DefaultImList<T> implements ImList<T>, Serializable {
    private static final long serialVersionUID = 1L;

    public static <T> ImList<T> create(final T... elements) {
        if ((elements == null) || (elements.length == 0)) {
            return ImCollections.emptyList();
        }

        final List<T> copy = new ArrayList<>(elements.length);
        copy.addAll(Arrays.asList(elements));

        return new DefaultImList<>(copy);
    }

    public static <T> ImList<T> create(final Collection<? extends T> list) {
        if (list == null) return ImCollections.emptyList();

        final List<T> copy = new ArrayList<>(list.size());
        copy.addAll(list);

        return new DefaultImList<>(copy);
    }

    // Quickly implemented with a backing java.util.List, though it would be
    // more efficient to do it right with a head element and a tail list.

    private final List<T> backingList;

    /**
     * Constructs with a backingList that will subsequently be owned by this
     * implementation.  This is private constructor because it would be an
     * error should the <code>backingList</code> be modified.
     */
    DefaultImList(final List<T> backingList) {
        this.backingList = Collections.unmodifiableList(backingList);
    }

    @Override
    public ImList<T> cons(final T t) {
        // ... list copying would be avoided with a proper implementation ...
        final List<T> tmp = new ArrayList<>(backingList.size() + 1);
        tmp.add(t);
        tmp.addAll(backingList);
        return new DefaultImList<>(tmp);
    }

    @Override
    public ImList<T> append(final ImList<? extends T> tail) {
        // ... list copying would be avoided with a proper implementation ...
        final List<T> tmp = new ArrayList<>(backingList.size() + tail.size());
        tmp.addAll(backingList);
        tmp.addAll(tail.toList());
        return new DefaultImList<>(tmp);
    }

    @Override
    public ImList<T> append(final T t) {
        final List<T> tmp = new ArrayList<>(backingList.size() + 1);
        tmp.addAll(backingList);
        tmp.add(t);
        return new DefaultImList<>(tmp);
    }

    @Override
    public ImList<T> remove(final T t) {
        int index = backingList.indexOf(t);
        if (index < 0) return this;

        final List<T> tmp = new ArrayList<>(backingList.size()-1);
        tmp.addAll(backingList.subList(0, index));
        tmp.addAll(backingList.subList(index+1, backingList.size()));
        return new DefaultImList<>(tmp);
    }

    @Override
    public ImList<T> remove(final Function1<? super T, Boolean> op) {
        final ArrayList<T> res = new ArrayList<>(backingList.size());
        backingList.forEach(t -> {
            if (!op.apply(t)) res.add(t);
        });
        res.trimToSize();
        return new DefaultImList<>(res);
    }

    @Override
    public ImList<T> updated(final int index, final T t) {
        final ArrayList<T> res = new ArrayList<>(backingList.size());
        res.addAll(backingList);
        res.set(index, t);
        return new DefaultImList<>(res);
    }

    @Override
    public T head() {
        if (backingList.size() == 0) return null;
        return backingList.get(0);
    }

    @Override
    public Option<T> headOption() {
        if (backingList.size() == 0) return None.instance();
        return new Some<>(backingList.get(0));
    }

    @Override
    public T last() {
        if (backingList.size() == 0) return null;
        return backingList.get(backingList.size()-1);
    }

    @Override
    public ImList<T> tail() {
        if (backingList.size() <= 1) return ImCollections.emptyList();
        return new DefaultImList<>(backingList.subList(1, backingList.size()));
    }

    @Override
    public ImList<T> initial() {
        if (backingList.size() <= 1) return ImCollections.emptyList();
        return new DefaultImList<>(backingList.subList(0, backingList.size()-1));
    }

    @Override
    public boolean contains(final T t) {
        return backingList.contains(t);
    }

    @Override
    public boolean containsAll(final ImList<?> c) {
        return backingList.containsAll(c.toList());
    }

    @Override
    public T get(final int index) {
        return backingList.get(index);
    }

    @Override
    public Option<T> getOption(final int index) {
        return (index < 0 || index >= backingList.size()) ? None.instance() : new Some<>(backingList.get(index));
    }

    @Override
    public int indexOf(final T t) {
        return backingList.indexOf(t);
    }

    @Override
    public int indexWhere(Function1<? super T, Boolean> p) {
        for (int i=0; i<backingList.size(); ++i) {
            if (p.apply(backingList.get(i))) return i;
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return backingList.isEmpty();
    }

    @Override
    public boolean nonEmpty() {
        return !isEmpty();
    }

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    public List<T> toList() {
        return backingList;
    }

    @Override
    public T[] toArray() {
        return (T[]) backingList.toArray();
    }

    @Override
    public Iterator<T> iterator() {
        return backingList.iterator();
    }

    @Override
    public <U> ImList<U> map(final Function1<? super T, U> op) {
        final List<U> res = new ArrayList<>();
        backingList.forEach(t -> res.add(op.apply(t)));
        return new DefaultImList<>(res);
    }

    @Override
    public <U> ImList<U> flatMap(final Function1<? super T, ImList<U>> op) {
        final List<U> res = new ArrayList<>();
        backingList.forEach(t -> res.addAll(op.apply(t).toList()));
        return new DefaultImList<>(res);
    }

    @Override
    public void foreach(final ApplyOp<? super T> op) {
        backingList.forEach(op::apply);
    }

    @Override
    public ImList<T> filter(final Function1<? super T, Boolean> op) {
        final List<T> res = new ArrayList<>();
        backingList.forEach(t -> { if (op.apply(t)) res.add(t); });
        return new DefaultImList<>(res);
    }

    @Override
    public Option<T> find(Function1<? super T, Boolean> op) {
        for (final T t : this) if (op.apply(t)) return new Some<>(t);
        return None.instance();
    }

    @Override
    public Tuple2<ImList<T>, ImList<T>> partition(final Function1<? super T, Boolean> op) {
        final List<T> lst1 = new ArrayList<>();
        final List<T> lst2 = new ArrayList<>();

        backingList.forEach(t -> {
            if (op.apply(t)) {
                lst1.add(t);
            } else {
                lst2.add(t);
            }
        });

        return new Pair<>(new DefaultImList<>(lst1), new DefaultImList<>(lst2));
    }

    @Override
    public boolean forall(final Function1<? super T, Boolean> op) {
        for (final T t : this) if (!op.apply(t)) return false;
        return true;
    }

    @Override
    public boolean exists(final Function1<? super T, Boolean> op) {
        for (final T t : this) if (op.apply(t)) return true;
        return false;
    }

    @Override
    public String mkString(final String prefix, final String separator, final String suffix) {
        final StringBuilder buf = new StringBuilder(prefix);
        final Iterator<T> it = iterator();
        if (it.hasNext()) {
            buf.append(it.next());
            while (it.hasNext()) {
                buf.append(separator).append(it.next());
            }
        }
        buf.append(suffix);
        return buf.toString();
    }

    @Override
    public <U> ImList<Tuple2<T, U>> zip(final ImList<U> list) {
        final List<Tuple2<T, U>> res = new ArrayList<>();

        final int limit = Math.min(backingList.size(), list.size());
        for (int i=0; i<limit; ++i) {
            res.add(new Pair<>(backingList.get(i), list.get(i)));
        }

        return new DefaultImList<>(res);
    }

    @Override
    public ImList<Tuple2<T, Integer>> zipWithIndex() {
        final List<Tuple2<T, Integer>> res = new ArrayList<>(backingList.size());

        int index = 0;
        for (final T t : backingList) {
            res.add(new Pair<>(t, index++));
        }
        return new DefaultImList<>(res);
    }

    @Override
    public ImList<Tuple2<T, Option<T>>> zipWithNext() {
        return zip(tail().map(t -> ImOption.apply(t)).append(ImOption.<T>empty()));
    }

    @Override
    public ImList<T> sort(final Comparator<? super T> c) {
        if (backingList.size() < 2) return this;

        final List<T> sortedList = new ArrayList<>(backingList.size());
        sortedList.addAll(backingList);
        Collections.sort(sortedList, c);
        return new DefaultImList<>(sortedList);
    }

    @Override
    public T min(final Comparator<? super T> c) {
        if (backingList.size() == 1) return backingList.get(0);

        T minElement = backingList.get(0);
        for (final T cur : backingList.subList(1, backingList.size())) {
            if (c.compare(minElement, cur) > 0) minElement = cur;
        }
        return minElement;
    }

    @Override
    public T max(final Comparator<? super T> c) {
        if (backingList.size() == 1) return backingList.get(0);

        T maxElement = backingList.get(0);
        for (final T cur : backingList.subList(1, backingList.size())) {
            if (c.compare(maxElement, cur) < 0) maxElement = cur;
        }
        return maxElement;
    }

    @Override
    public ImList<T> reverse() {
        if (backingList.size() < 2) return this;

        final List<T> revList = new ArrayList<>(backingList.size());
        revList.addAll(backingList);
        Collections.reverse(revList);
        return new DefaultImList<>(revList);
    }

    @Override
    public <U> U foldLeft(final U start, final Function2<U, ? super T, U> op) {
        U cur = start;
        for (final T t : backingList) cur = op.apply(cur, t);
        return cur;
    }

    @Override
    public <U> U foldRight(final U start, final Function2<? super T, U, U> op) {
        U cur = start;
        for (int i=backingList.size()-1; i >= 0; --i) {
            final T elem = backingList.get(i);
            cur = op.apply(elem, cur);
        }
        return cur;
    }

    @Override
    public <K> HashMap<K, ImList<T>> groupBy(Function1<? super T, K> f) {
        HashMap<K, ImList<T>> m = new HashMap<>();
        for (final T elem : backingList) {
            final K key = f.apply(elem);
            m.put(key, ImOption.apply(m.get(key)).map(
                    lst -> lst.append(elem)
            ).getOrElse(() -> create(elem)));
        }
        return m;
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public ImList<T> take(int n) {
        final int to = Math.min(n, backingList.size());
        return (to < 0) ? ImCollections.<T>emptyList() : new DefaultImList<>(backingList.subList(0, to));
    }

    /**
     * Calls {@link #mkString(String, String, String)} with the arguments
     * "{", ", ", "}".
     * @return String representation of this list
     */
    @Override
    public String toString() {
        return mkString("{", ", ", "}");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ImList)) return false;

        final ImList<T> that = (ImList<T>) o;
        if (this.size() != that.size()) return false;

        final Iterator<T> thatIt = that.iterator();
        for (final T t : this) {
            if (t == null) {
                if (thatIt.next() != null) return false;
            } else {
                if (!t.equals(thatIt.next())) return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int res = 1;
        for (final T t : this) {
            res = 31*res + ((t == null) ? 0 : t.hashCode());
        }
        return res;
    }

}
