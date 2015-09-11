// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$

package edu.gemini.spModel.gemini.calunit.smartgcal;

import edu.gemini.spModel.gemini.calunit.calibration.CalibrationStep;

public interface Calibration extends CalibrationStep {

    public enum Type {
        ARC,
        FLAT
    }

    /** Repeat count. */
    Integer getObserve();
}
