package edu.gemini.shared.util.immutable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * An immutable list interface.
 */
public interface ImList<T> extends Iterable<T> {

    /** Creates a Stream from the list.
      */
    Stream<T> stream();

    /**
     * Creates a new ImList that has the same contents as this one, except it
     * is prepended with the given element.
     *
     * @param t new object that is added to the head of the list returned by
     * this method
     *
     * @return a new ImList, identical to this one, but with the given item
     * prepended
     */
    ImList<T> cons(T t);

    /**
     * Creates a new ImList that has the same contents as this one except that
     * the contents of <code>tail</code> are appended.
     *
     * @param tail items to append to the end of the ImList returned by this
     * method
     *
     * @return a new ImList, identical to this one, with the given items
     * appended
     */
    ImList<T> append(ImList<? extends T> tail);

    /**
     * Creates a new ImList that has the same contents as this one except that
     * the given element is appended.
     *
     * @param t item to append to the tail of the ImList returned by this
     * method
     *
     * @return a new ImList, identical to this one, but with the given item
     * appended
     */
    ImList<T> append(T t);

    /**
     * Creates a new ImList without the given element.
     *
     * @param t element to remove from the ImList returned by this method
     *
     * @return a new ImList, identical to this one, but without the given
     * element
     */
    ImList<T> remove(T t);

    /**
     * Creates a new ImList that contains all the elements of this ImList
     * except for those that satisfy the given predicate.
     *
     * @param op predicate op to apply to the list, a <code>true</code> return
     * results in removing the item from the returned list
     *
     * @return a new ImList, identical to this one, but without any elements
     * for which the given predicate returns <code>true</code>
     */
    ImList<T> remove(Function1<? super T, Boolean> op);

    /**
     * Creates a copy of this list with one single element replaced.
     *
     * @param index position in the list to be updated, which must be in the
     * range 0 <= index < size()
     * @param t element to include in the new list that is returned in place
     * of the element that is current at the indicated position
     *
     * @return an updated list that has the provided element <code>t</code>
     * in place of the element at the position indicated by <code>index</code>
     */
    ImList<T> updated(int index, T t);

    /**
     * Gets the first item in this list.
     *
     * @return first item in the list
     */
    T head();

    /**
     * Gets the first item in this list if it is not empty.  Otherwise returns
     * {@link None}.
     */
    Option<T> headOption();

    /**
     * Gets all the elements in this list except for the first one.
     *
     * @return all the elements in this list except the first one
     */
    ImList<T> tail();

    /**
     * Gets the last item in this list.
     *
     * @return last item in the list
     */
    T last();

    /**
     * Gets all the elements in this list except for the last one.
     *
     * @return all the elements in this list except for the last one
     */
    ImList<T> initial();

    /**
     * Determines whether the given element is in this list.
     *
     * @param t the element whose presence is sought
     *
     * @return <code>true</code> if the given element is in the list
     */
    boolean contains(T t);

    /**
     * Determines whether all the given elements are present in this list.
     *
     * @param c collection of elements whose presence in this list is sought
     *
     * @return <code>true</code> if all the given elements are contained in
     * this list
     */
    boolean containsAll(ImList<?> c);

    /**
     * Gets the item at the given position in the list.
     *
     * @param index position of the element to return
     *
     * @return the element at the specified position in this list
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (<code>index < 0 || index >= size()</code>).
     */
    T get(int index);

    /**
     * If the index is a valid position in the list which contains item, returns Some(item).
     * Otherwise, returns None.
     *
     * @param index position of the element to return.
     *
     * @return Some(item) if index is a valid position in the list containing item,
     * and None otherwise.
     */
    Option<T> getOption(int index);

    /**
     * Returns the index in this list of the first occurrence of the specified
     * element, or -1 if this list does not contain this element.
     *
     * @param o element to search for
     *
     * @return index in this list of the first occurrence of the specified
     * element, or -1 if this list does not contain this element
     */
    int indexOf(T o);

    /**
     * Returns the index of the first occurrence which satisfies the predicate.
     *
     * @param p the predicate to test the elements against
     *
     * @return index of the first matching element, if any; -1 otherwise
     */
    int indexWhere(Function1<? super T, Boolean> p);

    /**
     * Returns <code>true</code> if this list contains no elements.
     *
     * @return <code>true</code> if this list contains no elements
     */
    boolean isEmpty();

    /**
     * Returns <code>true</code> if this list contains elements.
     *
     * @return <code>true</code> if this list contains elements
     */
    boolean nonEmpty();

    /**
     * Returns the number of elements in this list.
     * @return the number of elements in this list
     */
    int size();

    /**
     * Returns a <code>java.util.List</code> view of this ImList instance.
     * Note that the returned list is unmodifiable, so clients that wish to
     * manipulate it must make a copy.
     *
     * @return List view of the immutable list
     */
    List<T> toList();

    /**
     * Applies the given map operation to the list, creating and returning
     * the mapped ImList.  Each element of the immutable list is processed by
     * the provided <code>op</code>, and the resulting objects are collected
     * and returned in an immutable list.
     *
     * <p>For example, a {@link Function1}<String,Integer> that maps a string
     * to its corresponding integer could map a list
     * <code>{"1", "2", "3"}</code> to <code>{1, 2, 3}</code>.
     *
     * @param op the operation to apply to each element of the list
     *
     * @param <U> the type of the result of the map operation (and therefore
     * the type of the elements contained in the result immutable list)
     *
     * @return immutable list of mapped elements
     */
    <U> ImList<U> map(Function1<? super T, U> op);

    /**
     * Applies the given map operation to the list, creating and returning
     * the mapped ImList.  Here the {@link Function1} produces a list of type
     * <code>U</code> for each element and the contents of these lists are
     * added to a single result list of type <code>U</code>.
     *
     * <p>For example, consider a <code>{@link Function1}<String, ImList<char>></code>
     * that maps a word to a list of characters:
     * <pre>
     * "word" -> {'w', 'o', 'r', 'd'}
     * </pre>
     *
     * In this case, using {@link #map} we would have, for example:
     *
     * <pre>
     * {"Scala", "Java"} -> {{'S', 'c', 'a', 'l', 'a'}, {'J', 'a', 'v', 'a'}}
     * </pre>
     *
     * whereas with flatMap, we would get
     *
     * <pre>
     * {"Scala", "Java"} -> {'S', 'c', 'a', 'l', 'a', 'J', 'a', 'v', 'a'}
     * </pre>
     *
     * @param op the operation to apply to each element of the list
     *
     * @param <U> the type of element contained in the list of items produced
     * by the map operation
     *
     * @return immutable list of mapped elements where the content of each
     * resulting list is added rather than simply returning a list of lists
     */
    <U> ImList<U> flatMap(Function1<? super T, ImList<U>> op);

    /**
     * Applies the given {@link ApplyOp} to each element of the list. This
     * method can be used, for example, to replace code that explicitly loops
     * through each element of a list and performs some operation using each
     * element.
     */
    void foreach(ApplyOp<? super T> op);

    /**
     * Finds the first element that satisfies the given {@link PredicateOp},
     * if any.
     *
     * @param op predicate to apply to each element of the list
     *
     * @return first element for which the predicate returns <code>true</code>
     * wrapped in a {@link Some} instance, or {@link None} if none
     */
    Option<T> find(Function1<? super T, Boolean> op);

    /**
     * Creates a new ImList with an element for each element of this list that
     * satisfies the given {@link PredicateOp}.
     *
     * @param op predicate to apply to each element of the list
     *
     * @return a new ImList with all the elements for which the given
     * <code>op</code> returns <code>true</code>
     */
    ImList<T> filter(Function1<? super T, Boolean> op);

    /**
     * Partitions the immutable list into two lists, one containing elements
     * that satisfy the given predicate and the other with the remaining
     * elements.
     *
     * @param op predicate to apply to each element of the list
     *
     * @return a tuple of two lists, those that satisfy the predicate
     * {@link Tuple2#_1} and those that do not {@link Tuple2#_2}
     */
    Tuple2<ImList<T>, ImList<T>> partition(Function1<? super T, Boolean> op);

    /**
     * Returns <code>true</code> if all elements of this list satisfy the given
     * predicate.
     *
     * @param op predicate to apply to each element of the list
     *
     * @return <code>true</code> if all elements of the list satisfy the
     * predicate; <code>false</code> otherwise
     */
    boolean forall(Function1<? super T, Boolean> op);

    /**
     * Returns <code>true</code> if any element of this list satisfies the
     * given predicate.
     *
     * @param op predicate to apply to each element of the list
     *
     * @return <code>true</code> if any element of the list satisfies the
     * predicate; <code>false</code> otherwise
     */
    boolean exists(Function1<? super T, Boolean> op);

    /**
     * Creates a string representation of the list which is equivalent to
     * <pre>
     *    pre + get(0) + sep + ... + sep + get(size()-1) + post
     * </pre>
     *
     * @param prefix string to prepend to the result
     * @param separator separator to place between each successive element
     * @param suffix string to append to the result
     *
     * @return string representation of the list
     */
    String mkString(String prefix, String separator, String suffix);

    /**
     * Creates a new immutable list containing {@link Tuple2}s where each element
     * is an association between the element in this list and in the provided
     * list at the same position.  If the two lists are of different lengths,
     * the returned value will contain only the number of elements in the
     * smaller of the two lists.
     *
     * For example<pre>
     *     ('a', 'b', 'c', 'd') zip (1, 2, 3) => (('a', 1), ('b', 2), ('c', 3))
     * </pre>
     *
     * @return a list of tuples combining this lists elements with the provided
     * lists elements
     */
    <U> ImList<Tuple2<T, U>> zip(ImList<U> list);

    /**
     * Creates a new immutable list containing {@link Tuple2}s where each element
     * is an association between the element in this list and its index.  This
     * is a special case of the {@link #zip} method, where the list being
     * passed in is understood to be the index of each element in this list.
     *
     * For example<pre>
     *     ('a', 'b', 'c', 'd') zipWithIndex => (('a', 1), ('b', 2), ('c', 3), ('d', 4))
     * </pre>
     *
     * @return a list of tuples combining this lists elements with its index
     */
    ImList<Tuple2<T, Integer>> zipWithIndex();

    /**
     * Creates a new immutable list with the same elements as this list, but
     * with its members sorted according to the ordering specified in the
     * given Comparator.
     *
     * @param c comparator to use in determining the order of the elements
     *
     * @return a sorted version of this list according to the ordering
     * specified in the comparator
     */
    ImList<T> sort(Comparator<? super T> c);

    /**
     * Retrieves the minimum element of the list according to the given
     * Comparator.
     * @throws java.util.NoSuchElementException if the list is empty
     */
    T min(Comparator<? super T> c);

    /**
     * Retrieves the maximum element of the list according to the given
     * Comparator.
     * @throws java.util.NoSuchElementException if the list is empty
     */
    T max(Comparator<? super T> c);

    /**
     * Creates and returns a new immutable list with the same elements as this
     * list but in reverse order.
     *
     * @return a new list with the same elements as this list in reverse order
     */
    ImList<T> reverse();

    /**
     * Combines the elements of this list using the function op, working from
     * left to right and starting with the value <code>start</code>.
     *
     * @param start seed value that is combined with the first value in the list
     * @param op function to apply
     *
     * @param <U> type of the value that is computed
     *
     * @return the result of combining the elements with the seed value
     */
    <U> U foldLeft(U start, Function2<U, ? super T, U> op);

    /**
     * Combines the elements of this list using the function op, working from
     * right to left and starting with the value <code>start</code>.
     *
     * @param start seed value that is combined with the last value in the list
     * @param op function to apply
     *
     * @param <U> type of the value that is computed
     *
     * @return the result of combining the elements with the seed value
     */
    <U> U foldRight(U start, Function2<? super T, U, U> op);

    /**
     * Partitions the elements of this list according to the discriminator function.
     *
     * @param f the discriminator function.
     *
     * @param <K> the type of the keys returned by the discriminator function.
     *
     * @return A HashMap from keys to lists where the following invariant holds:
     *
     *         {{{
     *           (xs groupBy f)(k) = xs filter (x => f(x) == k)
     *         }}}
     */
    <K> HashMap<K, ImList<T>> groupBy(Function1<? super T, K> f);
}
