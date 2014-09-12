package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>ProgramManager</code> is the centralized place where references
 * to all the programs in the database are kept.  It provides access to those
 * programs and support for listening to changes when programs are added or
 * removed.
 */
final class ProgramManager<N extends ISPRootNode> {
    private static final Logger LOG = Logger.getLogger(ProgramManager.class.getName());

    private final List<ProgramEventListener<N>> _listeners;  // Can't use EventSupport with non-public inf.
    private final Map<SPNodeKey, N>   _progKeyMap;
    private final Map<SPProgramID, N> _progIdMap;

    /**
     * Constructs with the initial collection of programs.
     */
    ProgramManager(Collection<N> progCollection) {
        _listeners  = new ArrayList<ProgramEventListener<N>>();
        _progKeyMap = new TreeMap<SPNodeKey, N>();
        _progIdMap  = new TreeMap<SPProgramID, N>();

        for (N prog : progCollection) {
            _progKeyMap.put(prog.getProgramKey(), prog);
            final SPProgramID progId = prog.getProgramID();
            if (progId != null) _progIdMap.put(progId, prog);
        }
    }

    /**
     * Adds a program manager listener; it will be informed when programs are
     * added or removed.  If the listener is already present, nothing changes.
     */
    void addListener(ProgramEventListener<N> pml) {
        synchronized (_listeners) {
            if (!_listeners.contains(pml)) _listeners.add(pml);
        }
    }

    /**
     * Removes a program manager listener; it will no longer be informed when
     * programs are added or removed.  If the listener is not registered, nothing
     * changes.
     */
    void removeListener(ProgramEventListener<N> pml) {
        synchronized (_listeners) {
            _listeners.remove(pml);
        }
    }

    /**
     * Fires a <code>ProgramEvent</code>.
     *
     * @param methodName the method to invoke (<code>programAdded</code> or
     * <code>programRemoved</code>)
     */
    private void _fireEvent(N oldProg, N newProg, String methodName) {
        final List<ProgramEventListener<N>> listeners;
        synchronized (_listeners) {
            if (_listeners.size() == 0) return;  // nobody to notify anyway
            listeners = new ArrayList<ProgramEventListener<N>>(_listeners);
        }

        final ProgramEvent<N> pme = new ProgramEvent<N>(this, oldProg, newProg);

        final Method method;
        try {
            method = ProgramEventListener.class.getMethod(methodName, ProgramEvent.class);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Couldn't send event: " + methodName, ex);
            return;
        }

        for (ProgramEventListener<N> pml : listeners) {
            try {
                method.invoke(pml, pme);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Couldn't invoke method " + methodName + "() on " + pml, ex);
            }
        }
    }

    /** Fires a "program added" event. */
    private void _fireProgramEvent(N oldProg, N newProg) {
        if (oldProg == null) {
            _fireEvent(null, newProg, "programAdded");
        } else if (newProg == null) {
            _fireEvent(oldProg, null, "programRemoved");
        } else {
            _fireEvent(oldProg, newProg, "programReplaced");
        }
    }

    /**
     * Fetches the named program if the <code>ProgramManager</code>
     * knows of it; returns <code>null</code> otherwise.
     */
    synchronized N lookupProgram(SPNodeKey progKey) {
        return _progKeyMap.get(progKey);
    }

    synchronized SPNodeKey lookupProgramKey(SPProgramID progID) {
        final N prog = lookupProgramByID(progID);
        return (prog == null) ? null : prog.getProgramKey();
    }

    /**
     * Fetches the named program if found; returns <code>null</code> otherwise
     */
    synchronized N lookupProgramByID(SPProgramID progID) {
        return (progID == null) ? null : _progIdMap.get(progID);
    }

    /**
     * Adds the given <code>newProg</code> notifying listeners.  If the same
     * program has already been added, nothing is done.
     */
    N putProgram(N newProg) throws DBIDClashException {
        final SPNodeKey  key = newProg.getProgramKey();
        final SPProgramID id = newProg.getProgramID();
        final N oldProg;
        synchronized (this) {
            final N tmp0 = _progKeyMap.get(key);
            if (tmp0 == newProg) return null; // already present, do nothing
            oldProg = tmp0;

            // If some other program has the same id, we cannot add newProg
            if (id != null) {
                final N tmp1 = _progIdMap.get(id);
                if ((tmp1 != null) && (tmp1 != oldProg)) throw new DBIDClashException(tmp1.getProgramKey(), id);
            }

            // Whatever the existing program's id was, we are removing it now
            if ((oldProg != null) && (oldProg.getProgramID() != null)) {
                _progIdMap.remove(oldProg.getProgramID());
            }

            _progKeyMap.put(key, newProg);
            if (id != null) _progIdMap.put(id, newProg);
        }

        _fireProgramEvent(oldProg, newProg);
        return oldProg;
    }

    /**
     * Removes the given <code>prog</code>ram, if it is know about.  Otherwise,
     * does nothing.
     *
     * @return <code>true</code> if the <code>ProgramManager</code> is modified
     * as a result of this call
     */
    boolean removeProgram(SPNodeKey key) {
        final N prog;
        synchronized (this) {
            prog = _progKeyMap.remove(key);
            if (prog == null) return false;
            final SPProgramID id = prog.getProgramID();
            if (id != null) _progIdMap.remove(id);
        }

        _fireProgramEvent(prog, null);
        return true;
    }

    /**
     * Fetches a <code>List</code> of the available programs.  The list may
     * be freely modified by the caller.
     */
    synchronized List<N> getPrograms() {
        return new ArrayList<N>(_progKeyMap.values());
    }

    /**
     * Shuts down the program manager, un-exporting all of its programs.
     */
    synchronized void shutdown() {
        _progKeyMap.clear();
        _progIdMap.clear();
    }
}
