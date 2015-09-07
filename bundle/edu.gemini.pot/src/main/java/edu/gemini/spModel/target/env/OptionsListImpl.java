//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Baseline implementation of {@link OptionsList}.
 */
public final class OptionsListImpl<T> implements Serializable, Iterable<T>, Tuple2<Option<T>, ImList<T>>, OptionsList<T> {

    /**
     * Creates an OptionsList with the given list of elements.  Defaults the
     * primary element to the first element if the list is not empty,
     * {@link None} otherwise.
     *
     * @param list options to include; may be an empty list if there are no
     * options
     *
     * @return new OptionsList with the given elements
     */
    public static <T> OptionsListImpl<T> create(T... list) {
        return create(DefaultImList.create(list));
    }

    /**
     * Creates an OptionsList with the given list of elements.  Defaults the
     * primary element to the first element if the list is not empty,
     * {@link None} otherwise.
     *
     * @param list options to include; may be an empty list if there are no
     * options
     *
     * @return new OptionsList with the given elements
     */
    public static <T> OptionsListImpl<T> create(ImList<T> list) {
        Option<Integer> index = list.size() > 0 ? new Some<Integer>(0) : None.INTEGER;
        return create(index, list);
    }

    /**
     * Creates an OptionsList with the given list of elements and indicated
     * primary element.
     *
     * @param primary index of the primary element in the list, which must be
     * in the range <code>0 <= primary < list.length</code>
     *
     * @param list options to include; may not be empty or else the index
     * will be out of range
     *
     * @throws IllegalArgumentException if the list is empty or the given
     * primary index is otherwise out of range
     * <code>0 <= primary < list.length</code>
     *
     * @return new OptionsList with the given elements and primary element
     */
    public static <T> OptionsListImpl<T> createP(int primary, T... list) {
        return create(primary, DefaultImList.create(list));
    }

    /**
     * Creates an OptionsList with the given list of elements and indicated
     * primary element.
     *
     * @param primary index of the primary element in the list, which must be
     * {@link None} or else {@link Some} with a value in the range
     * <code>0 <= primary < list.length</code>
     *
     * @param list options to include; may be empty only if the primary index
     * is {@link None}
     *
     * @throws IllegalArgumentException if the primary index is not {@link None}
     * and out of range <code>0 <= primary < list.length</code>
     *
     * @return new OptionsList with the given elements and primary element
     */
    public static <T> OptionsListImpl<T> create(Option<Integer> primary, T... list) {
        return create(primary, DefaultImList.create(list));
    }

    /**
     * Creates an OptionsList with the given list of elements and indicated
     * primary element.
     *
     * @param primary index of the primary element in the list, which must be
     * in the range <code>0 <= primary < list.size()</code>
     *
     * @param list options to include; may not be empty or else the index
     * will be out of range
     *
     * @throws IllegalArgumentException if the list is empty or the given
     * primary index is otherwise out of range
     * <code>0 <= primary < list.size()</code>
     *
     * @return new OptionsList with the given elements and primary element
     */
    public static <T> OptionsListImpl<T> create(int primary, ImList<T> list) {
        return create(new Some<Integer>(primary), list);
    }

    /**
     * Creates an OptionsList with the given list of elements and indicated
     * primary element.
     *
     * @param primary index of the primary element in the list, which must be
     * {@link None} or else {@link Some} with a value in the range
     * <code>0 <= primary < list.size()</code>
     *
     * @param list options to include; may be empty only if the primary index
     * is {@link None}
     *
     * @throws IllegalArgumentException if the primary index is not {@link None}
     * and out of range <code>0 <= primary < list.size()</code>
     *
     * @return new OptionsList with the given elements and primary element
     */
    public static <T> OptionsListImpl<T> create(Option<Integer> primary, ImList<T> list) {
        return new OptionsListImpl<T>(primary, list);
    }

    private final Option<Integer> primaryIndex;
    private final ImList<T> list;

    protected OptionsListImpl(Option<Integer> primaryIndex, ImList<T> list) {
        if (list == null) throw new IllegalArgumentException("list = null");

        if (!primaryIndex.isEmpty()) {
            int index = primaryIndex.getValue();
            if ((index < 0) || (index >= list.size())) {
                String msg = String.format(
                        "primary index, %d, out of range 0 .. %d (list.size())",
                         primaryIndex.getValue(), list.size());
                throw new IllegalArgumentException(msg);
            }
        }

        this.primaryIndex = primaryIndex;
        this.list         = list;
    }

    @Override
    public Option<T> getPrimary() {
        return primaryIndex.map(new Function1<Integer, T>() {
            @Override public T apply(Integer integer) { return list.get(integer); }
        });
    }

    @Override
    public OptionsList<T> selectPrimary(Option<T> primary) {
        return setPrimaryIndex(primary.map(new Function1<T, Integer>() {
            @Override public Integer apply(T t) { return list.indexOf(t); }
        }));
    }

    @Override
    public OptionsList<T> selectPrimary(T primary) {
        return setPrimaryIndex(new Some<Integer>(list.indexOf(primary)));
    }

    @Override
    public OptionsList<T> setPrimary(T primary) {
        return primaryIndex.isEmpty() ?
                create(list.size(), list.append(primary)) :
                create(primaryIndex, list.updated(primaryIndex.getValue(), primary));
    }

    @Override
    public Option<Integer> getPrimaryIndex() {
        return primaryIndex;
    }

    @Override
    public OptionsList<T> setPrimaryIndex(Option<Integer> primary) {
        if (primary.equals(primaryIndex)) return this;
        return new OptionsListImpl<T>(primary, list);
    }

    @Override
    public OptionsList<T> setPrimaryIndex(int primary) {
        return setPrimaryIndex(new Some<Integer>(primary));
    }

    @Override
    public ImList<T> getOptions() {
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public OptionsList<T> setOptions(final ImList<T> newList) {
        return newList.isEmpty() ? create(newList) :
            create(getPrimary().map(new Function1<T, Integer>() {
                @Override public Integer apply(T t) {
                    int res = newList.indexOf(t);
                    if (res >= 0) return res;

                    int size = newList.size();
                    int primary = primaryIndex.getOrElse(size);
                    return primary < size ? primary : size-1;
                }
            }), newList);
    }

    @Override
    public OptionsList<T> update(Option<Integer> newPrimaryIndex, ImList<T> newList) {
        if (newPrimaryIndex.equals(primaryIndex) && newList.equals(list)) return this;
        return create(newPrimaryIndex, newList);
    }

    @Override
    public OptionsList<T> update(OptionsList.Op<T> op) {
        Tuple2<Option<Integer>, ImList<T>> tup = op.apply(this);
        return update(tup._1(), tup._2());
    }

    @Override
    public Option<T> _1() { return getPrimary(); }

    @Override
    public ImList<T> _2() { return list; }

    @Override
    public Tuple2<ImList<T>, Option<T>> swap() {
        return new Pair<ImList<T>, Option<T>>(_2(), _1());
    }

    @Override
    public String mkString(String prefix, String sep, String suffix) {
        StringBuilder buf = new StringBuilder(prefix);
        buf.append("primary=").append(primaryIndex.getOrElse(-1));
        buf.append(sep).append("list=").append(list.mkString(prefix, sep, suffix));
        buf.append(suffix);
        return buf.toString();
    }

    @Override
    public String toString() {
        return mkString("[", ", ", "]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OptionsListImpl that = (OptionsListImpl) o;

        if (!list.equals(that.list)) return false;
        return primaryIndex.equals(that.primaryIndex);
    }

    @Override
    public int hashCode() {
        int result = primaryIndex.hashCode();
        result = 31 * result + list.hashCode();
        return result;
    }
}
