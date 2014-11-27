//
// $
//

package edu.gemini.catalog.skycat.binding.adapter;

import edu.gemini.catalog.skycat.DefaultOutputAdapter;
import edu.gemini.catalog.skycat.SkycatOutputAdapter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A {@link edu.gemini.catalog.skycat.DefaultOutputAdapter} for use with the PPMXL, which returns
 * its results in a different format than expected by the catalog output parser.
 */
@Deprecated
public final class PpmxlOutputAdapter extends DefaultOutputAdapter {
    public static final Factory FACTORY = new Factory() {
        public SkycatOutputAdapter create(BufferedReader rdr) {
            return new PpmxlOutputAdapter(rdr);
        }
    };

    /**
     * Constructs with the BufferedReader source of catalog data.
     */
    public PpmxlOutputAdapter(BufferedReader rdr) {
        super(rdr);
    }

    public String readLine() throws IOException {
        String line = getReader().readLine();
        //b1mag => B r1mag => R imag => I Jmag => J Hmag => H Kmag => K (actually Ks)
        if (line != null) {
            if (line.startsWith("PPMXL")) { //column headers line
                line = line.replaceFirst("b1mag", "Bmag");
                line = line.replaceFirst("r1mag", "Rmag");
                line = line.replaceFirst("imag", "Imag");
            }
        }
        return line;
    }
}
