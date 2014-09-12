// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPStructureChange.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;

/**
 * An instance of <code>SPStructureChange</code> is passed as the value of
 * structure change events.  See <code>{@link ISPContainerNode}</code> for
 * more detail on structure change events.  This class encapsulates the
 * original source of the structure change event (the node whose structure
 * changed), the property of that node that was effected, and the new value
 * of the property.
 */
public class SPStructureChange extends SPNestedChange {

    /**
     * Constructs with the property name that changed, the parent whose
     * structure changed, and the old and new values of the property.
     */
    public SPStructureChange(String propName, ISPNode parent,
                             Object oldValue, Object newValue) {
        super(propName, parent, oldValue, newValue);
    }

    /**
     * Gets the (parent) node whose structure changed.  This is just a rewording
     * of the <code>{@link SPNestedChange#getModifiedNode}</code> method.
     */
    public ISPNode getParent() {
        return getModifiedNode();
    }
}

