// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPCompositeChange.java 5309 2004-11-06 18:36:02Z shane $
//

package edu.gemini.pot.sp;




/**
 * An instance of <code>SPCompositeChange</code> is passed as the value of
 * composite change events.  Composite change events are fired for any change
 * within a node or any node nested inside of that node.  They provide a
 * means whereby the client can register with one parent node, and receive
 * any event that happens within the parent's descendents.  This class
 * encapsulates the original source of the composite change event (the node
 * that actually changed), the property of that node that was effected,
 * and the old and new values of the property.
 */
public class SPCompositeChange extends SPNestedChange {
    /**
     * Constructs with the property name that changed, the node that was directly
     * modified, and the old and new values of the property.
     */
    public SPCompositeChange(
            String propName,
            ISPNode modifiedNode,
            Object oldValue,
            Object newValue) {
        super(propName, modifiedNode, oldValue, newValue);
    }

}

