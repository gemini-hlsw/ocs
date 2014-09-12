//
// $Id: Container.java 15335 2008-10-30 15:26:53Z swalker $
//

package edu.gemini.spModel.pio;

import java.util.List;

/**
 * Containers indicate the structure of a document. This interface corresponds
 * to the <code>container</code> element in SpXML2.dtd.  Containers can contain
 * both {@link ParamSet} and other Container instances.
 */
public interface Container extends PioNamedNode, ContainerParent {

    /**
     * Returns the hint to the reader about what type of object the container
     * contains.
     */
    String getKind();

    /**
     * Sets the kind of the container.  See {@link #getKind}.  The Container
     * kind is required to be present so the <code>kind</code> argument may
     * not be <code>null</code>.
     *
     * @param kind new setting for the container kind; may <em>not</em> be
     * <code>null</code>
     *
     * @throws NullPointerException if <code>kind</code> is <code>null</code>
     */
    void setKind(String kind);


    /**
     * Returns the type, or broad category, to which the Container belongs.
     * For example, "instrument".
     */
    String getType();

    /**
     * Sets the type of the container. See {@link #getType}.  The Container
     * type is required to be present so the <code>type</code> argument may
     * not be <code>null</code>.
     *
     * @param type new setting for the container type; may <em>not</em> be
     * <code>null</code>
     *
     * @throws NullPointerException if <code>type</code> is <code>null</code>
     */
    void setType(String type);


    /**
     * Returns the subtype, or category refinement, to which the Container
     * belongs. For example, "niri".
     */
    String getSubtype();

    /**
     * Sets the subtype of the container. See {@link #getSubtype}.
     *
     * @param subtype new setting for the container subtype; may be
     * <code>null</code> to clear the setting
     */
    void setSubtype(String subtype);

    /**
     * Sets the name of this Container. For Containers, the name is
     * optional so the <code>name</code> parameter of this method may be
     * <code>null</code>.
     *
     * @param name new name for this Container; may be <code>null</code> to
     * remove the setting
     */
    void setName(String name);

    /**
     * Gets the version of the type/subtype configuration.
     */
    Version getVersion();

    /**
     * Sets the version of the type/subtype configuration.  See
     * {@link #getVersion}.  The Container version is required to be present so
     * the <code>version</code> argument may not be <code>null</code>.
     *
     * @param version new setting for the version; may <em>not</em> be
     * <code>null</code>
     *
     * @throws NullPointerException if <code>version</code> is <code>null</code>
     */
    void setVersion(String version);


    /**
     * Returns the sequence number for this container relative to others
     * in the same parent node.
     */
    int getSequence();

    /**
     * Sets the sequence number for this container.
     */
    void setSequence(int sequence);


    /**
     * Returns the key that uniquely identifies the Container.
     */
    String getKey();

    /**
     * Sets the key for this Container.
     *
     * @param key new setting for the key; may be <code>null</code> to clear
     * the setting
     */
    void setKey(String key);


    /**
     * Gets the number of {@link ParamSet}s contained in this Container;
     * contained {@link Container}s are not included in this count.
     */
    int getParamSetCount();

    /**
     * Gets the first child of this Container that is a {@link ParamSet}
     * instance and that has the given name, if any.
     *
     * @param name name of the {@link ParamSet} to retrieve
     *
     * @return the named {@link ParamSet}, if it exists; <code>null</code> if
     * there are no {@link ParamSet} children with the given name
     */
    ParamSet getParamSet(String name);

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
     */
    ParamSet lookupParamSet(PioPath path);

    /**
     * Gets the subset of children of this Container that are {@link ParamSet}s.
     *
     * @return List of {@link ParamSet}s contained in this Container; if there
     * are no ParamSets, then an empty List is returned
     */
    List getParamSets();

    /**
     * Gets the subset of (immediate) children of this Container that are
     * {@link ParamSet}s and which have the given <code>name</code>.
     *
     * @param name name of the immediate {@link ParamSet} children to retrieve
     *
     * @return List of {@link ParamSet}s contained in this Container which have
     * name <code>name</code> or an empty List if there are no such ParamSets
     */
    List getParamSets(String name);

    /**
     * Adds the given {@link ParamSet} child to this Container node.  If the
     * <code>child</code> is already contained elsewhere, it is first removed
     * from its current parent node.
     *
     * @param child {@link ParamSet} that should be added as a child of this
     * node
     */
    void addParamSet(ParamSet child);

}
