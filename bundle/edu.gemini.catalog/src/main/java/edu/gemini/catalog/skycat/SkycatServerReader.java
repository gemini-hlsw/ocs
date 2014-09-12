//
// $
//

package edu.gemini.catalog.skycat;

import edu.gemini.catalog.skycat.table.*;
import edu.gemini.shared.util.immutable.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example of how a different CatalogReader can be used to connect to a Skycat
 * server.
 */
public class SkycatServerReader implements CatalogReader {

    private static final Pattern HEADER_SEPARATOR_PAT = Pattern.compile("(?:-|\\s)*");

    private final InputStream is;

    private BufferedReader rdr;
    private Option<CatalogHeader> header = None.instance();
    private String curLine;

    public SkycatServerReader(InputStream is) {
        this.is = is;
    }

    public final void open() throws IllegalStateException, IOException {
        if (rdr != null) throw new IllegalStateException("already open");

        rdr = new BufferedReader(new InputStreamReader(is));
        header = None.instance();

        try {
            advanceLine();
            if (curLine != null) {
                header = createHeader(curLine);
                advanceLine();
            }
        } catch (IOException ex) {
            rdr.close();
            throw ex;
        }
    }

    protected Option<CatalogHeader> createHeader(String firstLine) {
        String[] columns = splitLine(firstLine);

        List<Tuple2<String, Class>> lst = new ArrayList<Tuple2<String, Class>>(columns.length);
        for (String col : columns) {
            lst.add(new Pair<String, Class>(col, String.class));
        }

        ImList<Tuple2<String, Class>> imList = DefaultImList.create(lst);
        return new Some<CatalogHeader>(new DefaultCatalogHeader(imList));
    }

    @Override
    public Option<CatalogHeader> getHeader() throws IOException {
        return header;
    }

    private void advanceLine() throws IOException {
        curLine = null;
        String line;
        while ((curLine == null) && (line = rdr.readLine()) != null) {
            line = line.trim();

            // Skip blank spaces and comments.
            if ("".equals(line) || line.startsWith("#")) continue;

            // Some servers terminate their results with "[EOD]".
            if ("[EOD]".equals(line)) break;

            // Skip header separators.
            Matcher mat = HEADER_SEPARATOR_PAT.matcher(line);
            if (mat.matches()) continue;

            curLine = line;
        }
    }

    public final void close() throws IOException {
        if (rdr != null) {
            rdr.close();
            rdr = null;
        }
    }

    protected String[] splitLine(String line) {
        return line.split("\t");
    }

    @Override
    public boolean hasNext() {
        return curLine != null;
    }

    private static final MapOp<Object, Object> TO_DOUBLE = new MapOp<Object, Object>() {
        @Override public Object apply(Object o) {
            if (o instanceof String) {
                try { o = Double.parseDouble((String) o); } catch (Exception ignore) { }
            }
            return o;
        }
    };

    @Override
    public CatalogRow next() throws IOException {
        Object[] tmp = splitLine(curLine);
        ImList<Object> lst = DefaultImList.create(Arrays.asList(tmp)).map(TO_DOUBLE);
        CatalogRow row = new DefaultCatalogRow(lst);
        advanceLine();
        return row;
    }
}
