package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.util.ReadableNodeName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.*;

/**
 * The abstract base class for the Science Program nodes in the "Mem"
 * implementation.  Handles property change support and user objects.
 * Also provide keeps the node's parent reference.
 */
public abstract class MemAbstractBase implements ISPNode, Serializable {
//    private static final Logger LOG = Logger.getLogger(MemAbstractBase.class.getName());

    // The property change supports for this node
    private transient PropertyChangeSupport _pSupport, _compSupport;

    // The parent of this node, if any.
    private MemAbstractContainer _parent;

    private final DocumentData _docData;

    // The node's unique key.
    private final SPNodeKey _nodeKey;

    // Holds the client data.
    private final Map<String, Object> _clientData = new HashMap<>(4);

    // Holds the transient client data.
    private transient PropertyChangeSupport _transSupport;
    private transient Map<String, Object> _transClientData;

    // Whether to send events or not.
    private boolean _sendEvents = true;

    /**
     * Constructs a MemAbstractBase object.
     */
    protected MemAbstractBase(DocumentData docData, SPNodeKey nodeKey) {
        if (nodeKey == null) nodeKey = new SPNodeKey();
        _nodeKey = nodeKey;
        _docData = docData;
        _init();
    }


    /**
     * Called by the default constructor and by <code>readObject</code> when
     * de-serializing.
     */
    private void _init() {
        _sendEvents = true; // make sure that events are turned on
        _transClientData = new TreeMap<>();
    }

    protected void markModified() {
        getDocumentData().markModified(this);
    }

    void markModified(VersionVector<LifespanId, Integer> newVersion) {
        getDocumentData().markModified(this, newVersion);
    }

    /**
     * Copy constructor.
     */
    protected MemAbstractBase(DocumentData docData, ISPNode node, boolean preserveKeys) {
        this(docData, preserveKeys ? node.getNodeKey() : (node instanceof ISPRootNode ? docData.getDocumentKey() : new SPNodeKey()));

        // Copy the client data.  Clone the objects that are cloneable, add a
        // reference to the ones that aren't.
        Set<String> nameSet = node.getClientDataKeys();
        if (nameSet != null) {
            for (String key : nameSet) {
                Object value = node.getClientData(key);
                if (value instanceof ISPCloneable) {
                    value = ((ISPCloneable) value).clone();
                }
                putClientData(key, value);
            }
        }
    }


    public boolean isSendingEvents() {
        return _sendEvents;
    }

    public PropagationId setSendingEvents(boolean sendEvents) {
        if (sendEvents == _sendEvents) return PropagationId.EMPTY;

        final PropagationId propId;
        _sendEvents = sendEvents;
        if (_sendEvents) {
            propId = PropagationId.next();
            fireCompositeChange(propId, EVENTS_ACTIVATED, this, null, null);
        } else {
            propId = PropagationId.EMPTY;
        }
        return propId;
    }


    public SPNodeKey getNodeKey() {
        return _nodeKey;
    }

    public Integer localVersion() {
        return _docData.localVersion(_nodeKey);
    }

    public DocumentData getDocumentData() {
        return _docData;
    }

    public LifespanId getLifespanId() {
        return getDocumentData().getLifespanId();
    }

    public SPNodeKey getProgramKey() {
        return getDocumentData().getDocumentKey();
    }

    public SPProgramID getProgramID() {
        return getDocumentData().getDocumentID();
    }

    // ****************** RW Lock ***************************************** \\
    // Convenience methods.
    public void getProgramReadLock() {
        _docData.getProgramReadLock();
    }

    public void returnProgramReadLock() {
        _docData.returnProgramReadLock();
    }

    public void getProgramWriteLock() {
        _docData.getProgramWriteLock();
    }

    public void returnProgramWriteLock() {
        _docData.returnProgramWriteLock();
    }

    public boolean haveProgramWriteLock() {
        return _docData.haveProgramWriteLock();
    }

    // ****************** Parents ***************************************** \\

    public ISPContainerNode getParent() {
        return _parent;
    }

    public ISPRootNode getRootAncestor() {
        if (this instanceof ISPRootNode) {
            return (ISPRootNode) this;
        } else if (_parent == null) {
            return null;
        } else {
            return _parent.getRootAncestor();
        }
    }

    public ISPObservation getContextObservation() {
        final ISPObservation obs;
        if (this instanceof ISPObservation) {
            obs = (ISPObservation) this;
        } else if (this instanceof ISPConflictFolder) {
            obs = null;
        } else {
            obs = (_parent == null) ? null : _parent.getContextObservation();
        }
        return obs;
    }

    public Option<SPObservationID> getContextObservationId() {
        return ImOption.apply(getContextObservation())
               .flatMap(o -> ImOption.apply(o.getObservationID()));
    }

    /**
     * Sets the new parent of this node.  The <code>newParent</code> must have
     * been created with the same program data, and there must currently not
     * be a parent of this node.
     * <p/>
     * <p>Assumes write permission.
     */
    private void _setParent(MemAbstractContainer newParent) throws SPNodeNotLocalException, SPTreeStateException {
        if (newParent.getDocumentData() != getDocumentData()) {
            throw new SPNodeNotLocalException();
        }
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do no have write lock.");
        if (_parent != null) throw new SPTreeStateException();
        _parent = newParent;
    }

    /**
     * Attaches this node to the given <code>node</code>.  By default, this
     * method assumes that <code>node</code> should be the new parent.  Subclasses
     * can override to provide other meanings of "attach".  For instance, for an
     * observation component in a catalog, "attach" may mean record a link to an
     * observation.
     * <p/>
     * <p>Assumes write permission.
     */
    protected void attachTo(MemAbstractContainer node) throws SPNodeNotLocalException, SPTreeStateException {
        _setParent(node);
//        markModified();  moving a child impacts the old and new parents, but not the child
        if (node != null) node.markModified();

        // Add this node to the map if it wasn't there previously.  This
        // awful hack was put into place to allow us to create temporary
        // nodes that never get added to the program and yet not pollute the
        // version map.  We use this to run the validity checker "can add"
        // with temporary throw-away nodes.
        //
        // See DocumentData.markModified for the corresponding
        // hack in markModified for nodes without parents. Sorry. :/
        if (!getDocumentData().containsVersion(getNodeKey())) {
            markModified();
        }
    }

    /**
     * This method detaches the node from its parent, which must be
     * equal to the given <code>node</code>.  Subclasses may override to
     * provide other meanings.  For instance, a cataloged obs component's
     * parent will be its catalog, but it can still be detached from an
     * observation.
     * <p/>
     * <p>Assumes write permission.
     */
    protected void detachFrom(MemAbstractBase node) {
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");
        if (_parent != node) throw new IllegalArgumentException("Cannot detach from a node other than the parent.");
        _parent = null;
        if (node != null) node.markModified();
//        markModified(); moving a child impacts the old and new parents, but not the child
    }

    // ****************** Handling of property changes ******************* \\

    public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
        _getPropertySupport().addPropertyChangeListener(propName, pcl);
    }

    public void removePropertyChangeListener(String propName, PropertyChangeListener pcl) {
        _getPropertySupport().removePropertyChangeListener(propName, pcl);
    }

    /**
     * Fires a property change event to registered listeners.
     */
    protected PropagationId firePropertyChange(String propName, Object oldValue, Object newValue) {
        if (!isSendingEvents()) return PropagationId.EMPTY;

        PropagationId propId = PropagationId.next();
        if (_pSupport != null) {
            PropertyChangeEvent pce = new PropertyChangeEvent(this, propName, oldValue, newValue);
            pce.setPropagationId(propId);
            _pSupport.firePropertyChange(pce);
        }
        fireCompositeChange(propId, propName, this, oldValue, newValue);
        return propId;
    }

    /**
     * Gets a reference to the RemotePropertyChangeSupport object, creating it
     * if necessary.
     */
    private synchronized PropertyChangeSupport _getPropertySupport() {
        if (_pSupport == null) {
            _pSupport = new PropertyChangeSupport(this);
        }
        return _pSupport;
    }


    // ****************** Handling of composite changes **************** \\

    public void addCompositeChangeListener(PropertyChangeListener rel) {
        _getCompositeSupport().addPropertyChangeListener(rel);
    }

    public void removeCompositeChangeListener(PropertyChangeListener rel) {
        _getCompositeSupport().removePropertyChangeListener(rel);
    }

    /**
     * Fires a composite event to registered listeners.  Passes the event
     * along to any parent(s) to notify their composite listeners.
     */
    void fireCompositeChange(PropagationId propId, String propName, ISPNode node, Object oldValue, Object newValue) {
        if (!isSendingEvents()) return;

        SPCompositeChange cc;
        cc = new SPCompositeChange(propName, node, oldValue, newValue);
        cc.setPropagationId(propId);
        if (_compSupport != null) {
            _compSupport.firePropertyChange(cc);
        }
        MemAbstractBase parent = (MemAbstractBase) getParent();
        if (parent != null) {
            parent.fireCompositeChange(propId, propName, node, oldValue, newValue);
        }

        // Let any ClientData objects that are interested know about the
        // updates.
        for (ISPEventMonitor em : getEventMonitors()) em.propertyChanged(cc);
    }

    protected List<ISPEventMonitor> getEventMonitors() {
        final List<ISPEventMonitor> ems = new ArrayList<>();
        for (Object cd : getClientData()) {
            if (cd instanceof ISPEventMonitor) ems.add((ISPEventMonitor) cd);
        }
        for (Object tcd : getTransientClientData()) {
            if (tcd instanceof ISPEventMonitor) ems.add((ISPEventMonitor) tcd);
        }
        return ems;
    }


    /**
     * Gets a reference to the RemotePropertyChangeSupport object for composite
     * changes, creating it if necessary.
     */
    private synchronized PropertyChangeSupport _getCompositeSupport() {
        if (_compSupport == null) {
            _compSupport = new PropertyChangeSupport(this);
        }
        return _compSupport;
    }


    // ****************** Handling of client data ********************* \\

    private static Object copyClientData(Object dataObj) {
        if (dataObj == null) {
            return null;
        }
        if (dataObj instanceof ISPCloneable) {
            dataObj = ((ISPCloneable) dataObj).clone();
        }
        return dataObj;
    }

    @Override
    public boolean hasStaffOnlyFields() {
        getProgramReadLock();
        try {
            return _clientData.get(DATA_OBJECT_KEY) instanceof ISPStaffOnlyFieldProtected;
        } finally {
            returnProgramReadLock();
        }
    }

    public ISPDataObject getDataObject() {
        return (ISPDataObject) getClientData(DATA_OBJECT_KEY);
    }

    public PropagationId setDataObject(ISPDataObject newValue) {
        return setDataObject(newValue, false);
    }

    public PropagationId setDataObject(ISPDataObject newValue, boolean conflicts) {
        if (!conflicts) {
            return putClientData(DATA_OBJECT_KEY, newValue);
        } else {
            getProgramWriteLock();
            try {
                DataObjectConflict doc = new DataObjectConflict(DataObjectConflict.Perspective.LOCAL, getDataObject());
                putClientData(CONFLICTS_KEY, getConflicts().withDataObjectConflict(doc));
                return putClientData(DATA_OBJECT_KEY, newValue);
            } finally {
                returnProgramWriteLock();
            }
        }
    }

    public boolean hasConflicts() {
        return !getConflicts().isEmpty();
    }

    public Conflicts getConflicts() {
        Conflicts c = (Conflicts) getClientData(CONFLICTS_KEY);
        return (c == null) ? Conflicts.EMPTY : c;
    }

    public void setConflicts(Conflicts c) {
        if ((c == null) || c.isEmpty()) {
            removeClientData(CONFLICTS_KEY);
        } else {
            putClientData(CONFLICTS_KEY, c);
        }
    }

    public void swapDataObjectConflict() {
        getProgramWriteLock();
        try {
            final Conflicts oldCon = getConflicts();
            if (!oldCon.dataObjectConflict.isEmpty()) {
                final DataObjectConflict oldDoc = oldCon.dataObjectConflict.getValue();
                final DataObjectConflict newDoc = new DataObjectConflict(oldDoc.perspective.opposite(), getDataObject());
                putClientData(CONFLICTS_KEY, oldCon.withDataObjectConflict(newDoc));
                setDataObject(oldDoc.dataObject);
            }
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addConflictNote(Conflict.Note cn) {
        setConflicts(getConflicts().withConflictNote(cn));
    }

    public void resolveDataObjectConflict() {
        setConflicts(getConflicts().resolveDataObjectConflict());
    }

    public void resolveConflict(Conflict.Note cn) {
        setConflicts(getConflicts().resolveConflictNote(cn));
    }

    public void resolveConflicts() {
        setConflicts(Conflicts.EMPTY);
    }

    @Override public VersionVector<LifespanId, Integer> getVersion() {
        return _docData.versionVector(_nodeKey);
    }

    @Override public PropagationId setDataObjectAndVersion(ISPDataObject dataObject, VersionVector<LifespanId, Integer> newVersion) {
        return putClientDataAndVersion(DATA_OBJECT_KEY, dataObject, newVersion);
    }

    synchronized List<Object> getClientData() {
        getProgramReadLock();
        try {
            return new ArrayList<>(_clientData.values());
        } finally {
            returnProgramReadLock();
        }
    }

    public Set<String> getClientDataKeys() {
        getProgramReadLock();
        try {
            // return a copy since the Set returned by the entrySet() method is
            // backed by the _clientData Map
            return new HashSet<>(_clientData.keySet());
        } finally {
            returnProgramReadLock();
        }
    }

    public Object getClientData(String name) {
        getProgramReadLock();
        try {
            return copyClientData(_clientData.get(name));
        } finally {
            returnProgramReadLock();
        }
    }

    public PropagationId putClientData(String name, Object obj) {
        getProgramWriteLock();

        try {
            return putClientDataAndVersion(name, obj, getDocumentData().nextVersion(this));
        } finally {
            returnProgramWriteLock();
        }
    }

    private PropagationId putClientDataAndVersion(String name, Object obj, VersionVector<LifespanId, Integer> newVersion) {
        getProgramWriteLock();

        final PropagationId propId;
        try {
            // RCN: If it's the data object, it must be typed
            if (DATA_OBJECT_KEY.equals(name) && !(obj instanceof ISPDataObject)) {
                throw new IllegalArgumentException("Primary data object (client data object " + DATA_OBJECT_KEY + ") must implement ISPDataObject.");
            }

            Object oldValue = copyClientData(_clientData.get(name));
            Object newValue = copyClientData(obj);
            if (oldValue == newValue) {
                // copy didn't work
                oldValue = null;
            }

            _clientData.put(name, newValue);

            final String propName = SPUtil.getClientDataPropertyName(name);
            final Object evtValue;
            if (DATA_OBJECT_KEY.equals(name) && !DataObjectBlob.same(oldValue, newValue)) {
                evtValue = copyClientData(newValue);
                if (!getVersion().equals(newVersion)) markModified(newVersion);
            } else {
                // ideally all non "data object" client data is immutable ...
                evtValue = obj;
            }
            propId = firePropertyChange(propName, oldValue, evtValue);
        } finally {
            returnProgramWriteLock();
        }
        return propId;
    }

    public void removeClientData(String name) {
        getProgramWriteLock();
        try {
            if (_clientData.containsKey(name)) {
                Object val = _clientData.remove(name);
                markModified();
                String propName = SPUtil.getClientDataPropertyName(name);
                firePropertyChange(propName, val, null);
            }
        } finally {
            returnProgramWriteLock();
        }
    }

    // ****************** Handling of transient client data ********************* \\
    private synchronized PropertyChangeSupport _getTransientPropertySupport() {
        // lazily created because it is transient.  Upon deserialization it is
        // reset to null.
        if (_transSupport == null) {
            _transSupport = new PropertyChangeSupport(this);
        }
        return _transSupport;
    }

    @Override
    public void addTransientPropertyChangeListener(String propName, PropertyChangeListener pcl) {
        _getTransientPropertySupport().addPropertyChangeListener(propName, pcl);
    }

    @Override
    public void removeTransientPropertyChangeListener(String propName, PropertyChangeListener pcl) {
        _getTransientPropertySupport().removePropertyChangeListener(propName, pcl);
    }

    synchronized List<Object> getTransientClientData() {
        return new ArrayList<>(_transClientData.values());
    }

    public void putTransientClientData(String key, Object newValue) {
        final PropertyChangeSupport pcs;
        final PropertyChangeEvent pce;
        synchronized (this) {
            final Object oldValue = _transClientData.get(key);
            if (newValue == null) {
                _transClientData.remove(key);
            } else {
                _transClientData.put(key, newValue);
            }
            final String transProp = SPUtil.getTransientClientDataPropertyName(key);
            pce = new PropertyChangeEvent(this, transProp, oldValue, newValue);
            pcs = _getTransientPropertySupport();
        }
        pcs.firePropertyChange(pce);
    }

    public synchronized Object getTransientClientData(String key) {
        return _transClientData.get(key);
    }

    public void removeTransientClientData(String key) {
        putTransientClientData(key, null);
    }


    /**
     * Implements readObject to also store the stub in the stub map.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        _init();
    }

    /**
     * Implements writeObject to make sure that read permission is held while
     * writing this node.  Throws an IOException if read permission is not
     * held.  (To serialize a program, use the MemProgram.serializeProgram()
     * method.)
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        getProgramReadLock();
        try {
            oos.defaultWriteObject();
        } finally {
            returnProgramReadLock();
        }
    }

    private static String toString(final String indent, final ISPNode n) {
        final StringBuilder buf = new StringBuilder();
        buf.append(indent).append(ReadableNodeName.format(n)).append("\n");
        final String childIndent = "  " + indent;
        if (n instanceof ISPContainerNode) {
            for (ISPNode child : ((ISPContainerNode) n).getChildren()) {
                buf.append(toString(childIndent, child));
            }
        }
        return buf.toString();
    }

    @Override public String toString() {
        return toString("", this);
    }
}
