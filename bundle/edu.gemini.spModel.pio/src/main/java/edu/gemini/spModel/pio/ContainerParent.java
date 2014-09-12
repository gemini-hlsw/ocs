//
// $Id: ContainerParent.java 5303 2004-11-05 21:59:10Z shane $
//

package edu.gemini.spModel.pio;

import java.util.List;

/**
 * An interface that marks a parent which can contain {@link Container}s.
 */
public interface ContainerParent extends PioNodeParent {

    /**
     * Gets the number of Containers nested inside this Container;
     * contained {@link ParamSet}s are not included in this count.
     */
    int getContainerCount();

    /**
     * Gets the first child of this Container that is also a Container
     * instance and that has the given name, if any.
     *
     * @param name name of the nested Container to retrieve
     *
     * @return the named Container, if it exists; <code>null</code> if
     * there are no Container children with the given name
     */
    Container getContainer(String name);

    /**
     * Looks up the Container indicated by the given <code>path</code>.  If
     * the <code>path</code> is relative, then the search will be relative to
     * this node.  If absolute, then it will be relative to the highest
     * ancestor of this node.  If the first node indicated by this
     * <code>path</code> is not a Container, then <code>null</code> is
     * returned.
     *
     * @param path path (relative or absolute) identifying the Container to
     * retrieve
     *
     * @return Container indicated by this <code>path</code>, or
     * <code>null</code> if not found or not a Container
     */
    Container lookupContainer(PioPath path);

    /**
     * Gets the subset of children of this Container that are nested Containers.
     *
     * @return List of Containers nested inside of this Container, or an
     * empty List if there are no such Containers
     */
    List getContainers();

    /**
     * Gets the subset of (immediate) children of this Container that are also
     * Containers and which have the given <code>name</code>.
     *
     * @param name name of the immediate Container children to retrieve
     *
     * @return List of Containers nested in this Container which have
     * name <code>name</code>, or an empty List if there are no such Containers
     */
    List getContainers(String name);

    /**
     * Adds the given Container child to this Container node.  If the
     * <code>child</code> is already contained elsewhere, it is first removed
     * from its current parent node.
     *
     * @param child Container that should be added as a child of this
     * node
     */
    void addContainer(Container child);
}
