// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ICoaddExpSeqComponent.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.seqcomp;


/**
 * A simple interface used by configuration builders to recognize
 * Observation sequence components that include exposure time, repeat
 * and coadd count.
 */
public interface ICoaddExpSeqComponent extends IObserveSeqComponent {

    /**
     * Return the exposure time.
     */
    double getExposureTime();

    /**
     * Set the exposure time and fire an event.
     */
    void setExposureTime(double expTime);

    /**
     * Return the current coadd count.
     */
    int getCoaddsCount();

    /**
     * Set the coadds count and fire an event.
     */
    void setCoaddsCount(int coaddsCount);

}

