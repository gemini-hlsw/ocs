//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.GsaUrl;
import edu.gemini.dataman.context.DatamanConfig;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obtains the CRC of the version of a file that is stored in the GSA (if any).
 */
public final class CrcQuery extends GsaQueryBase {
    private static final Logger LOG = Logger.getLogger(CrcQuery.class.getName());

    private final GsaUrl gsaUrl;

    public CrcQuery(GsaUrl url) {
        gsaUrl = url;
    }

    public CrcQuery(GsaUrl url, int connectTimeout, int readTimeout) {
        super(connectTimeout, readTimeout);
        gsaUrl = url;
    }

    public CrcQuery(DatamanConfig config) {
        gsaUrl = config.getGsaCrcUrl();
    }

    public CrcQuery(DatamanConfig config, int connectTimeout, int readTimeout) {
        super(connectTimeout, readTimeout);
        gsaUrl = config.getGsaCrcUrl();
    }

    /**
     * Obtains the CRC for the version of the file that is stored in the GSA
     * (if any).
     *
     * @param filename name of the file whose CRC is sought
     *
     * @return CRC for an accepted file; <code>null</code> if the file doesn't
     * exist in the GSA
     *
     * @throws IOException if there is a problem contacting the GSA machine
     */
    public Long getCrc(String filename) throws IOException {

        long startTime = System.currentTimeMillis();

        StringBuilder msg = new StringBuilder();
        msg.append("> CrcQuery(").append(filename).append(") ");

        URL url = gsaUrl.toURL(filename);

        BufferedReader br = null;
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(getConnectTimeout());
            con.setReadTimeout(getReadTimeout());
            InputStream is = con.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            String res = br.readLine();

            // Log the time the query took.
            long queryTime = System.currentTimeMillis() - startTime;
            msg.append(queryTime).append(" ms: ").append(res);

            if ((res == null) || !res.startsWith("0x")) return null;
            String hex = res.substring(2).toLowerCase();
            return Long.parseLong(hex, 16);

        } catch (FileNotFoundException ex) {
            msg.append(" not found");
            return null;

        } finally {
            LOG.log(Level.INFO, msg.toString());
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

        GsaUrl url = new GsaUrl("http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/geminiInfo?file=%FILE%&options=-ufilecrc");
        CrcQuery query = new CrcQuery(url);

        for (String filename : args) {
            Long l = null;
            try {
                l = query.getCrc(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (l == null) {
                System.out.println(filename + ": not found");
            } else {
                System.out.println(String.format("%s: %d (%x)", filename, l, l));
            }
        }
    }

}
