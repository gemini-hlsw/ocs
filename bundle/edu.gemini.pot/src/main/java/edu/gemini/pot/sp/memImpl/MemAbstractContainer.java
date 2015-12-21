package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>MemAbstractContainer</code> is the abstract implementation
 * of the <code>ISPContainerNode</code> interface.  It provides support
 * for registering structure listeners and firing structure events.
 */
public abstract class MemAbstractContainer extends MemProgramNodeBase implements ISPContainerNode {
    private static final Logger LOG = Logger.getLogger(MemAbstractContainer.class.getName());

// RCN: may or may not be useful
//    protected void validateCardinality() {
//        final Map<SPComponentType, Integer> counts = new HashMap<SPComponentType, Integer>();
//        for (SPComponentType t: SPComponentType.values())
//            counts.put(t, 0);
//        for (ISPNode n: getChildren()) {
//            final SPComponentType t = n.getDataObject().getType();
//            final int count = counts.get(t);
//            counts.put(t, count + 1);
//        }
//        for (SPComponentType t: SPComponentType.values()) {
//            final int count = counts.get(t);
//            final Cardinality cardinality = getCardinality(t);
//            if (!cardinality.contains(count)) {
//                throw new IllegalArgumentException(String.format(
//                        "Cardinality violation for %s: expected %s, found %d",
//                        t, cardinality, count));
//            }
//        }
//    }

    /**
     * A class that supports the {@link MemAbstractContainer#setChildren} and
     * the {@link MemAbstractContainer#setTypedChildren} method that it calls.
     * Supports the implementation of {@link MemAbstractContainer#setChildren}
     * in the concrete subclasses. Provides access to the new children based
     * upon the child type.
     */
    protected static class TypedChildren {
        private Map<Class<?>, List<ISPNode>> _typedChildren = new HashMap<>();

        private TypedChildren() {
        }

        private void addChild(ISPNode child) {
            Class<?> type = child.getClass();
            List<ISPNode> childList = _typedChildren.get(type);
            if (childList == null) {
                childList = new ArrayList<>();
                _typedChildren.put(type, childList);
            }
            childList.add(child);
        }

        /**
         * Gets the children that are of the given class.
         *
         * @return unmodifiable List of children of class <code>type</code>
         */
        public <T extends ISPNode> List<T> getChildren(Class<T> type) {
            @SuppressWarnings("unchecked")
            List<T> childList = (List<T>) _typedChildren.get(type);
            if (childList == null) return Collections.emptyList();
            return Collections.unmodifiableList(childList);
        }

        /**
         * Gets the only child of the given type (if any).
         *
         * @return child of specified type, if any. <code>null</code>
         * otherwise
         *
         * @throws SPTreeStateException if there are multiple children of the
         * specified type
         */
        public <T extends ISPNode> T getOnlyChild(Class<T> type) throws SPTreeStateException {
            final List<T> children = getChildren(type);
            switch (children.size()) {
                case 0: return null;
                case 1: return children.get(0);
                default: throw new SPTreeStateException("Cannot support multiple children of type: " + type);
            }
        }

        /**
         * Verifies that the children in this TypedChildren object are only of
         * the given types.  If any child is of any other class then a
         * ClassCastException is thrown.
         *
         * @param validTypes valid types for this node
         */
        public void verify(Class<?>[] validTypes) {
            Set<Class<?>> childTypes = new HashSet<>(_typedChildren.keySet());
            for (Class<?> type : validTypes) {
                // Remove this valid type from the set of actual child types,
                // if it exists.
                childTypes.remove(type);
            }

            // Now if there are any types left over in the set of actual
            // child types we know they are not valid.
            if (childTypes.size() > 0) {
                // Format the invalid types in a string so that the exception
                // will be more meaningful.
                StringBuilder buf = new StringBuilder("Illegal child type(s) for ");
                buf.append(getClass().getName()).append(": ");
                Iterator<Class<?>> it = childTypes.iterator();
                buf.append(it.next().getName());
                while (it.hasNext()) {
                    buf.append(", ").append(it.next().getName());
                }
                throw new ClassCastException(buf.toString());
            }
        }
    }

    // The structure change support for this node
    private transient PropertyChangeSupport _structSupport;

    private MemConflictFolder conflictFolder;

    protected MemAbstractContainer(DocumentData documentData, SPNodeKey key) {
        super(documentData, key);
    }

    /**
     * Copy constructor.
     */
    protected MemAbstractContainer(DocumentData documentData, ISPContainerNode node,
                                   boolean preserveKeys) {
        super(documentData, node, preserveKeys);

        // Cannot copy the conflict folder here since the factory requires a
        // program and the MemProgram is itself a MemAbstractContainer
    }

    public ISPConflictFolder getConflictFolder() {
        getProgramReadLock();
        try {
            return conflictFolder;
        } finally {
            returnProgramReadLock();
        }
    }

    public void setConflictFolder(ISPConflictFolder folder) throws SPNodeNotLocalException, SPTreeStateException {
        MemConflictFolder node = (MemConflictFolder) folder;
        if (this.conflictFolder == folder) return;

        getProgramWriteLock();
        try {
            MemConflictFolder oldValue = this.conflictFolder;
            this.conflictFolder = node;
            updateParentLinks(oldValue, node);
            firePropertyChange(CONFLICT_FOLDER_PROP, oldValue, node);
            fireStructureChange(CONFLICT_FOLDER_PROP, this, oldValue, node);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeConflictFolder() {
        try {
            setConflictFolder(null);
        } catch (SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, "Unexpected node not local removing conflict folder", ex);
        } catch (SPTreeStateException ex) {
            LOG.log(Level.SEVERE, "Unexpected tree state removing conflict folder", ex);
        }
    }

    // ****************** Handling of Structure Changes ******************* \\

    public void addStructureChangeListener(PropertyChangeListener rel) {
        _getStructureSupport().addPropertyChangeListener(rel);
    }

    public void removeStructureChangeListener(PropertyChangeListener rel) {
        _getStructureSupport().removePropertyChangeListener(rel);
    }

    /**
     * Fires a structure event to registered listeners.  Passes the event
     * along to any parent(s) to notify their structure listeners.
     */
    protected void fireStructureChange(String propName, ISPNode parent, Object oldValue, Object newValue) {
        if (!isSendingEvents()) return;
        SPStructureChange sc;
        sc = new SPStructureChange(propName, parent, oldValue, newValue);
        if (_structSupport != null) {
            _structSupport.firePropertyChange(sc);
        }
        MemAbstractBase myParent = (MemAbstractBase) getParent();
        if (myParent != null) {
            MemAbstractContainer con = (MemAbstractContainer) myParent;
            con.fireStructureChange(propName, parent, oldValue, newValue);
        }

        // Let any interested ClientData objects know about the change.
        for (ISPEventMonitor em : getEventMonitors()) em.structureChanged(sc);
    }

    /**
     * Gets a reference to the RemotePropertyChangeSupport object, creating it
     * if necessary.
     */
    private synchronized PropertyChangeSupport _getStructureSupport() {
        if (_structSupport == null) {
            _structSupport = new PropertyChangeSupport(this);
        }
        return _structSupport;
    }

    // ********************** Children ******************************** \\

    /**
     * Implements the {@link ISPContainerNode#setChildren} method by splitting
     * the given <code>children</code> list into sub-lists based upon each
     * element's class.  The sub-lists are then passed to
     * {@link #setTypedChildren}, which must be implemented in each subclass.
     *
     * @param children new child nodes for this container
     */
    public void setChildren(List<ISPNode> children) throws SPException {
        final TypedChildren tc = new TypedChildren();
        for (Iterator<ISPNode> it = children.iterator(); it.hasNext(); ) {
            tc.addChild(it.next());
        }

        setTypedChildren(tc);
    }

    /**
     * Sets the children of this container node.  Removes any children of any
     * type not in the given <code>children</code> object.
     *
     * @param children new children of this container, separated by type
     * @throws SPNodeNotLocalException if any of the observations in the
     *                                 <code>obsList</code> were not created in the same JVM as this program
     * @throws SPTreeStateException    if any of the observations in the
     *                                 <code>obsList</code> are already in another program or if they are not
     *                                 valid types for this node
     */
    protected abstract void setTypedChildren(TypedChildren children)
            throws SPException;


    /**
     * Attaches each child in the newChildren collection to this node.  If any
     * of the children reject this change, an exception is thrown and any
     * newly added children are removed.
     * <p/>
     * <p>Assumes write permission.
     */
    @SuppressWarnings("rawtypes")
    protected void attachChildren(Collection newChildren) throws SPNodeNotLocalException, SPTreeStateException {
        // Make sure write permission is held.
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");

        // Remember the children that are attached, in case we have to roll back.
        try {
            for (Object aNewChildren : newChildren) {
                MemAbstractBase mab = (MemAbstractBase) aNewChildren;
                if (mab.getParent() != this) mab.attachTo(this);
            }
        } catch (SPNodeNotLocalException | SPTreeStateException ex) {
            throw ex;
        }
    }

    /**
     * Extracts all the children from this node, by setting their parent links
     * to null.
     * <p/>
     * <p>Assumes write permission.
     */
    @SuppressWarnings("rawtypes")
    protected void extractChildren(Collection children) {
        // Make sure write permission is held.
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");
        for (Object aChildren : children) {
            MemAbstractBase mab = (MemAbstractBase) aChildren;
            mab.detachFrom(this);
        }
    }

    /**
     * Updates the given <code>oldList</code> to contain the nodes in the
     * <code>newList</code>.  It is expected that the <code>oldChildren</code>
     * and <code>newChildren</code> will be a references to an internal list of
     * children.  This method takes care of updating parents in any nodes being
     * removed or added.
     * <p/>
     * <p>Assumes write permission.
     *
     * @throws SPNodeNotLocalException if any object in the <code>newList</code>
     *                                 is not local
     */
    // CQ This method cannot be properly made generic, it shouldn't be writing to oldChildren but instead return a new copy
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void updateChildren(Collection oldChildren, Collection newChildren) throws SPNodeNotLocalException, SPTreeStateException {
        // Make sure write permission is held.
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");

        // Split the children in the union of oldChildren and newChildren into
        // two collections.  "rmSet" will contain only the nodes to be removed
        // and "addList" will contain only the nodes to be added.
        Set rmSet = new HashSet(oldChildren);
        List addList = new LinkedList();
        for (Object aNewChildren : newChildren) {
            MemAbstractBase mab = (MemAbstractBase) aNewChildren;

            // There are 2 possibilities for mab:
            //
            // 1. It is in both collections (and should not be changed)
            // 2. It is only in newChildren (and should be attached)
            //
            // If rmSet.remove(mab) succeeds, we know mab is in both collections
            // (so it is correct to remove it from rmSet -- which will be left
            // with only children to extract).  If rmSet.remove(mab) fails, it
            // is only in the newChildren collection.

            if (!rmSet.remove(mab)) {
                addList.add(mab);
            }
        }

        // Attach the new children.  This may fail with an SPTreeStateException,
        // in which case any new children will not be added and any children to
        // remove will not be removed (nothing changes).
        final Integer v0 = localVersion();
        attachChildren(addList);
        extractChildren(rmSet);
        final Integer v1 = localVersion();

        // Finally, update the old list to contain only the members of the
        // new list.
        // If we are just rearranging items within this node, no modification
        // is recorded as a result of attaching or extracting children so be
        // sure to mark the node modified here.
        if ((v0.equals(v1)) && !oldChildren.equals(newChildren)) markModified();
        oldChildren.clear();
        oldChildren.addAll(newChildren);
    }

    protected void updateParentLinks(MemAbstractBase oldChild, MemAbstractBase newChild) throws SPNodeNotLocalException, SPTreeStateException {
        if (oldChild == newChild) return;
        if (oldChild != null) oldChild.detachFrom(this);
        if (newChild != null) newChild.attachTo(this);
    }

    /**
     * Check each element of the given <code>collection</code> to make sure that
     * each is an instance of the given <code>type</code>.  This method is used
     * extensively in the implementation where new <code>Collection</code>
     * properties are assigned.  Since <code>Collection</code>s can hold any
     * <code>Object</code> reference, it is important that when a
     * <code>Collection</code> property is assigned it must contain only objects
     * of the proper type.
     *
     * @param collection the <code>Collection</code> whose members should be
     *                   checked
     * @param type       the <code>Class</code> of which each collection member should
     *                   be an instance
     * @throws ClassCastException if any member of the <code>collection</code>
     *                            is not an instance of the given <code>type</code>
     */
    public static void checkChildTypes(Collection<?> collection, Class<?> type)
            throws SPTreeStateException {
        for (Object obj : collection) {
            if (!type.isInstance(obj)) {
                throw new SPTreeStateException("Object " + obj + " is not instance of " +
                        type.getName() + ".");
            }
        }
    }

}

