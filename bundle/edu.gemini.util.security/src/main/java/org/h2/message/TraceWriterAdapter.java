/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.message;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This adapter sends log output to SLF4J. SLF4J supports multiple
 * implementations such as Logback, Log4j, Jakarta Commons Logging (JCL), JDK
 * 1.4 logging, x4juli, and Simple Log. To use SLF4J, you need to add the
 * required jar files to the classpath, and set the trace level to 4 when opening
 * a database:
 *
 * <pre>
 * jdbc:h2:&tilde;/test;TRACE_LEVEL_FILE=4
 * </pre>
 *
 * The logger name is 'h2database'.
 */
public class TraceWriterAdapter implements TraceWriter {

    private String name;
    private Logger logger = Logger.getLogger(TraceWriterAdapter.class.getName());

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled(int level) {
        switch (level) {
        case TraceSystem.DEBUG:
            return logger.isLoggable(Level.FINE);
        case TraceSystem.INFO:
            return logger.isLoggable(Level.INFO);
        case TraceSystem.ERROR:
            return logger.isLoggable(Level.SEVERE);
        default:
            return false;
        }
    }

    public void write(int level, String module, String s, Throwable t) {
        if (isEnabled(level)) {
            if (name != null) {
                s = name + ":" + module + " " + s;
            } else {
                s = module + " " + s;
            }
            switch (level) {
            case TraceSystem.DEBUG:
                logger.fine(s + " " + t);
                break;
            case TraceSystem.INFO:
                logger.info(s + " " + t);
                break;
            case TraceSystem.ERROR:
                logger.severe(s + " " + t);
                break;
            default:
            }
        }
    }

}
