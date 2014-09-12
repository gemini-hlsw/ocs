// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id:$
//
package edu.gemini.spModel.smartgcal;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationRepository;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;

import java.io.IOException;
import java.util.Date;

public interface UpdatableCalibrationRepository extends CalibrationRepository {

    void clear();

    void writeUpdateTimestamp() throws IOException;
    Date getLastUpdateTimestamp();

    void updateCalibrationFile(Calibration.Type type, String instrument,  Version newestVersion, byte[] data) throws IOException;

}
