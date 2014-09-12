// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: OT.java 36881 2011-08-26 20:02:12Z fnussber $

package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.io.IOException;
import java.io.Serializable;

/**
 * Simple interface for a smart gemini calibration repository that holds versioned calibration files for
 * different instruments identified by their names.
 */
public interface CalibrationRepository extends Serializable {

    /**
     * Gets the current version number of the calibrations file for the instrument.
     * @param type the calibration type (flat or arc)
     * @param instrument the instrument name
     * @return the current version number
     */
    Version getVersion(Calibration.Type type, String instrument) throws IOException;

    /**
     * Gets the calibration file from the repository for a given instrument.
     * @param type the calibration type (flat or arc)
     * @param instrument the instrument name
     * @return the calibration file data
     */
    CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) throws IOException;

}
