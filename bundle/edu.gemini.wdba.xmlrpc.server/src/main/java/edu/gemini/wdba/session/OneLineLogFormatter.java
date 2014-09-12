/*
 *
 */
package edu.gemini.wdba.session;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Taken from Java Source.  This is a rewrite to ensure one line that looks good for the WDBA event log
 * The output is like this:
 * INFO: short date, short time: short class name.method: message
 */
public class OneLineLogFormatter extends Formatter {
    private Date _dat = new Date();

    // This method uses the contained exception to get a stack trace 
    private String _getException(LogRecord record) {
        StringWriter sw = new StringWriter();
        try {
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
        } catch (Exception ex) {
        }
        return sw.toString();
    }

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();

        // Level message
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");

        // Time and date
        // Minimize memory allocations here.
        _dat.setTime(record.getMillis());

        // Class name.method name
        StringWriter classInfo = new StringWriter();
        if (record.getSourceClassName() != null) {
            // Only use the class name
            String className = record.getSourceClassName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            classInfo.append(className);
        } else {
            classInfo.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            classInfo.append(">");
            classInfo.append(record.getSourceMethodName());
        }

        String result = String.format("%7s: %tD %tr %s: %s%n", record.getLevel().getLocalizedName(), _dat, _dat, classInfo.toString(), formatMessage(record));
        return record.getThrown() == null ? result : result + _getException(record);
    }
}
