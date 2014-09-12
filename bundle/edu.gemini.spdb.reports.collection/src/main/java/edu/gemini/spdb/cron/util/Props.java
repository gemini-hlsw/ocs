package edu.gemini.spdb.cron.util;

import java.io.File;
import java.util.Map;
import java.util.NoSuchElementException;

/** Base class that can do sane things with property maps. */
@SuppressWarnings({"UnusedDeclaration", "RedundantCast" /* not really */})
public class Props {

    private final Map<String, String> props;

    public Props(final Map<String, String> props) {
        this.props = props;
    }

    public String getString(final String key, final String defaultValue) {
        return props.containsKey(key) ? props.get(key) : defaultValue;
    }

    public String getString(final String key) {
        return props.containsKey(key) ? props.get(key) : ((String) fail(key));
    }

    public int getInt(final String key, final int defaultValue) {
        try {
            return props.containsKey(key) ? Integer.parseInt(props.get(key)) : defaultValue;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Expected int for property: " + key, nfe);
        }
    }

    public int getInt(final String key) {
        try {
            return props.containsKey(key) ? Integer.parseInt(props.get(key)) : ((Integer) fail(key));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Expected int for property: " + key, nfe);
        }
    }

    public File getFile(final String key) {
        return new File(getString(key));
    }

    public File getFile(final String key, final String defaultValue) {
        return new File(getString(key, defaultValue));
    }

    //////

    private static <T> T fail(final String key) {
        throw new NoSuchElementException("Missing property: " + key);
    }

}
