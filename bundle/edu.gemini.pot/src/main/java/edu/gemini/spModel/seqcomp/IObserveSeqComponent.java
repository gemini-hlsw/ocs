// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: IObserveSeqComponent.java 7987 2007-08-02 23:16:52Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.spModel.obsclass.ObsClass;


/**
 * A simple interface used by configuration builders to recognize
 * Observation sequence components.
 */
public interface IObserveSeqComponent extends ISPSeqObject {

    /**
     * Return the OBSERVE_TYPE property value.
     */
    public String getObserveType();

    /**
     * Set the repeat count and fire an event.
     */
    public void setStepCount(int repeatCount);

    /**
     * Return the observation (charging) class for this object.
     */
    public ObsClass getObsClass();
}

