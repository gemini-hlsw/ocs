// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GrismOptics.java,v 1.5 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TextFileReader;
import java.text.ParseException;
import java.util.Hashtable;
import java.io.IOException;

/**
 * This represents the transmission of the Grism optics.
 * See REL-557
 */
public class GrismOptics extends TransmissionElement {

    class CoverageEntry {
		public double _start, _end, _width;
        public double _resolution;
		public String _name;
        public String _grismDataFileName;

		CoverageEntry(String name, double start, double end, double width,
				double res, String grismDataFileName) {
			_name = name;
			_start = start;
			_end = end;
			_width = width;
            _resolution = res;
            _grismDataFileName = grismDataFileName;
		}
	}

	private String _grismName;
    private String _coverageDataName;
	private double _slitSize;

	private Hashtable<String, CoverageEntry> _coverage = new Hashtable<String, CoverageEntry>(
			10);

	public GrismOptics(String directory, String grismName, double slitSize,
			String filterBand) throws Exception {

		super();

		_grismName = grismName;
        _coverageDataName = buildCoverageDataName(grismName, filterBand);
		_slitSize = slitSize;

		readCoverageData(directory);

        // REL-557: Get grism data file name from coverage table
        String fname = directory + getGrismDataFileName();
        try {
            setTransmissionSpectrum(fname);
        } catch (Exception e) {
            throw new Exception("Grism/filter " + grismName + "+" + filterBand + " combination is not supported.");
        }
    }

    private String buildCoverageDataName(String grismName, String filter) {
        return grismName + "-" + filter;
    }

	private void readCoverageData(String directory) throws Exception {
		try {
			TextFileReader tr = new TextFileReader(directory
					+ Flamingos2.getPrefix() + "grism-coverage"
					+ Instrument.getSuffix());

			while (tr.hasMoreData()) {
				String grism = tr.readString();
                String filter = tr.readString();
				double start = tr.readDouble();
				double end = tr.readDouble();
				double width = tr.readDouble();
                double res = tr.readDouble();
                String grismDataFileName = tr.readString();

                String name = buildCoverageDataName(grism, filter);
				CoverageEntry ce = new CoverageEntry(name, start, end, width,
						res, grismDataFileName);
				_coverage.put(name, ce);
			}
		} catch (ParseException e) {
			throw new Exception("Error while parsing grism_coverage file", e);
		} catch (IOException e) {
			throw new Exception(
					"Unexpected end of file in grism_coverage file", e);
		}
	}

	public double getStart() {
		CoverageEntry ce = _coverage.get(_coverageDataName);
		if (ce == null)
			return this.get_trans().getStart();
		return ce._start;
	}

	public double getEnd() {
		CoverageEntry ce = _coverage.get(_coverageDataName);
		if (ce == null)
			return this.get_trans().getEnd();
		return ce._end;
	}

	public double getEffectiveWavelength() {
		return (getStart() + getEnd()) / 2;
	}

	public double getPixelWidth() {
		CoverageEntry ce = _coverage.get(_coverageDataName);
		if (ce == null)
			return _slitSize;
		return ce._width;
	}

	public double getGrismResolution() {
		CoverageEntry ce = _coverage.get(_coverageDataName);
		if (ce == null)
			return 0;
		return ce._resolution;
	}

    public String getGrismDataFileName() {
   		CoverageEntry ce = _coverage.get(_coverageDataName);
   		if (ce == null)
   			return null;
   		return ce._grismDataFileName;
   	}

	public String toString() {
		return "Grism Optics: " + _grismName;
	}
}
