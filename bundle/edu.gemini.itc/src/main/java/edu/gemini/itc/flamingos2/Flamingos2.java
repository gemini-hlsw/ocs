// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: AcquisitionCamera.java,v 1.4 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Flamingos 2 specification class
 */
public class Flamingos2 extends Instrument {
    class ReadNoiseEntry {
        public String _name;
        public double _readNoise;

        ReadNoiseEntry(String name, double readNoise) {
            _name = name;
            _readNoise = readNoise;
        }
    }

    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;

    // Instrument reads its configuration from here.
    private static final String FILENAME = "flamingos2" + getSuffix();

    private static final String HIGHNOISE = "highNoise";

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "flamingos2";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";
    public static final String INSTR_PREFIX_2 = "flamingos2_";
    // Readnoise names
    private static final String LOWNOISE = "lowNoise";
    private static final String MEDNOISE = "medNoise";

    // Well Depth
    private static final double WELL_DEPTH = 200000.0;

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public static String getPrefix2() {
        return INSTR_PREFIX_2;
    }

    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _colorFilter;
    private String _filterBand;

    private String _grism;
    private GrismOptics _grismOptics;
    private double _observingEnd;
    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private String _readNoise;
    private String _focalPlaneMask;

    private Hashtable<String, ReadNoiseEntry> _readNoiseLevels = new Hashtable<String, ReadNoiseEntry>(3);

    private double _slitSize;

    /**
     * construct a Flamingos2 object with specified color filter and ND filter.
     */
    public Flamingos2(Flamingos2Parameters fp) throws Exception {
        super(INSTR_DIR, FILENAME);

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();

        _filterBand = fp.getColorFilter();
        _readNoise = fp.getReadNoise();
        _focalPlaneMask = fp.getFPMask();
        _grism = fp.getGrism();
        _slitSize = fp.getSlitSize() * getPixelSize();

        addColorFilter();

        _dtv = new DetectorsTransmissionVisitor(1,
                getDirectory() + "/" + getPrefix2() + "ccdpix" + Instrument.getSuffix());

        addComponent(new FixedOptics(getDirectory() + File.separator, getPrefix()));
        addComponent(new Detector(getDirectory() + File.separator, getPrefix(),
                "detector", "2048x2048 Hawaii-II (HgCdTe)"));

        addGrism(_filterBand);
        readReadNoiseData();
    }

    public void addColorFilter() throws Exception {
        if (_filterBand.equalsIgnoreCase(Flamingos2Parameters.CLEAR))
            return;

        _colorFilter = Filter.fromFile(getPrefix(), _filterBand, getDirectory() + "/");
        addComponent(_colorFilter);

    }

    private void addGrism(String filterBand) throws Exception {
        if (_grism.equalsIgnoreCase(Flamingos2Parameters.NOGRISM))
            return;

        try {
            _grismOptics = new GrismOptics(getDirectory() + File.separator, _grism, _slitSize * getPixelSize(), filterBand);
        } catch (Exception e) {
            throw new Exception("Grism/filter " + _grism + "+" + filterBand + " combination is not supported.");
        }

        addComponent(_grismOptics);
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    /**
     * Returns the effective observing wavelength. This is properly calculated
     * as a flux-weighted averate of observed spectrum. So this may be
     * temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (_colorFilter == null)
            return (int) (getEnd() + getStart()) / 2;
        return (int) _colorFilter.getEffectiveWavelength();
    }

    public double getGrismResolution() {
        if (_grismOptics != null)
            return _grismOptics.getGrismResolution();
        return 0;
    }

    public double getObservingEnd() {
        if (_colorFilter != null)
            return _colorFilter.getEnd();
        return _observingEnd;
    }

    public double getObservingStart() {
        if (_colorFilter != null)
            return _colorFilter.getStart();
        return _observingStart;
    }

    public edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    @Override
    public double getReadNoise() {
        ReadNoiseEntry re = _readNoiseLevels.get(_readNoise);
        if (re == null)
            re = _readNoiseLevels.get(Flamingos2.HIGHNOISE);
        return re._readNoise;
    }

    public double getSpectralPixelWidth() {
        return _grismOptics.getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    void readReadNoiseData() throws Exception {
        try {
            TextFileReader tr = new TextFileReader(getDirectory() + File.separator + Flamingos2.getPrefix()
                    + "readnoise" + Instrument.getSuffix());

            while (tr.hasMoreData()) {
                String name = tr.readString();
                double rn = tr.readDouble();

                ReadNoiseEntry re = new ReadNoiseEntry(name, rn);
                _readNoiseLevels.put(name, re);
            }
        } catch (ParseException e) {
            throw new Exception("Error while parsing readnoise file", e);
        } catch (IOException e) {
            throw new Exception("Unexpected end of file in readnoise file", e);
        }
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (Iterator itr = getComponents().iterator(); itr.hasNext(); ) {
            s += "<LI>" + itr.next().toString() + "<BR>";
        }
        s += "<LI>Read Noise: " + _readNoise + "\n";

        if (!_focalPlaneMask.equals("none"))
            s += "<LI>Focal Plane Mask: " + _focalPlaneMask + " pix slit\n";

        s += "<BR>Pixel Size: " + getPixelSize() + "<BR>";

        return s;
    }
}
