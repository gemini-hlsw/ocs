package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;

/**
 * A type representing an immutable list of objects and a designated primary
 * element, which must be a member of the list.  The list contains all the
 * options, while the optional primary element contains the selected option.
 */
public interface OptionsList<T> extends Iterable<T> {

    /**
     * An function representing an arbitrary update operation on the options
     * list.  Used with the {@link OptionsList#update} methods.
     */
    interface Op<T> extends Function1<OptionsList<T>, Tuple2<Option<Integer>, ImList<T>>> {

        /**
         * Given an {@link OptionsList} object, the function must compute a new
         * primary index and list of options and return them to the caller.
         * The caller is expected to be the {@link OptionsList#update(Op)}
         * method, which will take the results and apply them to create a new
         * instance with these changes in effect.
         *
         * @param list options list that will be used to compute a new primary
         * index and list of items
         *
         * @return a {@link Tuple2 tuple} containing the results of the
         * calculation
         */
        Tuple2<Option<Integer>, ImList<T>> apply(OptionsList<T> list);
    }

    /**
     * A collection of {@link Op update functions} that can be applied to the
     * {@link OptionsList#update(Op)} method.  These are commonly needed
     * operations for OptionsLists.
     */
    final class UpdateOps {
        // don't construct any instances
        private UpdateOps() { /* empty */ }

        private static <T> Tuple2<Option<Integer>, ImList<T>> create(final Option<Integer> primary, final ImList<T> list) {
            return new Pair<>(primary, list);
        }

        /**
         * Appends an item to the list of options, leaving the primary index
         * unchanged.
         *
         * @param t item to append
         *
         * @return {@link Tuple2 tuple} describing the result of appending
         * the given item to the options list
         */
        public static <T> Op<T> append(final T t) {
            return olist -> create(olist.getPrimaryIndex(), olist.getOptions().append(t));
        }

        /**
         * Appends an item to the list of options and marks it as the primary
         * element.
         *
         * @param t item to append and become primary
         *
         * @return {@link Tuple2 tuple} describing the result of appending
         * the given item to the options list and making it the primary element
         */
        public static <T> Op<T> appendAsPrimary(final T t) {
            return olist -> {
                final Option<Integer> newPrimary = new Some<>(olist.getOptions().size());
                final ImList<T> newList = olist.getOptions().append(t);
                return create(newPrimary, newList);
            };
        }

        /**
         * Removes the indicated item from the list and handles the primary
         * element update. In particular, the primary element is maintained
         * unless it is the item being removed.  In that case, if the resulting
         * list is empty, there is no primary element.  Otherwise, the primary
         * element is the next element in the list after the item being removed
         * if any -- otherwise the previous element in the list.
         *
         * @param t item to remove from the list
         *
         * @return {@link Tuple2 tuple} describing the result of appending the
         * given item to the options list and making it the primary element
         */
        public static <T> Op<T> remove(final T t) {
            return olist -> {
                final Option<Integer> primary = olist.getPrimaryIndex();
                final ImList<T> list = olist.getOptions();

                final int index = olist.getOptions().indexOf(t);
                if (index == -1) return create(primary, list);

                final ImList<T> newList = olist.getOptions().remove(t);
                final ImList<T> empty = ImCollections.emptyList();
                if (newList.isEmpty()) return create(None.INTEGER, empty);

                final Option<Integer> newPrimary = primary.map(pindex -> {
                    if (pindex < index) return pindex;
                    if (pindex > index) return pindex - 1;

                    final int size = newList.size();
                    return pindex == size ? size - 1 : pindex;
                });

                return create(newPrimary, newList);
            };
        }

        /**
         * Creates and returns a copy of the OptionsList object that is
         * identical except that the given element's primary state is toggled.
         * In other words, if the given element is the primary element in this
         * list, the updated object does not have a primary element.  If
         * the given element is not primary, then the returned list has this
         * element as its primary.
         *
         * @param primary element whose primary state should be toggled; must
         * exist in this list or exception will be thrown
         *
         * @return a new OptionsList; if the specified primary is already the
         * primary member of the list then the return value has no primary
         * element; if not already the primary, then the return value has this
         * element as its primary position
         *
         * @throws IllegalArgumentException if <code>primary</code> is not a
         * member of the collection (i.e., if
         * <code>!{@link OptionsList#getOptions()}.contains(primary)</code>)
         */
        public static <T> Op<T> togglePrimary(final T primary) {
            return olist -> {
                final int index = olist.getOptions().indexOf(primary);
                if (index < 0) {
                    throw new IllegalArgumentException("not a member of the list");
                }

                final Option<Integer> opt = (olist.getPrimaryIndex().getOrElse(-1) == index) ? None.INTEGER : new Some<>(index);
                return create(opt, olist.getOptions());
            };
        }
    }


    /**
     * Gets the primary element in the list of options, if any.
     *
     * @return the primary element wrapped in a {@link Some}, or {@link None}
     * if there isn't one
     */
     Option<T> getPrimary();

    /**
     * Creates and returns a copy of the list that is identical except with
     * the given primary element (which if defined must already be in this
     * collection).
     *
     * @param primary new element to use as the primary element (or
     * {@link None} to indicate that there is no primary element)
     *
     * @return a new list with the specified primary element
     *
     * @throws IllegalArgumentException if <code>primary</code> is not
     * {@link None} and not a member of the collection (i.e., if
     * <code>!{@link #getOptions()}.contains(primary.getValue())</code>)
     */
    OptionsList<T> selectPrimary(Option<T> primary);

    /**
     * Creates and returns a copy of the list that is identical except with
     * the given primary element (which must already be in this collection).
     *
     * @param primary new element to use as the primary element
     *
     * @return a new list with the specified primary element
     *
     * @throws IllegalArgumentException if <code>primary</code> is not a member
     * of the collection (i.e., if
     * <code>!{@link #getOptions()}.contains(primary)</code>)
     */
    OptionsList<T> selectPrimary(T primary);

    /**
     * Creates and returns a copy of the list that is identical except with the
     * given primary element, which either replaces the existing primary
     * element or is appended to this list and becomes the primary element.
     *
     * @param primary new primary element
     *
     * @return a new list with the specified primary element in place
     * of the existing primary element (if any)
     */
    OptionsList<T> setPrimary(T primary);

    /**
     * Gets the index of the primary item in this list, if any.  Assuming there
     * is a primary item, this is equivalent to:
     *
     * <pre>
     *     new {@link Some}<T>({@link #getOptions()}.indexOf(getPrimary().getValue()))
     * </pre>
     *
     * @return the primary index or <code>{@link None}</code> if there isn't
     * one
     */
    Option<Integer> getPrimaryIndex();

    /**
     * Creates and returns a copy of this OptionsList object that is
     * identical except with the given primary element.
     *
     * @param primary index of the primary element in this list, if any
     *
     * @return a new OptionsList with the primary element at the specified
     * position in the list
     *
     * @throws IllegalArgumentException if primary is not {@link None} and
     * not in the range
     * <code>0 <= primary < {@link #getOptions()}.size()</code>
     */
    OptionsList<T> setPrimaryIndex(Option<Integer> primary);

    /**
     * Creates and returns a copy of this OptionsList object that is
     * identical except with the given primary element.
     *
     * @param primary index of the primary element in this list
     *
     * @return a new OptionsList with the primary element at the specified
     * position in the list
     *
     * @throws IllegalArgumentException if primary is not in the range
     * <code>0 <= primary < {@link #getOptions()}.size()</code>
     */
    OptionsList<T> setPrimaryIndex(int primary);

    /**
     * Gets the immutable list of options.  May return an empty list but will
     * not return <code>null</code>.
     */
    ImList<T> getOptions();

    /**
     * Returns a copy of this OptionsList object, but using the provided
     * list of elements. The primary element (if any) is maintained if it still
     * appears in the <code>newList</code>.  Otherwise, the position of the
     * primary element in the list is maintained, unless the
     * <code>newList</code> is shorter than this list in which case it is set
     * to the index of the last element in <code>newList</code>.
     */
    OptionsList<T> setOptions(ImList<T> newList);

    /**
     * Creates a new copy of this OptionsList object, but using the given
     * primary index and list of elements.
     *
     * @param primaryIndex new primary index to use
     * @param list new list of options
     *
     * @return a copy of this list using the provided primary index and list
     * of values
     *
     * @throws IllegalArgumentException if the index is not {@link None} and
     * not in the range <code>0 <= primary < {@link #getOptions()}.size()</code>
     */
    OptionsList<T> update(Option<Integer> primaryIndex, ImList<T> list);

    /**
     * Executes the provided operation and applies the results to the
     * {@link #update(edu.gemini.shared.util.immutable.Option, edu.gemini.shared.util.immutable.ImList)}
     * method.
     *
     * @param op update operation to apply
     *
     * @return updated options list, according to the results of the operation
     */
    OptionsList<T> update(Op<T> op);

    /**
     * Creates a string representation of this options list using the provided
     * prefix, separator, and suffix Strings.
     */
    String mkString(String prefix, String sep, String suffix);
}
