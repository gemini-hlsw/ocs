package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationRepository;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;

import java.io.*;

/**
 */
public class CalibrationResourceRepository implements CalibrationRepository {

    @Override
    public Version getVersion(Calibration.Type type, String instrument) throws IOException {
        BufferedReader reader = getBufferedResourceReader(type, instrument);
        try {
            String firstLine = reader.readLine();
            return Version.parse(firstLine);
        } finally {
            reader.close();
        }
    }

    @Override
    public CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) throws IOException {
        BufferedReader reader = getBufferedResourceReader(type, instrument);
        try {
            return CalibrationFile.fromReader(reader);
        } finally {
            reader.close();
        }
    }

    private BufferedReader getBufferedResourceReader(Calibration.Type type, String instrument) {
        InputStream is = getClass().getResourceAsStream(getResourceName(type, instrument));
        if (is == null) {
            throw new InternalError("missing resource file: " + getResourceName(type, instrument));
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    private String getResourceName(Calibration.Type type, String instrument) {
        return "/resources/smartgcal/calibrations/" + instrument + "_" + type + ".csv";
    }

}
