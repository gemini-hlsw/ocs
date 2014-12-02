//
// $
//

package edu.gemini.catalog.skycat.binding.adapter;

import edu.gemini.catalog.skycat.DefaultOutputAdapter;
import edu.gemini.catalog.skycat.SkycatOutputAdapter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A {@link edu.gemini.catalog.skycat.DefaultOutputAdapter} for use with the GSC2 @ STScI, which returns
 * its results in a different format than expected by the catalog output parser.
 */
@Deprecated
public final class Ucac3OutputAdapter extends DefaultOutputAdapter {
    public static final Factory FACTORY = new Factory() {
        public SkycatOutputAdapter create(BufferedReader rdr) {
            return new Ucac3OutputAdapter(rdr);
        }
    };

    /**
     * Constructs with the BufferedReader source of catalog data.
     */
    public Ucac3OutputAdapter(BufferedReader rdr) {
        super(rdr);
    }

    public String readLine() throws IOException {
        String line =getReader().readLine();
        if(line.startsWith("3UC")){//column headers line
            line=line.replaceFirst("f.mag","UCmag");
        }
        return line;
    }
}
