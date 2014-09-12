// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IOffsetPosListProvider.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.data;

import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosBase;


/**
 * This is the common interface for data object classes that manage an offset position list.
 */
public abstract interface IOffsetPosListProvider<P extends OffsetPosBase> extends ISPDataObject {

    /**
     * Return true if the offset position list provider feature is enabled.
     */
    public boolean isOffsetPosListProviderEnabled();

    /**
     * Return the offset position list, creating a new one if needed.
     */
    public OffsetPosList<P> getPosList();
}
