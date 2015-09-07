//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.dataman.context.GsaUrl;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obtains the transfer status of a named file or files, assuming they are in
 * the GSA e-transfer system.
 */
public final class SingleXferQuery extends XferQueryBase {
    private static final Logger LOG = Logger.getLogger(CrcQuery.class.getName());
    private static final Level LEVEL = Level.INFO;

    private GsaUrl gsaUrl;

    public SingleXferQuery(DatamanConfig config) {
        this(config.getGsaXferStatusUrl());
    }

    public SingleXferQuery(GsaUrl url) {
        gsaUrl = url;
    }

    public SingleXferQuery(DatamanConfig config, int connectTimeout, int readTimeout) {
        this(config.getGsaXferStatusUrl(), connectTimeout, readTimeout);
    }

    public SingleXferQuery(GsaUrl url, int connectTimeout, int readTimeout) {
        super(connectTimeout, readTimeout);
        gsaUrl = url;
    }


    /**
     * Obtains the e-transfer status for the given file.
     *
     * @param filename name of the file whose e-transfer status is sought
     *
     * @return e-transfer status for a file
     *
     * @throws java.io.IOException if there is a problem contacting the GSA
     * machine
     */
    public Status getStatus(String filename) throws IOException {

        long startTime = System.currentTimeMillis();

        StringBuilder msg = new StringBuilder();
        msg.append("> XferQuery(").append(filename).append(") ");

        URL url = gsaUrl.toURL(filename);

        Status status = null;
        BufferedReader br = null;
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(getConnectTimeout());
            con.setReadTimeout(getReadTimeout());
            InputStream is = con.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            String line = br.readLine();

            // Log the time the query took.
            long queryTime = System.currentTimeMillis() - startTime;
            msg.append(queryTime).append(" ms: ").append(line).append("\n");

            // Parse the result if we can.
            if (line == null) {
                status = new Status(filename, 0, Status.Code.unknown, "No information returned from e-transfer query");
            } else {
                status = parseStatusLine(filename, line);
            }

        } catch (FileNotFoundException ex) {
            // thrown when an HTTP 404 status is returned -- which happens when
            // the requested file isn't in the process of being transfered.

            // Log the time the query took.
            long queryTime = System.currentTimeMillis() - startTime;
            msg.append(queryTime).append(" ms:\n");

            status = Status.createNotFound(filename);

        } finally {
            if (status != null) {
                msg.append("\t").append(status.toString());
            }
            LOG.log(LEVEL, msg.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (IOException iex) {
                    LOG.log(Level.WARNING, iex.getMessage(), iex);
                }
            }
        }

        return status;
    }

    // for simple testing
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("specify one or more GS filenames");
            System.exit(-1);
        }

        GsaUrl url = new GsaUrl("http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/etransferState?source=gsags&format=text&file=%FILE%");
        SingleXferQuery query = new SingleXferQuery(url);

        for (String filename : args) {
            try {
                System.out.println(query.getStatus(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
