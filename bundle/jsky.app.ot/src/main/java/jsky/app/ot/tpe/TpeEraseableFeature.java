// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeEraseableFeature.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;


/**
 * This is an interface supported by TpeImageFeatures that can erase
 * one or more items (such as target positions).
 */
public interface TpeEraseableFeature {
    /**
     * Erase an item, returning true if successful.
     */
    public boolean erase(TpeMouseEvent evt);
}

