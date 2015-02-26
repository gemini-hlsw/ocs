//
// $
//

package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.*;

/**
 * Default implementation of the {@link ImList} interface.  This implementation
 * is backed with a <code>java.util.ArrayList</code>, which results in quicker
 * random access but wasteful construction and appending operations than a
 * more traditional immutable list implementation.
 */
public final class DefaultImList<T> implements ImList<T>, Serializable {
    private static final long serialVersionUID = 1l;

    public static <T> ImList<T> create(T... elements) {
        if ((elements == null) || (elements.length == 0)) {
            return ImCollections.emptyList();
        }

        List<T> copy = new ArrayList<T>(elements.length);
        copy.addAll(Arrays.asList(elements));

        return new DefaultImList<T>(copy);
    }

    public static <T> ImList<T> create(Collection<? extends T> list) {
        if (list == null) return ImCollections.emptyList();

        List<T> copy = new ArrayList<T>(list.size());
        copy.addAll(list);

        return new DefaultImList<T>(copy);
    }

    // Quickly implemented with a backing java.util.List, though it would be
    // more efficient to do it right with a head element and a tail list.

    private final List<T> backingList;

    /**
     * Constructs with a backingList that will subsequently be owned by this
     * implementation.  This is private constructor because it would be an
     * error should the <code>backingList</code> be modified.
     */
    DefaultImList(List<T> backingList) {
        this.backingList = Collections.unmodifiableList(backingList);
    }

    @Override
    public ImList<T> cons(T t) {
        // ... list copying would be avoided with a proper implementation ...
        List<T> tmp = new ArrayList<T>(backingList.size() + 1);
        tmp.add(t);
        tmp.addAll(backingList);
        return new DefaultImList<T>(tmp);
    }

    @Override
    public ImList<T> append(ImList<? extends T> tail) {
        // ... list copying would be avoided with a proper implementation ...
        List<T> tmp = new ArrayList<T>(backingList.size() + tail.size());
        tmp.addAll(backingList);
        tmp.addAll(tail.toList());
        return new DefaultImList<T>(tmp);
    }

    @Override
    public ImList<T> append(T t) {
        List<T> tmp = new ArrayList<T>(backingList.size() + 1);
        tmp.addAll(backingList);
        tmp.add(t);
        return new DefaultImList<T>(tmp);
    }

    @Override
    public ImList<T> remove(T t) {
        int index = backingList.indexOf(t);
        if (index < 0) return this;

        List<T> tmp = new ArrayList<T>(backingList.size()-1);
        tmp.addAll(backingList.subList(0, index));
        tmp.addAll(backingList.subList(index+1, backingList.size()));
        return new DefaultImList<T>(tmp);
    }

    @Override
    public ImList<T> remove(Function1<? super T, Boolean> op) {
        ArrayList<T> res = new ArrayList<T>(backingList.size());
        for (T t : backingList) {
            if (!op.apply(t)) res.add(t);
        }
        res.trimToSize();
        return new DefaultImList<T>(res);
    }

    @Override
    public ImList<T> updated(int index, T t) {
        ArrayList<T> res = new ArrayList<T>(backingList.size());
        res.addAll(backingList);
        res.set(index, t);
        return new DefaultImList<T>(res);
    }

    @Override
    public T head() {
        if (backingList.size() == 0) return null;
        return backingList.get(0);
    }

    @Override
    public Option<T> headOption() {
        if (backingList.size() == 0) return None.INSTANCE;
        return new Some<T>(backingList.get(0));
    }

    @Override
    public T last() {
        if (backingList.size() == 0) return null;
        return backingList.get(backingList.size()-1);
    }

    @Override
    public ImList<T> tail() {
        if (backingList.size() <= 1) return ImCollections.emptyList();
        return new DefaultImList<T>(backingList.subList(1, backingList.size()));
    }

    @Override
    public ImList<T> initial() {
        if (backingList.size() <= 1) return ImCollections.emptyList();
        return new DefaultImList<T>(backingList.subList(0, backingList.size()-1));
    }

    @Override
    public boolean contains(T t) {
        return backingList.contains(t);
    }

    @Override
    public boolean containsAll(ImList<?> c) {
        return backingList.containsAll(c.toList());
    }

    @Override
    public T get(int index) {
        return backingList.get(index);
    }

    @Override
    public int indexOf(T t) {
        return backingList.indexOf(t);
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
    public Iterator<T> iterator() {
        return backingList.iterator();
    }

    @Override
    public <U> ImList<U> map(Function1<? super T, U> op) {
        List<U> res = new ArrayList<U>();
        for (T t : this) res.add(op.apply(t));
        return new DefaultImList<U>(res);
    }

    @Override
    public <U> ImList<U> flatMap(Function1<? super T, ImList<U>> op) {
        List<U> res = new ArrayList<U>();
        for (T t : this) res.addAll(op.apply(t).toList());
        return new DefaultImList<U>(res);
    }

    @Override
    public void foreach(ApplyOp<? super T> op) {
        for (T t : this) op.apply(t);
    }

    @Override
    public ImList<T> filter(Function1<? super T, Boolean> op) {
        List<T> res = new ArrayList<T>();
        for (T t : this) if (op.apply(t)) res.add(t);
        return new DefaultImList<T>(res);
    }

    @Override
    public Option<T> find(Function1<? super T, Boolean> op) {
        for (T t : this) if (op.apply(t)) return new Some<T>(t);
        return None.instance();
    }

    @Override
    public Tuple2<ImList<T>, ImList<T>> partition(Function1<? super T, Boolean> op) {
        List<T> lst1 = new ArrayList<T>();
        List<T> lst2 = new ArrayList<T>();

        for (T t : this) {
            if (op.apply(t)) {
                lst1.add(t);
            } else {
                lst2.add(t);
            }
        }

        return new Pair<ImList<T>, ImList<T>>(new DefaultImList<T>(lst1), new DefaultImList<T>(lst2));
    }

    @Override
    public boolean forall(Function1<? super T, Boolean> op) {
        for (T t : this) if (!op.apply(t)) return false;
        return true;
    }

    @Override
    public boolean exists(Function1<? super T, Boolean> op) {
        for (T t : this) if (op.apply(t)) return true;
        return false;
    }

    @Override
    public String mkString(String prefix, String separator, String suffix) {
        StringBuilder buf = new StringBuilder(prefix);
        Iterator<T> it = iterator();
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
    public <U> ImList<Tuple2<T, U>> zip(ImList<U> list) {
        List<Tuple2<T, U>> res = new ArrayList<Tuple2<T, U>>();

        int limit = Math.min(backingList.size(), list.size());
        for (int i=0; i<limit; ++i) {
            res.add(new Pair<T, U>(
                    backingList.get(i), list.get(i)));
        }

        return new DefaultImList<Tuple2<T, U>>(res);
    }

    @Override
    public ImList<Tuple2<T, Integer>> zipWithIndex() {
        List<Tuple2<T, Integer>> res = new ArrayList<Tuple2<T, Integer>>(backingList.size());

        int index = 0;
        for (T t : backingList) {
            res.add(new Pair<T, Integer>(t, index++));
        }
        return new DefaultImList<Tuple2<T, Integer>>(res);
    }

    @Override
    public ImList<T> sort(Comparator<? super T> c) {
        if (backingList.size() < 2) return this;

        List<T> sortedList = new ArrayList<T>(backingList.size());
        sortedList.addAll(backingList);
        Collections.sort(sortedList, c);
        return new DefaultImList<T>(sortedList);
    }

    @Override
    public T min(Comparator<? super T> c) {
        if (backingList.size() == 1) return backingList.get(0);

        T minElement = backingList.get(0);
        for (T cur : backingList.subList(1, backingList.size())) {
            if (c.compare(minElement, cur) > 0) minElement = cur;
        }
        return minElement;
    }

    @Override
    public T max(Comparator<? super T> c) {
        if (backingList.size() == 1) return backingList.get(0);

        T maxElement = backingList.get(0);
        for (T cur : backingList.subList(1, backingList.size())) {
            if (c.compare(maxElement, cur) < 0) maxElement = cur;
        }
        return maxElement;
    }

    @Override
    public ImList<T> reverse() {
        if (backingList.size() < 2) return this;

        List<T> revList = new ArrayList<T>(backingList.size());
        revList.addAll(backingList);
        Collections.reverse(revList);
        return new DefaultImList<T>(revList);
    }

    @Override
    public <U> U foldLeft(U start, Function2<U, ? super T, U> op) {
        U cur = start;
        for (T t : backingList) cur = op.apply(cur, t);
        return cur;
    }

    @Override
    public <U> U foldRight(U start, Function2<? super T, U, U> op) {
        U cur = start;
        for (int i=backingList.size()-1; i >= 0; --i) {
            T elem = backingList.get(i);
            cur = op.apply(elem, cur);
        }
        return cur;
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

        ImList that = (ImList) o;
        if (this.size() != that.size()) return false;

        Iterator thatIt = that.iterator();
        for (T t : this) {
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
        for (T t : this) {
            res = 31*res + ((t == null) ? 0 : t.hashCode());
        }
        return res;
    }
}
