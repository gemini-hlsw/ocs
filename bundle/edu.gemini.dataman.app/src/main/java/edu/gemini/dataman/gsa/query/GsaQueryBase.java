//
// $
//

package edu.gemini.dataman.gsa.query;

/**
 * Base class for queries that contact the GSA for information.  Holds common
 * information needed by subclasses.
 */
public abstract class GsaQueryBase {

    public static int DEFAULT_CONNECT_TIMEOUT =  30000; // 30 seconds
    public static int DEFAULT_READ_TIMEOUT    = 120000; //  2 minutes

    private final int conntectTimeout;
    private final int readTimeout;

    protected GsaQueryBase() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected GsaQueryBase(int connectTimeout, int readTimeout) {
        this.conntectTimeout = connectTimeout;
        this.readTimeout     = readTimeout;
    }

    public final int getConnectTimeout() {
        return conntectTimeout;
    }

    public final int getReadTimeout() {
        return readTimeout;
    }
}