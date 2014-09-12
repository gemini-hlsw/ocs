/*
 * Copyright 2004 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ParamSet.java 7116 2006-06-04 22:13:06Z rnorris $
 */

package edu.gemini.spModel.pio;

import java.util.List;

/**
 * Describes a set of parameters or (recursively) parameter sets, with
 * associated attributes. This interface corresponds to the
 * <code>paramset</code> element in SpXML2.dtd.  ParamSet can contain both
 * {@link Param} and other ParamSet instances.
 *
 * <h2>Ids and References</h2>
 *
 * ParamSets may be given an {@link #getId id} in order to mark the ParamSet as
 * a shared resource that may be referred to in other parts of the
 * {@link Document}.  Other ParamSets may be given a {@link #getReferenceId
 * reference id} that refers to the shared ParamSet.  ParamSets may support
 * either ids or reference ids (or neither) but not both at the same time.  In
 * other words, <pre>(getId() != null) && (getReferenceId() != null)</pre> will
 * always be <code>false</code>. This is enforced in the code by setting the
 * reference id to <code>null</code> when the id is set, and vice versa.
 *
 * <p>When a ParamSet references a target ParamSet, the referring ParamSet
 * acquires the children of the target from the point of view of methods that
 * retrieve child {@link Param}s and nested ParamSets.  A call to
 * {@link #getParams}, for example, will return the {@link Param}s of the
 * referenced ParamSet.
 */
public interface ParamSet extends PioNamedNode, PioNodeParent {

    /**
     * Sets the name of this ParamSet. For ParamSets, the name is not
     * optional so the <code>name</code> parameter of this method may
     * <em>not</em> be <code>null</code>.
     *
     * @param name new name for this ParamSet; may be not be <code>null</code>
     *
     * @throws NullPointerException if <code>name</code> is null
     */
    void setName(String name);

    /**
     * Gets the id of this ParamSet, which serves as a means by which
     * other ParamSets may refer to it.  Only ParamSets intended to be shared
     * should be marked with ids.  Most ParamSets will not have an id.
     *
     * <p>Another ParamSet would reference this one by setting its
     * {@link #getReferenceId reference id} to this value.
     *
     * @return id of the ParamSet, if set; <code>null</code> otherwise
     */
    String getId();

    /**
     * Sets the id of the ParamSet so that it may be referred to by another
     * ParamSet.  See {@link #getId}.
     *
     * <p>If this ParamSet has a {@link #getReferenceId reference id} at the
     * time that this method is called, and <code>id</code> is not
     * <code>null</code>, then the reference id is removed.
     *
     * @param id new id for the ParamSet; may be <code>null</code> to clear the
     * setting
     */
    void setId(String id);

    /**
     * Gets the ParamSets that refer to this one, if any.  A ParamSet refers
     * to this ParamSet if its {@link #getReferenceId reference id} is set to
     * the same non-<code>null</code> value as this ParamSet's
     * {@link #getId id}.
     *
     * @return List of {@link ParamSet} that refer to this ParamSet, or an
     * empty List if there are none
     */
    List getReferences();

    /**
     * Gets the reference id of this ParamSet, which is used to refer to another
     * ParamSet whose {@link #getId id} has the same value.  When a ParamSet
     * refers to another target ParamSet, actions which retrieve or modify
     * contained {@link Param}s or nested ParamSets take place on the target.
     * For example, {@link #addParam} will cause a {@link Param} to be added to
     * the target.
     *
     * @return reference id of the ParamSet, if set; <code>null</code> otherwise
     */
    String getReferenceId();

    /**
     * Sets the reference id of this ParamSet so that it may refer to the
     * ParamSet whose {@link #getId id} has the same non-<code>null</code>
     * value.  See {@link #getReferenceId}.
     *
     * <p>If this ParamSet has an {@link #getId id} at the time that this
     * method is called, and <code>refid</code> is not <code>null</code>, then
     * the <code>id</code> is removed along with any children that this
     * ParamSet may have had.
     *
     * @param refid new reference id for this ParamSet; may be <code>null</code>
     * to clear the setting
     */
    void setReferenceId(String refid);

    /**
     * If this ParamSet refers to another ParamSet (in other words, if its
     * {@link #getReferenceId reference id} is non-<code>null</code>), then
     * this method returns the ParamSet being referenced.
     *
     * @return target ParamSet whose non-<code>null</code> {@link #getId id} is
     * the same value as this ParamSet's {@link #getReferenceId reference id};
     * <code>null</code> if the reference id is not set or if there is no
     * matching ParamSet
     */
    ParamSet getReferent();


    /**
     * Returns the hint to the reader about what the param set is.  For
     * example, "userObj" or "dataObj" referring to Science Program user and
     * data objects.
     */
    String getKind();

    /**
     * Sets the kind of the parameter set.  See {@link #getKind}.
     *
     * @param kind new setting for the param set kind; may be <code>null</code>
     * to clear the setting
     */
    void setKind(String kind);

    /**
     * Returns <code>true</code> (the default) if this param set (and all its
     * children) may be modified; <code>false</code> if explicitly set to
     * <code>false</code> to indicate a read-only param set.
     */
    boolean isEditable();

    /**
     * Sets the editable state of the param set (see {@link #isEditable}).
     *
     * @param editable <code>true</code> if this param set is modifiable,
     * <code>false</code> otherwise
     */
    void setEditable(boolean editable);

    /**
     * Returns <code>true</code> if the param set (and its children) are
     * publicly accessible; <code>false</code> if only privately accessible.
     * This attribute is used, for example, by the Gemini Science Archive to
     * determine which parts of a Phase 2 program can be exposed to GSA users.
     *
     * <p><em>By default, access is public.</em>
     */
    boolean isPublicAccess();

    /**
     * Sets the accessibility state of the param set (see
     * {@link #isPublicAccess}).
     *
     * @param publicAccess <code>true</code> if this param set should be
     * publicly accessible; <code>false</code> if it should be private
     */
    void setPublicAccess(boolean publicAccess);

    /**
     * Returns the sequence number for this parameter set relative to others
     * in the same parent node.
     */
    int getSequence();

    /**
     * Sets the sequence number for this parameter set.
     */
    void setSequence(int sequence);


    // ---------------------------------------------------------------------
    // getXCount()
    // ---------------------------------------------------------------------

    /**
     * Gets the number of {@link Param parameters} contained in this ParamSet;
     * contained ParamSets are not included in this count.
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    int getParamCount();

    /**
     * Gets the number of ParamSets contained in this ParamSet; contained
     * {@link Param}s are not included in this count.
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    int getParamSetCount();


    // ---------------------------------------------------------------------
    // getX(String name)
    // ---------------------------------------------------------------------

    /**
     * Gets the first child of this ParamSet that is a {@link Param} instance
     * and that has the given name, if any.
     *
     * @param name name of the {@link Param} to retrieve
     *
     * @return the named {@link Param}, if it exists; <code>null</code> if there
     * are no {@link Param} children with the given name
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    Param getParam(String name);

    /**
     * Gets the first child of this ParamSet that is a ParamSet instance
     * and that has the given name, if any.
     *
     * @param name name of the ParamSet to retrieve
     *
     * @return the named ParamSet, if it exists; <code>null</code> if there
     * are no ParamSet children with the given name
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    ParamSet getParamSet(String name);



    // ---------------------------------------------------------------------
    // lookupX(PioPath path)
    // ---------------------------------------------------------------------

    /**
     * Looks up the {@link Param} indicated by the given <code>path</code>.  If
     * the <code>path</code> is relative, then the search will be relative to
     * this node.  If absolute, then it will be relative to the highest
     * ancestor of this node.  If the first node indicated by this
     * <code>path</code> is not a {@link Param}, then <code>null</code> is
     * returned.
     *
     * @param path path (relative or absolute) identifying the {@link Param} to
     * retrieve
     *
     * @return {@link Param} indicated by this <code>path</code>, or
     * <code>null</code> if not found or not a {@link Param}
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    Param lookupParam(PioPath path);

    /**
     * Looks up the ParamSet indicated by the given <code>path</code>.  If
     * the <code>path</code> is relative, then the search will be relative to
     * this node.  If absolute, then it will be relative to the highest
     * ancestor of this node.  If the first node indicated by this
     * <code>path</code> is not a ParamSet, then <code>null</code> is
     * returned.
     *
     * @param path path (relative or absolute) identifying the ParamSet to
     * retrieve
     *
     * @return ParamSet indicated by this <code>path</code>, or
     * <code>null</code> if not found or not a ParamSet
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    ParamSet lookupParamSet(PioPath path);



    // ---------------------------------------------------------------------
    // getXs()
    // ---------------------------------------------------------------------

    /**
     * Gets the subset of (immediate) children of this ParamSet that are
     * {@link Param}s.
     *
     * @return {@link Param}s contained in this ParamSet, or an empty List
     * if there are no params
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    List getParams();

    /**
     * Gets the subset of (immediate) children of this ParamSet that are
     * ParamSets.
     *
     * @return ParamSets contained in this ParamSet
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    List<ParamSet> getParamSets();


    // ---------------------------------------------------------------------
    // getXs(String name)
    // ---------------------------------------------------------------------

    /**
     * Gets the subset of (immediate) children of this ParamSet that are
     * {@link Param}s and which have the given <code>name</code>.
     *
     * @param name name of the immediate {@link Param} children to retrieve
     *
     * @return {@link Param}s contained in this ParamSet which have name
     * <code>name</code> or an empty List if there are no such Params
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    List getParams(String name);

    /**
     * Gets the subset of (immediate) children of this ParamSet that are
     * ParamSets and which have the given <code>name</code>.
     *
     * @param name name of the immediate ParamSet children to retrieve
     *
     * @return ParamSets contained in this ParamSet which have name
     * <code>name</code> or an empty List if there are no such ParamSets
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    List<ParamSet> getParamSets(String name);


    // ---------------------------------------------------------------------
    // addX(X x)
    // ---------------------------------------------------------------------

    /**
     * Adds the given {@link Param} child to this ParamSet node.  If the
     * <code>child</code> is already contained elsewhere, it is first removed
     * from its current parent node.
     *
     * @param child {@link Param} that should be added as a child of this node
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    void addParam(Param child);

    /**
     * Adds the given ParamSet child to this ParamSet node.  If the
     * <code>child</code> is already contained elsewhere, it is first removed
     * from its current parent node.
     *
     * @param child ParamSet that should be added as a child of this node
     *
     * @throws IllegalArgumentException if <code>child</code> is an ancestor
     * of this node
     *
     * @throws PioReferenceException if this ParamSet references another (see
     * {@link #getReferenceId}) but the referent is not found
     */
    void addParamSet(ParamSet child);
}

