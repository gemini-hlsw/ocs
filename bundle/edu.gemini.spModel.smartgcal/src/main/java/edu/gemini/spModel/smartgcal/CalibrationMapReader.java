package edu.gemini.spModel.smartgcal;

import au.com.bytecode.opencsv.CSVReader;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class CalibrationMapReader {

    private static final Logger LOG = Logger.getLogger(CalibrationMapReader.class.getName());

    private CalibrationMap map;
    private List<String> errors;
    private int line;

    public CalibrationMapReader(CalibrationMap map) {
        this.map = map;
        this.errors = new ArrayList<String>();
        this.line = 0;
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

    public List<String> getErrors() {
        return errors;
    }

    public CalibrationMap getMap() {
        return map;
    }

    public void read(byte[] data) {
        read(new ByteArrayInputStream(data));
    }

    public CalibrationMap read(InputStream is) {

        try {
            createFromData(is);

        // -- parse exception that was not catched below is supposed to stop parsing e.g. because header is invalid
        } catch (ParseException e) {
            LOG.log(Level.FINE, e.getMessage());
        // -- io exception, not much we can do about...
        }  catch (IOException e) {
            LOG.log(Level.SEVERE, "io exception while reading input", e);
            addError("io exception while reading from input stream " + e.getMessage());
        }

        return map;
    }

    public static List<String> validateData(String instrument, CalibrationFile file) {
         CalibrationMap map = CalibrationMapFactory.createEmpty(instrument, file.getVersion());
         CalibrationMapReader reader = new CalibrationMapReader(map);
         reader.read(file.getData().getBytes());
         return reader.getErrors();
     }


    private void createFromData(InputStream is) throws IOException, ParseException
    {

        CSVReader csvReader = new CSVReader(new InputStreamReader(is));

        List<String> header = null;
        for (String[] values : csvReader.readAll()) {
            line++;

            // skip empty lines and comments
            if (isEmptyLine(values)) {
                continue;
            }

            // first line with data MUST be the header (column names)
            if (header == null) {
                header = parseHeader(values);
                continue;
            }

            try {
                // translate this line
                Properties properties = createProperties(header, values);
                Calibration calibration = map.createCalibration(properties);
                Set<ConfigurationKey> keys = map.createConfig(properties);
                // insert all keys and the calibration
                for (ConfigurationKey key : keys) {
                    map.put(key, properties, calibration);
                }

            } catch (Exception e) {
                addError(e.getMessage());
            }
        }

        csvReader.close();

    }

    private boolean isEmptyLine(String[] values) {
        // skip comments
        if (values[0].trim().startsWith("#")) {
            return true;
        }
        // skip lines with only commas but no values
        for (String value : values) {
            if (!value.trim().isEmpty()) {
                // there is data, this is not an empty line
                return false;
            }
        }
        // no values detected, empty
        return true;
    }

    private List<String> parseHeader(String[] values) throws ParseException {
        List<String> header = new LinkedList<String>();
        for (int i = 0; i < values.length; i++) {
            String v = values[i].trim();
            // check for duplicate column names (ignore "empty" columns)
            if (!v.isEmpty() && header.contains(v)) {
                addError("duplicate column name '" + v + "'");
            }
            header.add(v);
        }

        // check that header has all necessary columns for key
        for (ConfigurationKey.Values v : map.getKeyValueNames()) {
            if (!header.contains(v.toString())) {
                addError("missing mandatory key column '" + v.toString() + "'");
            }
        }

        // check that header has all necessary columns for calibration
        for (ConfigurationKey.Values v : map.getCalibrationValueNames()) {
            if (!header.contains(v.toString())) {
                addError("missing mandatory calibration column '" + v.toString() + "'");
            }
        }

        if (hasErrors()) {
            addErrorAndStop("header is invalid, stop parsing");
        }

        // -- ok
        return header;
    }

    private Properties createProperties(List<String> header, String[] values) throws ParseException {
        // check if line is valid
        if (values.length < header.size()) {
            addErrorAndStop("too few values on this line");
        }
        if (values.length > header.size()) {
            addErrorAndStop("too many values on this line");
        }

        // create lookup map
        Properties properties = new Properties();
        int i = 0;
        for (String column : header) {
            properties.setProperty(column, values[i++].trim());
        }
        return properties;
    }

    private void addError(String error) {
        errors.add("line " + line + ": " + error);
    }

    private void addErrorAndStop(String error) throws ParseException {
        addError(error);
        throw new ParseException(error, line);
    }

}
