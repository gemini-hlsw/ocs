package jsky.app.ot.gemini.bhros.ech;

import edu.gemini.spModel.gemini.bhros.ech.ArrayReader;

import java.io.InputStream;

/**
 * Represents a single reference line, which is simply a wavelength + label ADT that can
 * be drawn on the Echellogram. There is also a hacked way to turn labels off if you want
 * to ... this can be helpful for ReferenceLine arrays that are very large.
 * @author rnorris
 */
class ReferenceLine {

    private static final ArrayReader<ReferenceLine> READER = new ArrayReader<ReferenceLine>() {
        public ReferenceLine build(String line) {
            String[] parts = line.split("\\s+", 2);
            return new ReferenceLine(Double.parseDouble(parts[0]), parts[1]);
        }
    };

    public final double wavelength;
    public final String id;
    public boolean drawLabel;

    private ReferenceLine(double wavelength, String id) {
        this.wavelength = wavelength;
        this.id = id;
    }

    public static ReferenceLine[] readArray(InputStream resource, boolean drawLabels) {
        ReferenceLine[] ret = READER.readArray(resource, new ReferenceLine[0]);
        for (ReferenceLine aRet : ret) aRet.drawLabel = drawLabels;
        return ret;
    }

}
