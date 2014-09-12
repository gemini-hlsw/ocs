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

	private static final ArrayReader READER = new ArrayReader() {
		public Object build(String line) {
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
		ReferenceLine[] ret = (ReferenceLine[]) READER.readArray(resource, new ReferenceLine[0]);
		for (int i = 0; i < ret.length; i++)
			ret[i].drawLabel = drawLabels;
		return ret;
	}

}
