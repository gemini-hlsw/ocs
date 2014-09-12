package edu.gemini.osgi.main;

import java.io.*;
import java.util.Properties;
import java.util.logging.LogManager;

/**
 * Initializes logging assuming neither traditional way of setting up logging
 * is specified (i.e., neither java.util.logging.config.class nor
 * java.util.logging.config.file properties are specified).
 *
 * <p>The goal is to just specify a "logging.properties" file in the conf
 * directory of the application and not worry about setting properties to find
 * it or creating log directories on installation.  Java logging does not work
 * if the log directory specified in logging.properties does not exist.</p>
 *
 * <p>Finds the "logging.properties" file for the application, if any, and loads
 * it to extract the "java.util.logging.FileHandler.pattern" which it uses to
 * first create the log directory if possible and then setup the root logger
 * programatically as if java.util.logging.config.file had been set.</p>
 *
 * <p>Respects the following components in the file pattern
 * <ul>
 *     <li>%a - application directory (home of bundle storage root and external storage)</li>
 *     <li>%h - user home directory</li>
 *     <li>%t - system temp directory</li>
 * </ul>
 */
final class LogInitializer {
    private static final String LOGFILE_PATTERN_PROP = "java.util.logging.FileHandler.pattern";
    private static final String LOGGING_CLASS_PROP   = "java.util.logging.config.class";
    private static final String LOGGING_FILE_PROP    = "java.util.logging.config.file";

    private LogInitializer() {}

    static void initializeLogging(AppRoot root) throws Exception {
        // Check explicit logging configuration and respect it if set.
        if (System.getProperty(LOGGING_CLASS_PROP) != null) return;
        if (System.getProperty(LOGGING_FILE_PROP) != null) return;

        // Load the logging.properties file, if found.
        final Properties props  = propsWithExpandedLogfilePattern(root, loadLoggingProperties());
        if (props.isEmpty()) return;

        // Create the log directory if necessary.
        mkLogDir(props);

        // Write the properties out to a String.
        final StringWriter sw = new StringWriter();
        props.store(sw, null);
        sw.close();

        // Load the properties String into the LogManager.
        final byte[] propsString = sw.toString().getBytes("UTF-8");
        final LogManager logManager = LogManager.getLogManager();
        logManager.readConfiguration(new ByteArrayInputStream(propsString));
    }

    private static void mkLogDir(Properties props) throws Exception {
        String pat = props.getProperty(LOGFILE_PATTERN_PROP);
        if (pat == null) return;

        String[] dirs = pat.split("/");
        if (dirs.length <= 1) return; // no directory specified, using cwd

        File logDir = new File(dirs[0]);
        for (int i=1; i<(dirs.length-1); ++i) {
            logDir = new File(logDir, dirs[i]);
        }
        logDir.mkdirs();
        if (!logDir.isDirectory()) throw new RuntimeException("Could not create log directory: " + logDir);
    }

    private static Properties propsWithExpandedLogfilePattern(AppRoot root, Properties props) throws Exception {
        final String pat = expandLogfilePattern(root, props);
        if (pat == null) return props;

        final Properties props0 = new Properties();
        for (String name : props.stringPropertyNames()) {
            props0.setProperty(name, props.getProperty(name));
        }
        props0.setProperty(LOGFILE_PATTERN_PROP, pat);
        return props0;
    }

    private static String expandLogfilePattern(AppRoot root, Properties props) throws Exception {
        String pat = props.getProperty(LOGFILE_PATTERN_PROP);
        if (pat == null) return null;

        pat = pat.trim();
        if (pat.startsWith("%a")) {
            pat = pat.replace("%a", root.dir.getAbsolutePath());
        } else if (pat.startsWith("%h")) {
            pat = pat.replace("%h", System.getProperty("user.home"));
        } else if (pat.startsWith("%t")) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (tmpDir == null) {
                // hey, this is what FileHandler does :/
                tmpDir = System.getProperty("user.home");
            }
            pat = pat.replace("%t", tmpDir);
        }

        return pat;
    }

    // Load the application "logging.properties" if found.
    private static Properties loadLoggingProperties() throws Exception {
        final Properties loggingProperties = new Properties();

        // Check whether there is a logging properties file.
        final File loggingPropertiesFile = getLoggingProperties();
        if (loggingPropertiesFile == null) return loggingProperties;

        final Reader r = new FileReader(loggingPropertiesFile);
        try {
            loggingProperties.load(r);
        } finally {
            r.close();
        }
        return loggingProperties;
    }

    // Get the first "logging.properties" file found in a sub directory of the
    // installation directory.
    private static File getLoggingProperties() {
        final File insDir  = getInstallDir();
        if (insDir == null) return null;

        final File[] subDirs = insDir.listFiles(new FileFilter() {
            @Override public boolean accept(File path) {
                return path.isDirectory() && path.canRead();
            }
        });
        if (subDirs == null) return null;

        for (File subDir : subDirs) {
            final File loggingProperties = new File(subDir, "logging.properties");
            if (loggingProperties.exists() && loggingProperties.isFile() && loggingProperties.canRead()) {
                return loggingProperties;
            }
        }
        return null;
    }

    // Get the install dir based upon the location of the main jar in the
    // classpath.
    private static File getInstallDir() {
        final String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String s : classPath) {
            if (s.contains("edu-gemini-osgi-main_")) {
                return new File(s).getAbsoluteFile().getParentFile();
            }
        }
        return null;
    }
}
