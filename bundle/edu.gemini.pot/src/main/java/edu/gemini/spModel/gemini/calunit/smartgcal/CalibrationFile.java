// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.io.*;

public class CalibrationFile {

    final Version version;
    final String data;

    /**
     * Creates a new calibration file.
     * @param version
     * @param data
     */
    public CalibrationFile(Version version, String data) {
        this.version = version;
        this.data = data;
    }

    /**
     * Gets the calibration file version.
     * @return
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Gets the calibration file data.
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     * Gets a string representation of the actual calibration file data prefixed with the
     * version information (i.e. revision number and a timestamp).
     * @return a string representing the calibration file with version information
     */
    public String toString() {
        return String.format("%s%n%s", version, data);
    }

    public static CalibrationFile fromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            return fromReader(reader);
        } finally {
            reader.close();
        }
    }

    public static CalibrationFile fromString(String s) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(s));
        try {
            return fromReader(reader);
        } finally {
            reader.close();
        }
    }

    public static CalibrationFile fromReader(BufferedReader reader) throws IOException {
        Version version = Version.parse(reader.readLine());
        StringBuffer data = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            data.append(line);
            data.append("\n");
        }
        return new CalibrationFile(version, data.toString());
    }
}
