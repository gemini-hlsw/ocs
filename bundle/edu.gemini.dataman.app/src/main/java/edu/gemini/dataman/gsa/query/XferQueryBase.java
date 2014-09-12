//
// $
//

package edu.gemini.dataman.gsa.query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtains the transfer status of a named file or files, assuming they are in
 * the GSA e-transfer system.
 */
public abstract class XferQueryBase extends GsaQueryBase {
    private static final Logger LOG = Logger.getLogger(CrcQuery.class.getName());
    private static final Level LEVEL = Level.INFO;

    /**
     * The result of the transfer status query.
     */
    public static final class Status {
        public static enum Code {
            notFound,               // not in GSA e-transfer system
            unknown,                // bug in the query results
            pickup(false),          // in the queueing directory
            preprocessing(false),   // in the process of being transferred
            transferring(false),    // dito
            transferred(false),     // dito
            postprocessing(false),  // dito
            rejected,               // file rejected by the GSA
            failed,                 // unknown problem
            errored,                // unknown problem
            success,                // very recently successfully transferred
            ;

            private boolean isTerminal = true;

            Code() {
            }

            Code(boolean isTerminal) {
                this.isTerminal = isTerminal;
            }

            public boolean isTerminal() {
                return isTerminal;
            }
        }

        public static Status createNotFound(String filename) {
            return new Status(filename, 0, Code.notFound, null);
        }

        public static Status createUnknown(String filename) {
            return new Status(filename, 0, Code.unknown, null);
        }

        private final String filename;
        private final long time;
        private final Code code;
        private final String info;

        public Status(String filename, long time, Code code, String info) {
            this.filename = filename;
            this.time = time;
            this.code = code;
            this.info = info;
        }

        public String getFilename() {
            return filename;
        }

        public long getTime() {
            return time;
        }

        public String getCadcFormattedTime() {
            return formatTime(time);
        }

        public Code getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Status that = (Status) o;

            if (time != that.time) return false;
            if (code != that.code) return false;
            if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
            return !(info != null ? !info.equals(that.info) : that.info != null);
        }

        public int hashCode() {
            int result;
            result = (filename != null ? filename.hashCode() : 0);
            result = 31 * result + (int) (time ^ (time >>> 32));
            result = 31 * result + (code != null ? code.hashCode() : 0);
            result = 31 * result + (info != null ? info.hashCode() : 0);
            return result;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();

            buf.append(filename).append("\t");
            if (time != 0) {
                buf.append(new Date(time));
            } else {
                buf.append("-");
            }
            buf.append("\t");

            buf.append(code);

            if (info != null) {
                buf.append("\t").append(info);
            }

            return buf.toString();
        }
    }

    protected XferQueryBase() {
    }

    protected XferQueryBase(int connectTimeout, int readTimeout) {
        super(connectTimeout, readTimeout);
    }


    // Parsing time returned by CADC
    private static final SimpleDateFormat CADC_FORMAT = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy");

    static {
        CADC_FORMAT.setTimeZone(SimpleTimeZone.getTimeZone("UTC"));
    }

    private synchronized static long parseTime(String cadcTime) {
        try {
            return CADC_FORMAT.parse(cadcTime).getTime();
        } catch (ParseException ex) {
            LOG.warning("Could not parse time returned by e-transfer query: "+ cadcTime);
            return 0;
        }
    }

    private synchronized static String formatTime(long time) {
        return CADC_FORMAT.format(new Date(time));
    }

//    private void fail(String msg) throws IOException {
//        LOG.warning(msg);
//        throw new IOException(msg);
//    }

    private static Pattern FITS_LINE_PATTERN = Pattern.compile("^([NS]\\d\\d\\d\\d\\d\\d\\d\\dS\\d+\\.fits).*");

    protected static String parseFilename(String line) {
        Matcher m = FITS_LINE_PATTERN.matcher(line);
        return m.matches() ? m.group(1) : null;
    }

    protected static Status parseStatusLine(String filename, String line) {
        String[] res = line.split("\\t");
        if ((res.length < 3) || (res.length > 4)) {
            return new Status(filename, 0, Status.Code.unknown, "unexpected query result");
        }

        // The results seem to come with trailing spaces ... so trim those off.
        for (int i=0; i<res.length; ++i) {
            res[i] = res[i].trim();
        }

//        if (!res[0].contains(".fits")) {
//            msg.append("not a fits file\n");
//            return null;
//        }
//
//        String filename = res[0];
//        if (!filename.endsWith(".fits")) {
//            int index = filename.lastIndexOf(".fits");
//            filename  = filename.substring(0, index + 5);
//        }

        long time = parseTime(res[1]);

        String info = null;
        if (res.length == 4) info = res[3];

        Status status;

        String codeStr = res[2];
        Status.Code code;
        try {
            code = Status.Code.valueOf(codeStr);
        } catch (Exception ex) {
            info = String.format("unknown e-transfer return code '%s'", codeStr);
            code = Status.Code.unknown;
        }

        status = new Status(filename, time, code, info);
        return status;
    }
}