// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TextFileReader;

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
    
    public GratingOptics(String directory, String prefix, String gratingName,
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

        //New read of Grating Proporties
        TextFileReader grismProperties = new TextFileReader(directory +
                prefix +
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
        
        if (_gratingName.equals(NifsParameters.Z_G5602)) {
            grating_num = NifsParameters.Z_G5602_N;
        } else if (_gratingName.equals(NifsParameters.J_G5603)) {
            grating_num = NifsParameters.J_G5603_N;
        } else if (_gratingName.equals(NifsParameters.H_G5604)) {
            grating_num = NifsParameters.H_G5604_N;
        } else if (_gratingName.equals(NifsParameters.K_G5605)) {
            grating_num = NifsParameters.K_G5605_N;
        } else if (_gratingName.equals(NifsParameters.KS_G5606)) {
            grating_num = NifsParameters.KS_G5606_N;
        } else if (_gratingName.equals(NifsParameters.KL_G5607)) {
            grating_num = NifsParameters.KL_G5607_N;
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
