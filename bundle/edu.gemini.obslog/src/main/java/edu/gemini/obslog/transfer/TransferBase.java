package edu.gemini.obslog.transfer;

import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.pot.sp.SPComponentType;

import java.io.Serializable;

//
// Gemini Observatory/AURA
// $Id: TransferBase.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//
abstract class TransferBase implements Serializable {
    private static final long serialVersionUID = 1;
    
    private OlSegmentType _instType;

    /**
     * The low-level data structure is a unique configuration of parameters from the database and one or more datasets.
     * For instance the parameter programID in system "ocs" is ocs.programID.
     */
    public TransferBase(SPComponentType instType) {
        _instType = new OlSegmentType(instType);
    }

    /**
     * Return the segment type for this observation.  The segment type is used to tell one segment from another.
     * instrument that is used to
     *
     * @return the {@link edu.gemini.obslog.core.OlSegmentType} for this segment.
     */
    public OlSegmentType getType() {
        return _instType;
    }

}
