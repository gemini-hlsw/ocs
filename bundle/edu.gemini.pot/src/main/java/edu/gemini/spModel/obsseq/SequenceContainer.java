//
// $Id: SequenceContainer.java 5860 2005-03-08 17:10:38Z shane $
//
package edu.gemini.spModel.obsseq;

import java.util.Iterator;

/**
 * An interface that identifies a container of {@link Sequence} objects.
 * {@link Sequence}s themselves contain other Sequences, so they are containers
 * along with {@link ObservingSequence}s.  Specifies required operations for
 * adding, removing, manipulating a list of child Sequence objects.
 */
public interface SequenceContainer {

    /**
     * Gets all the child {@link Sequence} objects that this parent contains,
     * in the order that they occur.
     *
     * @return child Sequence objects; manipulation of the array will not
     * impact the internals of this object
     */
    Sequence[] getAllSequences();

    /**
     * Sets the list of child {@link Sequence} objects to the specified array.
     * The caller may subsequently modify the <code>sequences</code> array
     * without impacting this SequenceContainer.
     *
     * @param sequences Sequence objects in the order that they should occur
     * in this parent container
     */
    void setAllSequences(Sequence[] sequences);

    /**
     * Adds the given {@link Sequence} child at the specified position.
     * Shifts the element at the child currently at the specified position
     * (if any) and any subsequent children to the right (adds one to their
     * indices).
     *
     * @param index index at which the specified child is to be inserted
     * @param sequence the child to insert
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (0 <= index <= getSequenceCount()).
     */
    void addSequence(int index, Sequence sequence);

    /**
     * Adds the given {@link Sequence} child to the end of the list of children.
     *
     * @param sequence child to insert
     */
    void addSequence(Sequence sequence);

    /**
     * Removes all children from this container.
     */
    void clearSequences();

    /**
     * Gets an iterator that will step through the {@link Sequence} children of
     * this container in the order that they appear.
     *
     * @return Iterator of {@link Sequence} children
     */
    Iterator sequenceIterator();

    /**
     * Returns the {@link Sequence} child at the specified <code>index</code>.
     *
     * @param index index of the {@link Sequence} child to be retrieved
     *
     * @return child of this parent container that exists at the specified
     * index
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (0 <= index < getSequenceCount)
     */
    Sequence getSequence(int index);

    /**
     * Returns <code>true</code> if this container has no child
     * {@link Sequence} objects.
     */
    boolean isEmptySequenceContainer();

    /**
     * Removes the {@link Sequence} object at the specified index.  The indices
     * of any remaining children after this one are reduced by one (i.e.,
     * shifted to the right).
     *
     * @param index position of the {@link Sequence} child that should be
     * removed
     *
     * @return {@link Sequence} child at the specified index that was removed
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     * (0 <= index < getSequenceCount)
     */
    Sequence removeSequence(int index);

    /**
     * Removes the indicated child from the list of contained children, assuming
     * it exists in the container.  If not, nothing is done.
     *
     * @param sequence {@link Sequence} child that should be removed
     *
     * @return <code>true</code> if the sequence item is removed,
     * <code>false</code> if it was not a child of this container
     */
    boolean removeSequence(Sequence sequence);

    /**
     * Gets the number of contained {@link Sequence} children in the
     * container.
     */ 
    int getSequenceCount();
}
