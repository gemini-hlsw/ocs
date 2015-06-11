package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.version.JavaVersionMapOps;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.pot.spdb.Locking;
import edu.gemini.pot.sp.SPNodeKeyLocks;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.VersionVector;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implementation class holds data that should be associated with
 * every node in a document.
 */
public class DocumentData implements Serializable {
    private static final Logger LOG = Logger.getLogger(DocumentData.class.getName());

    // The document's key and human readable reference id.
    private final SPNodeKey _docKey;
    private final SPProgramID _docId;
    private final UUID uuid; // The database UUID.
    private final LifespanId lifespanId;

    private final Map<Object, Object> _programClientData;
    private scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> versions = JavaVersionMapOps.emptyVersionMap();

    // The last modification timestamp.
    private long _lastModified;

    DocumentData(SPNodeKey docKey, SPProgramID docId, UUID uuid, LifespanId lifespanId) {
        if (docKey == null) throw new IllegalArgumentException("docKey == null");
        if (uuid == null) throw new IllegalArgumentException("uuid == null");
        this._docKey    = docKey;
        this._docId     = docId;
        this.uuid       = uuid;
        this.lifespanId = lifespanId;
        this._programClientData = Collections.synchronizedMap(new HashMap<Object, Object>());
    }

    SPNodeKey getDocumentKey() {
        return _docKey;
    }

    SPProgramID getDocumentID() {
        return _docId;
    }

    UUID getDatabaseUuid() {
        return uuid;
    }

    LifespanId getLifespanId() {
        return lifespanId;
    }

    long lastModified() {
        getProgramReadLock();
        try {
            return _lastModified;
        } finally {
            returnProgramReadLock();
        }
    }

    void markModified(MemAbstractBase node) {
        getProgramWriteLock();
        try {
            markModified(node, nextVersion(node));
        } finally {
            returnProgramWriteLock();
        }
    }

    VersionVector<LifespanId, Integer> nextVersion(MemAbstractBase node) {
        getProgramWriteLock();
        try {
            final SPNodeKey key = node.getNodeKey();
            final VersionVector<LifespanId, Integer> vector = JavaVersionMapOps.getOrEmpty(versions, key);

            // Avoid counting changes to temporary nodes as modifications.
            // Temporary nodes that never get added to the program shouldn't
            // be recorded in the version map. Sorry.
            if (!vector.isEmpty() || (node.getParent() != null) || (node instanceof ISPRootNode)) {
                return vector.incr(lifespanId);
            } else {
                return vector;
            }
        } finally {
            returnProgramWriteLock();
        }
    }

    void markModified(MemAbstractBase node, VersionVector<LifespanId, Integer> newVersion) {
        if (!newVersion.isEmpty()) {
            getProgramWriteLock();
            try {
                _lastModified = System.currentTimeMillis();
                versions = versions.updated(node.getNodeKey(), newVersion);
            } finally {
                returnProgramWriteLock();
            }
        }
    }

    scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> getVersions() {
        getProgramReadLock();
        try {
            return versions;
        } finally {
            returnProgramReadLock();
        }
    }

    void setVersions(scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> versions) {
        getProgramWriteLock();
        try {
            this.versions = versions;
        } finally {
            returnProgramWriteLock();
        }
    }

    boolean containsVersion(SPNodeKey key) {
        getProgramReadLock();
        try {
            return this.versions.contains(key);
        } finally {
            returnProgramReadLock();
        }
    }

    VersionVector<LifespanId, Integer> versionVector(SPNodeKey key) {
        getProgramReadLock();
        try {
            return JavaVersionMapOps.getOrEmpty(versions, key);
        } finally {
            returnProgramReadLock();
        }
    }

    void setVersionVector(SPNodeKey key, VersionVector<LifespanId, Integer> vv) {
        getProgramWriteLock();
        try {
            versions = versions.updated(key, vv);
        } finally {
            returnProgramWriteLock();
        }
    }

    Integer version(SPNodeKey key, LifespanId lifespanId) {
        return versionVector(key).apply(lifespanId);
    }

    Integer localVersion(SPNodeKey key) {
        return version(key, lifespanId);
    }

    Object getProgramClientData(Object key) {
        return _programClientData.get(key);
    }

    void putProgramClientData(Object key, Object value) {
        _programClientData.put(key, value);
    }

    void removeProgramClientData(Object key) {
        _programClientData.remove(key);
    }

    private static final Level implicitLockingLevel = Level.FINE;

    private void warnIfImplicitLocking() {
        if (!Locking.insideLockingOperation(_docKey) && LOG.isLoggable(implicitLockingLevel)) {
            LOG.log(implicitLockingLevel, "doing implicit locking operation", new Exception());
        }
    }

    public void getProgramReadLock() {
        warnIfImplicitLocking();
        SPNodeKeyLocks.instance.readLock(_docKey);
    }

    public void returnProgramReadLock() {
        SPNodeKeyLocks.instance.readUnlock(_docKey);
    }

    public void getProgramWriteLock() {
        warnIfImplicitLocking();
        SPNodeKeyLocks.instance.writeLock(_docKey);
    }

    public void returnProgramWriteLock() {
        SPNodeKeyLocks.instance.writeUnlock(_docKey);
    }

    public boolean haveProgramWriteLock() {
        return SPNodeKeyLocks.instance.isWriteLockHeld(_docKey);
    }
}
