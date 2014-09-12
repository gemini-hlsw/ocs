// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ISPSeqComponent.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.sp;


import java.util.List;


/**
 * This is the interface for a Science Program Sequence Component node.
 */
public interface ISPSeqComponent extends ISPContainerNode, ISPProgramNode {
    /**
     * Names the property in the <code>{@link SPStructureChange}</code>
     * object delivered when a child (nested) sequence component is
     * added or removed.
     */
    String SEQ_COMPONENTS_PROP = "SeqComponents";

    /**
     * Returns the type of this sequence component.
     */
    SPComponentType getType();

    /**
     * Returns the <code>List</code> of sequence components contained in
     * this sequence component.
     *
     * @return a <code>List</code> of contained
     *         <code>{@link ISPSeqComponent}</code>
     */
    List<ISPSeqComponent> getSeqComponents();

    /**
     * Replaces the <code>List</code> of child (nested) sequence components
     * held by this sequence component.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *   <li>property name = {@link #SEQ_COMPONENTS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of
     *       <code>{@link ISPSeqComponent}</code>
     * </ul>
     *
     * @param seqList a <code>List</code> that contains the components
     *        to be contained by this sequence component
     *
     * @throws SPNodeNotLocalException if any of the components in the
     * <code>seqList</code> were not created in the same JVM as this
     * sequence component
     *
     * @throws SPTreeStateException if any of the components in the
     * <code>seqList</code> are already in another observation or
     * sequence component
     */
    void setSeqComponents(List<? extends ISPSeqComponent> seqList)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Adds a nested <code>ISPSeqComponent</code> to this
     * sequence component.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *  <li>property name = {@link #SEQ_COMPONENTS_PROP}
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of
     *      <code>{@link ISPSeqComponent}</code>
     * </ul>
     *
     * @param seqComp the <code>ISPSeqComponent<code> to be added to this
     * sequence component
     *
     * @throws SPNodeNotLocalException if the <code>seqComp</code> was not
     * created in the same JVM as this sequence component
     *
     * @throws SPTreeStateException if the <code>seqComp</code> is already
     * in another sequence component or observation
     */
    void addSeqComponent(ISPSeqComponent seqComp)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Add a nested <code>ISPSeqComponent</code> to this sequence
     * component at a location given by <code>index</code>.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *  <li>property name = {@link #SEQ_COMPONENTS_PROP}
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of
     *      <code>{@link ISPSeqComponent}</code>
     * </ul>
     *
     * @param index the place to locate the <code>ISPSeqComponent</code>
     * @param seqComp the <code>ISPSeqComponent</code> to be added to this
     * sequence component
     *
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range
     * <code>(index < 0 || index > # components)
     *
     * @throws SPNodeNotLocalException if the <code>seqComp</code> was not
     * created in the same JVM as this sequence component
     *
     * @throws SPTreeStateException if the <code>seqComp</code> is already
     * in another sequence component or observation
     */
    void addSeqComponent(int index, ISPSeqComponent seqComp)
            throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException;

    /**
     * Removes a nested <code>ISPSeqComponent</code> from this
     * sequence component.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *  <li>property name = {@link #SEQ_COMPONENTS_PROP}
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of
     *      <code>{@link ISPSeqComponent}</code>
     * </ul>
     *
     * @param seqComp the nested <code>ISPSeqComponent</code> to be
     * removed from this sequence component
     */
    void removeSeqComponent(ISPSeqComponent seqComp);

    /**
     * Gets the number of steps that will be produced by this tree of sequence
     * components, if any.
     */
    int getStepCount();
}

