// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeCreateableFeature.java 18053 2009-02-20 20:16:23Z swalker $
//
package jsky.app.ot.tpe;


/**
 * This is an interface supported by TpeImageFeatures that can create
 * multiple items (such as target positions).
 */
public interface TpeCreateableFeature {
    /**
     * Return the label that should be on the create button.
     */
    public TpeCreateableItem[] getCreateableItems();
}

