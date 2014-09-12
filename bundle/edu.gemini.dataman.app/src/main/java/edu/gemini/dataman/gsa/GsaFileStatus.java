package edu.gemini.dataman.gsa;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.dataman.gsa.query.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A type that represents the status of a file from the perspective
 * of the GSA.  This class uses and coordinates the various queries in the
 * <code>edu.gemini.dataman.gsa.query</code> package.  Unfortunately to gather
 * the complete picture on the status of a file, we have to perform three
 * queries.
 *
 * <p>This class is closely related to the
 * {@link edu.gemini.spModel.dataset.GsaState}, but the GsaState embodies
 * more than just the file status.  It also includes copying the file down
 * to the GSA machine and verifying it.  A dataset can have a GsaState even
 * before it is ever sent to the GSA.  It only has a GsaFileStatus when it is
 * either in the GSA or in the process of being transferred by their software.
 *
 * <p>The GsaFileStatus reports on the most current version of the file known
 * to the GSA.
 */
public final class GsaFileStatus {
    private static final Logger LOG = Logger.getLogger(GsaFileStatus.class.getName());

    /**
     * The state of the file in the GSA.
     */
    public enum State {
        /** The GSA doesn't know anything about this file. */
        notFound(true),

        /**
         * Could not determine the state of the file due to a problem with
         * the GSA service.
         */
        unknown(true),

        /** File is queued awaiting processing. */
        queued(false),

        /** File is being processed by the GSA. */
        processing(false),

        /** File has been rejected by the GSA. */
        rejected(true),

        /** The file has been accepted in the GSA. */
        accepted(true),
        ;

        private boolean terminal;

        State(boolean terminal) {
            this.terminal = terminal;
        }

        public boolean isTerminal() {
            return terminal;
        }
    }

    private final String filename;
    private final State state;
    private final String info;
    private final Long crc;

    public GsaFileStatus(String filename, State state) {
        this(filename, state, null, null);
    }

    public GsaFileStatus(String filename, State state, String info, Long crc) {
        this.filename = filename;
        this.state = state;
        this.info     = info;
        this.crc      = crc;
    }

    public String getFilename() {
        return filename;
    }

    public State getState() {
        return state;
    }

    public String getAdditionalInformation() {
        return info;
    }

    public Long getCrc() {
        return crc;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GsaFileStatus that = (GsaFileStatus) o;

        if (crc != null ? !crc.equals(that.crc) : that.crc != null) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (info != null ? !info.equals(that.info) : that.info != null) return false;
        return state == that.state;

    }

    public int hashCode() {
        int result;
        result = (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        result = 31 * result + (crc != null ? crc.hashCode() : 0);
        return result;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(String.format("%s %s", filename, state));
        if (info != null) {
            buf.append(" (").append(info).append(")");
        }
        if (crc != null) {
            buf.append(" crc=").append(crc);
        }
        return buf.toString();
    }


    /**
     * Determines the current status of the given file from the point of view
     * of the GSA.  Manages the algorithm for making the various queries
     * necessary to determine this status.
     *
     * @param config context of the dataman application, from which configuration
     * information is extracted to make the queries
     *
     * @param filename name of the file whose status should be sought
     *
     * @return the current status of the file from the point of view of the GSA
     *
     * @throws GsaFileStatusException if there is a problem making the
     * determination of the file status; in this case, the status should be
     * considered unknown
     */
    public static GsaFileStatus query(DatamanConfig config, String filename) {
        try {
            // Check the e-transfer system and see if it knows about the file.
            SingleXferQuery xq = new SingleXferQuery(config);
            SingleXferQuery.Status xferStatus = xq.getStatus(filename);
            GsaFileStatus res = translate(filename, xferStatus);
            if (res != null) return res;

            // Okay, the file isn't active in the e-transfer system, so we need
            // check whether it has been accepted.
            CrcQuery cq = new CrcQuery(config);
            Long crc = cq.getCrc(filename);
            if (crc == null) {
                // Wasn't stored in the GSA, so apparently the GSA doesn't know
                // anything about it.
                return new GsaFileStatus(filename, State.notFound);
            }

            // Found in the GSA with the given CRC.
            return new GsaFileStatus(filename, State.accepted, null, crc);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem communicating with the GSA e-transfer system", ex);
            return new GsaFileStatus(filename, State.unknown);
        }
    }

    //
    // Translates between the GSA e-transfer state and a more simplified
    // representation of the state in the GsaFileStatus.
    private static GsaFileStatus translate(String filename, XferQueryBase.Status status) {
        String info;
        if (status.getTime() <= 0) {
            info = status.getInfo();
        } else {
            info = (new Date(status.getTime())).toString();
            if (status.getInfo() != null) {
                info += ": " + status.getInfo();
            }
        }

        switch (status.getCode()) {
            case notFound:
            case success:
                return null;

            case unknown:
                return new GsaFileStatus(filename, State.unknown, info, null);

            case pickup:
                return new GsaFileStatus(filename, State.queued);

            case preprocessing:
            case postprocessing:
            case transferring:
            case transferred:
                return new GsaFileStatus(filename, State.processing, info, null);

            case rejected:
            case errored:
            case failed:
                return new GsaFileStatus(filename, State.rejected, info, null);

            default:
                throw new RuntimeException("Unexpected e-transfer status: " +
                                                              status.getCode());
        }
    }

    /**
     * Determines the file status for the given collection of files from the
     * point of view of the GSA.
     *
     * @param config configuration of the dataman application
     *
     * @param filenames collection of file names for which the status is sought
     *
     * @return map of filename to current status from the perspective of the GSA
     *
     * @throws GsaFileStatusException if there is a problem making the
     * determination of the file status
     */
    public static Map<String, GsaFileStatus> query(
                                  DatamanConfig config, Set<String> filenames) {

        Map<String, GsaFileStatus> res;
        res = new TreeMap<String, GsaFileStatus>();

        // First process all the files that are in the e-transfer process.
        MultiXferQuery mxq = new MultiXferQuery(config);
        Map<String, XferQueryBase.Status> xferStatusMap;
        try {
            xferStatusMap = mxq.getStatus(filenames);
        } catch (IOException ex) {
            // Couldn't do the xfer status query, so we don't know anything
            // about these files.
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            for (String filename : filenames) {
                res.put(filename, new GsaFileStatus(filename, State.unknown));
            }
            return res;
        }

        // For each query result, translate the status to a GsaFileState, or
        // add the filename to the collection of files that weren't found.
        for (Map.Entry<String, XferQueryBase.Status> me : xferStatusMap.entrySet()) {
            String filename = me.getKey();
            XferQueryBase.Status xferStatus = me.getValue();

            GsaFileStatus fileStatus = translate(filename, xferStatus);
            if (fileStatus != null) res.put(filename, fileStatus);
        }

        // Get the set of the files that aren't active in e-transfer.
        // If we have a result for all the files then, we're done.
        Set<String> notFound = new HashSet<String>(filenames);
        notFound.removeAll(res.keySet());

        // Figure out the CRC of all files that aren't in the e-transfer system.
        MultiCrcQuery mcq = new MultiCrcQuery(config);
        Map<String, Long> crcMap;
        try {
            crcMap = mcq.lookup(notFound, 300, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // Log and go on.  Later all of these files will be given the
            // status "unknown".
            LOG.log(Level.WARNING, "interrupted while waiting for a response from the CRC queries", ex);
            crcMap = Collections.emptyMap();
        } finally {
            mcq.stop();
        }

        for (Map.Entry<String, Long> me : crcMap.entrySet()) {
            String filename = me.getKey();
            Long crc = me.getValue();
            if (crc == null) {
                // not accepted, and not in the e-transfer system
                res.put(filename, new GsaFileStatus(filename, State.notFound));
            } else {
                // accepted and with the given CRC
                res.put(filename, new GsaFileStatus(filename, State.accepted, null, crc));
            }
        }

        // Now, all files should be accounted for at this point.  If any don't
        // appear in the result set, then there was a problem determining the
        // status.
        notFound.removeAll(res.keySet());

        for (String filename : notFound) {
            res.put(filename, new GsaFileStatus(filename, State.unknown));
        }

        return res;
    }
}
