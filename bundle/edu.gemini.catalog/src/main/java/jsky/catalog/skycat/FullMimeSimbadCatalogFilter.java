package jsky.catalog.skycat;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter is used to parse the output of name server catalogs when the mimetype=full-rec
 * option is used.
 */
public class FullMimeSimbadCatalogFilter implements ICatalogFilter {

    private static ICatalogFilter filter;

    /**
     * Get the singleton of this Catalog Filter
     * @return the unique instance of this FullMimeSimbadCatalogFilter.
     */

    public static ICatalogFilter getFilter() {
        if (filter == null) {
            filter = new FullMimeSimbadCatalogFilter();
        }
        return filter;
    }

    private FullMimeSimbadCatalogFilter() {
    }

    public InputStream filterContent(InputStream is) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer sb = new StringBuffer();
        boolean isFullContent = false;
        boolean firstLine = true;
        while (true) {
            line = reader.readLine();

            if (line == null)
                break;

            // check for error message starting with "***" - a convention of skycat servers
            if (line.startsWith("***")) {
                throw new RuntimeException(line.substring(3));
            }

            if (line.length() == 0 || line.charAt(0) == '#')
                continue;    // empty line or comment

            if (firstLine && line.startsWith("Info")) {
                isFullContent = true;
            }
            firstLine = false;

            sb.append(line);
            sb.append("\n");
        }

        if (isFullContent) {
            sb = _parseContent(sb);
        }
        return new ByteArrayInputStream(sb.toString().getBytes());

    }

    private StringBuffer _parseContent(StringBuffer buffer) {
        String b = buffer.toString();
        final String LINE = "--------";
        StringBuffer sb = new StringBuffer("Id\tra\tdec\tpm1\tpm2\n");
        sb.append(LINE);
        sb.append("\n");
        Pattern p = Pattern.compile("Id's:\\s*?(\\w+?.*?$)", Pattern.MULTILINE);
        Matcher m = p.matcher(b);
        String id = "";
        if (m.find()) {
            id = m.group(1);
        }
        m.reset();
        String ra = "0.0", dec = "0.0";
        p = Pattern.compile("J2000.*?:\\s*?([-|\\d+].+?)\\s+?([-|\\d+].+)\\s*[\\(]");
        m = p.matcher(b);
        if (m.find()) {
            ra = m.group(1);
            dec = m.group(2);
        }

        String pm1 = "0.0", pm2 = "0.0";
        p = Pattern.compile("P Motions.*?:\\s*?([-|\\d+].+?)\\s+?([-|\\d+].+)\\s*[\\[]");
        m = p.matcher(b);
        if (m.find()) {
            pm1 = m.group(1);
            pm2 = m.group(2);
        }
        sb.append(id);
        sb.append("\t");
        sb.append(ra);
        sb.append("\t");
        sb.append(dec);
        sb.append("\t");
        sb.append(pm1);
        sb.append("\t");
        sb.append(pm2);
        sb.append("\n");
        return sb;
    }
}
