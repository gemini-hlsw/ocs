// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.*;

import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;

import java.io.IOException;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public class GratingOptics extends TransmissionElement {


    private List _resolvingPowerArray;
    private List _dispersionArray;
    private List _blazeArray;
    private List _resolutionArray;
    private List _gratingNameArray;
    private String _gratingName;
    private double _centralWavelength;
    private int _detectorPixels;
    private int _spectralBinning;

    public GratingOptics(String directory, String prefix, String gratingName, Detector detector,
                         String stringSlitWidth,
                         double centralWavelength,
                         int detectorPixels,
                         int spectralBinning)
            throws Exception {

        super(directory + prefix +
              gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;

        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;

        //New read of Grating Properties
        String detectorPrefix = detector.toString().contains("EEV") ? "eev_" : ""; // REL-477
        TextFileReader grismProperties = new TextFileReader(directory +
                                                            prefix +
                                                            detectorPrefix +
                                                            "gratings" +
                                                            Instrument.getSuffix());
        _resolvingPowerArray = new ArrayList();
        _gratingNameArray = new ArrayList();
        _blazeArray = new ArrayList();
        _resolutionArray = new ArrayList();
        _dispersionArray = new ArrayList();
        try {
            while (grismProperties.hasMoreData()) {
                _gratingNameArray.add(new String(grismProperties.readString()));
                _blazeArray.add(new Integer(grismProperties.readInt()));
                _resolvingPowerArray.add(new Integer(grismProperties.readInt()));
                _resolutionArray.add(new Double(grismProperties.readDouble()));
                _dispersionArray.add(new Double(grismProperties.readDouble()));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }

    }


    public double getStart() {
        return _centralWavelength - (
                (((Double) _dispersionArray.get(getGratingNumber())).doubleValue())
                * _detectorPixels / 2);
    }

    public double getEnd() {
        return _centralWavelength + (
                (((Double) _dispersionArray.get(getGratingNumber())).doubleValue())
                * _detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return _centralWavelength;
    }

    public double getPixelWidth() {
        return ((Double) _dispersionArray.get(getGratingNumber())).doubleValue() * _spectralBinning;

    }

    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(GmosParameters.B1200_G5301)) {
            grating_num = GmosParameters.B1200;
        } else if (_gratingName.equals(GmosParameters.R831_G5302)) {
            grating_num = GmosParameters.R831;
        } else if (_gratingName.equals(GmosParameters.B600_G5303)) {
            grating_num = GmosParameters.B600;
        } else if (_gratingName.equals(GmosParameters.R600_G5304)) {
            grating_num = GmosParameters.R600;
        } else if (_gratingName.equals(GmosParameters.R400_G5305)) {
            grating_num = GmosParameters.R400;
        } else if (_gratingName.equals(GmosParameters.R150_G5306)) {
            grating_num = GmosParameters.R150;
        }
        return grating_num;
    }

    public double getGratingResolution() {
        return ((Integer) _resolvingPowerArray.get(getGratingNumber())).intValue();
    }

    public double getGratingBlaze() {
        return ((Integer) _blazeArray.get(getGratingNumber())).intValue();
    }

    public double getGratingDispersion_nm() {
        return ((Double) _resolutionArray.get(getGratingNumber())).doubleValue();
    }

    public double getGratingDispersion_nmppix() {
        return ((Double) _dispersionArray.get(getGratingNumber())).doubleValue()
                * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + _gratingName;
    }

}
