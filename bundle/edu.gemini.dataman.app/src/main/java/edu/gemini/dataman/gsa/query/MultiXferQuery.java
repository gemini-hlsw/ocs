//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.DatamanConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obtains the transfer status of a named file or files, assuming they are in
 * the GSA e-transfer system.
 */
public final class MultiXferQuery extends XferQueryBase {
    private static final Logger LOG = Logger.getLogger(CrcQuery.class.getName());
    private static final Level LEVEL = Level.INFO;

    private URL _allXferStatusUrl;

    public MultiXferQuery(DatamanConfig config) {
        this(config.getGsaAllXferStatusUrl());
    }

    public MultiXferQuery(URL allFileUrl) {
        _allXferStatusUrl = allFileUrl;
    }

    public MultiXferQuery(DatamanConfig config, int connectTimeout, int readTimeout) {
        this(config.getGsaAllXferStatusUrl(), connectTimeout, readTimeout);
    }

    public MultiXferQuery(URL allFileUrl, int connectTimeout, int readTimeout) {
        super(connectTimeout, readTimeout);
        _allXferStatusUrl = allFileUrl;
    }


    /**
     * Obtains the e-transfer status for the given files.
     *
     * @param filenames set of FITS files whose e-transfer status is sought
     *
     * @return map of e-transfer status for each file file
     *
     * @throws java.io.IOException if there is a problem contacting the GSA
     * machine
     */
    public Map<String, Status> getStatus(Set<String> filenames) throws IOException {

        // Assume that none of the files are active in the e-transfer system
        Map<String, Status> res = new TreeMap<String, Status>();
        for (String filename : filenames) {
            res.put(filename, Status.createNotFound(filename));
        }


        // Remember when we started the query, for logging the time it takes.
        long startTime = System.currentTimeMillis();

        // Build up a log message that we will display at the end.
        StringBuilder msg = new StringBuilder();
        msg.append("> XferQuery(").append(filenames.toString()).append(")\n");

        BufferedReader br = null;
        try {
            // Perform the query
            URLConnection con = _allXferStatusUrl.openConnection();
            con.setConnectTimeout(getConnectTimeout());
            con.setReadTimeout(getReadTimeout());
            InputStream is = con.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            // Parse the results into the "res" map.
            String line;
            while ((line = br.readLine()) != null) {
                String filename = parseFilename(line);
                if (filename == null) {
                    // this line refers to a non-fits file or is empty or
                    // something unexpected
                    continue;
                }

                if (!filenames.contains(filename)) {
                    // we aren't interested in this file's status
                    continue;
                }

                // Parse the status
                Status status = parseStatusLine(filename, line);
                res.put(filename, status);
            }

            // Log the status of all the files.
            if (LOG.isLoggable(LEVEL)) {
                for (Map.Entry<String, Status> me : res.entrySet()) {
                    msg.append("\t").append(me.getValue()).append("\n");
                }
            }

            // Log the time the query took.
            long queryTime = System.currentTimeMillis() - startTime;
            msg.append("\tQuery Time: ").append(queryTime).append(" ms\n");

            return res;

        } finally {
            LOG.log(LEVEL, msg.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (IOException iex) {
                    LOG.log(Level.WARNING, iex.getMessage(), iex);
                }
            }
        }
    }

    // for simple testing
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("specify one or more GS filenames");
            System.exit(-1);
        }

        try {
            URL url = new URL("http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsagn&format=text");
            MultiXferQuery query = new MultiXferQuery(url);
            query.getStatus(new HashSet<String>(Arrays.asList(args)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
