// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DirtyProgramListener.java 46997 2012-07-26 15:51:35Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.SPUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;


/**
 * A DirtyProgramListener is a composite change listener applied to every
 * program in the database.  Whenever there is a property change on any
 * program, this listener is notified and it marks the program as "dirty".
 * This class is used in the implementation of the
 * <code>{@link StorageManager}</code>.  It contains a thread that
 * periodically checks for "dirty" programs (via the
 * <code>getDirtyPrograms()</code> method of this class) and saves.
 */
@SuppressWarnings("unchecked")
final class DirtyProgramListener<N extends ISPRootNode> implements PropertyChangeListener {
    private final Set<N> _progSet;

    /**
     * Default constructor declared because superclass default constructor
     * throws <code>RemoteException</code>.
     */
    DirtyProgramListener()  {
        _progSet = new HashSet<N>();
    }

    @Override public void propertyChange(PropertyChangeEvent pce) {
        if (SPUtil.isTransientClientDataPropertyName(pce.getPropertyName())) return;

        final Object src = pce.getSource();
        if (!(src instanceof ISPNode)) return;

        ISPRootNode root = ((ISPNode) src).getRootAncestor();
        if (root != null) {
            synchronized (this) { _progSet.add((N)root); }
        }
    }

    /**
     * Gets the modified, "dirty", programs and clears the record of their
     * being dirty.  In other words, immediately after this method is called
     * no programs are marked dirty.
     *
     * @return the modified programs, or <code>null</code> if there are none
     */
    synchronized List<N> getDirtyPrograms() {
        final List<N> lst = _progSet.isEmpty() ? Collections.<N>emptyList() : new ArrayList<N>(_progSet);
        _progSet.clear();
        return lst;
    }

    /**
     * Removes the given program from its collection of modified, "dirty",
     * programs.  If the program isn't in the collection, then nothing is done.
     */
    synchronized void removeProgram(N prog) {
        if (_progSet.size() == 0) return;
        _progSet.remove(prog);
    }
}
