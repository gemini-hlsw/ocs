package edu.gemini.spModel.gemini.calunit.smartgcal.maps;


import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.type.DisplayableSpType;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for all calibration maps.
 */
public abstract class BaseCalibrationMap implements CalibrationMap {

    private final Version version;

    public BaseCalibrationMap(Version version) {
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    static protected <T extends Enum<T>&DisplayableSpType> Set<T> getValues(Class<T> c, Properties properties, ConfigurationKey.Values name) {
        String valueString = getValue(properties, name);
        Set<T> results = new HashSet<>();

        T[] constants = c.getEnumConstants();

        // regular expressions
        if (valueString.startsWith("$")) {
            String s = valueString.substring(1, valueString.length());
            for (T t : constants) {
                String curDisplayValue = t.displayValue();
                if (curDisplayValue.matches(s)) {
                    results.add(t);
                }
            }

        // wildcard search (ignore case)
        } else if (valueString.endsWith("*")) {
            String s = valueString.substring(0, valueString.length()-1).toLowerCase();
            for (T t : constants) {
                String curDisplayValue = t.displayValue();
                if (curDisplayValue.toLowerCase().startsWith(s)) {
                    results.add(t);
                }
            }

        // exact search (ignore case)
        } else {
            for (T t : constants) {
                String curDisplayValue = t.displayValue();
                if (valueString.equalsIgnoreCase(curDisplayValue)) {
                    results.add(t);
                    break;
                }
            }
        }

        // check if at least one value was found
        if (results.isEmpty()) {
            throw new IllegalArgumentException("illegal value for '" + name + "': '" + valueString +"'");
        }

        return results;
    }

    static private String getValue(Properties properties, ConfigurationKey.Values name) {
        String valueString = properties.getProperty(name.toString());
        if (valueString == null) {
            throw new IllegalArgumentException("value for '" + name + "' is missing");
        }
        return valueString;
    }

    @Override
    public Calibration createCalibration(Properties properties) {
        return CalibrationImpl.parse(properties);
    }

    @Override
    public ConfigurationKey.Values[] getCalibrationValueNames() {
        return CalibrationImpl.Values.values();
    }

    @Override
    public Stream<ImList<String>> export() {
        return Stream.empty();
    }
}
