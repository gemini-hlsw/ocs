package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.TransmissionElement;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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

        super(directory + Gnirs.getPrefix() +
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
        * _detectorPixels / 2);//*_spectralBinning;
    }
    
    public double getEnd() {
        return _centralWavelength + (
        (((Double) _dispersionArray.get(getGratingNumber())).doubleValue())
        * _detectorPixels / 2);//*_spectralBinning;
    }
    
    public double getEffectiveWavelength() {
        return _centralWavelength;
    }
    
    public double getPixelWidth() {
        return ((Double) _dispersionArray.get(getGratingNumber())).doubleValue() * _spectralBinning;
        
    }
    
    
    public int getGratingNumber() {
        int grating_num = 0;
        
        if (_gratingName.equals(GnirsParameters.G10)) {
            grating_num = GnirsParameters.G10_N;
        } else if (_gratingName.equals(GnirsParameters.G32)) {
            grating_num = GnirsParameters.G32_N;
        } else if (_gratingName.equals(GnirsParameters.G110)) {
            grating_num = GnirsParameters.G110_N;
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
