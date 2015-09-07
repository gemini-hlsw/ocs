// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$

package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.gemini.calunit.CalUnitParams;

import java.io.Serializable;
import java.util.Set;

/**
 * The configuration of a single calibration step.
 */
public interface CalibrationStep extends Serializable {

    Boolean isFlat();

    Boolean isArc();

    Boolean isBasecalNight();

    Boolean isBasecalDay();

    Set<CalUnitParams.Lamp> getLamps();

    CalUnitParams.Shutter getShutter();

    CalUnitParams.Filter getFilter();

    CalUnitParams.Diffuser getDiffuser();

    Double getExposureTime();

    Integer getCoadds();
}
