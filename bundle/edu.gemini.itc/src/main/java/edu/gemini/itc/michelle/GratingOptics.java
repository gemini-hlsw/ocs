// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.michelle;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.ITCConstants;

import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;

import java.io.IOException;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public class GratingOptics extends TransmissionElement {


    private List _x_values;
    //private List _F6_res_values;
    //private List _F14_res_values;
    //private List _spectralCoverageArray;
    //private List _spectralPixelWidthArray;
    private List _resolvingPowerArray;
    private List _dispersionArray;
    private List _blazeArray;
    private List _resolutionArray;
    private List _gratingNameArray;
    private String _gratingName;
    //private String _focalPlaneMaskOffset;
    private double _centralWavelength;
    private int _detectorPixels;
    private int _spectralBinning;

    public GratingOptics(String directory, String gratingName,
                         //	      String focalPlaneMaskOffset,
                         String stringSlitWidth,
                         double centralWavelength,
                         int detectorPixels,
                         int spectralBinning)
            throws Exception {

        super(directory + Michelle.getPrefix() +
              gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;

        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;
        //    _focalPlaneMaskOffset = focalPlaneMaskOffset;

        //Read The transmission file for the start and stop wavelengths
        TextFileReader dfr = new TextFileReader(directory +
                                                Michelle.getPrefix() +
                                                gratingName +
                                                Instrument.getSuffix());
        _x_values = new ArrayList();

        double x = 0;
        double y = 0;

        try {
            while (true) {
                x = dfr.readDouble();
                _x_values.add(new Double(x));
                y = dfr.readDouble();
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }

        //New read of Grating Proporties
        TextFileReader grismProperties = new TextFileReader(directory +
                                                            Michelle.getPrefix() +
                                                            "gratings" +
                                                            Instrument.getSuffix());
        _resolvingPowerArray = new ArrayList();
        _gratingNameArray = new ArrayList();
        _blazeArray = new ArrayList();
        _resolutionArray = new ArrayList();
        _dispersionArray = new ArrayList();
        //int _resolvingPower = 0;
        try {
            while (true) {
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

        //TextFileReader grismCoverage = new TextFileReader(directory+
        //					      Niri.getPrefix()+
        //					      "grism-coverage-"+
        //					      focalPlaneMaskOffset+
        //					      Instrument.getSuffix());
        //    _spectralCoverageArray = new ArrayList();
        //_spectralPixelWidthArray = new ArrayList();


        //int _resolvingPower = 0;
        //try {
        //while (true) {
        //    _spectralCoverageArray.add(new Double(grismCoverage.readDouble()));
        //    _spectralCoverageArray.add(new Double(grismCoverage.readDouble()));
        //    _spectralPixelWidthArray.add(new Double(grismCoverage.readDouble()));
        //}
        //} catch (ParseException e) {
        //throw e;
        //} catch (IOException e) {
        // normal eof
        //}


    }


    public double getStart() {
        //return ((Double)_spectralCoverageArray.get(getGrismNumber()*2)).doubleValue();
        return _centralWavelength - (
                (((Double) _dispersionArray.get(getGratingNumber())).doubleValue())
                * _detectorPixels / 2) * _spectralBinning;
    }

    public double getEnd() {
        //return ((Double)_spectralCoverageArray.get(getGrismNumber()*2+1)).doubleValue();
        return _centralWavelength + (
                (((Double) _dispersionArray.get(getGratingNumber())).doubleValue())
                * _detectorPixels / 2) * _spectralBinning;
    }

    public double getEffectiveWavelength() {
        //return (getStart()+getEnd())/2;
        return _centralWavelength;
    }

    public double getPixelWidth() {
        //return ((Double)_spectralPixelWidthArray.get(getGrismNumber())).doubleValue();
        return ((Double) _dispersionArray.get(getGratingNumber())).doubleValue() * _spectralBinning;

    }

    // for right now effective wavelen will just be the mid pt of the filter

//    public double getEffectiveWavelength()
//    {
//       return ((Double)_x_values.get((int)_x_values.size()/2)).doubleValue();
//    }
    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(MichelleParameters.LOW_N)) {
            grating_num = MichelleParameters.LOWN;
        } else if (_gratingName.equals(MichelleParameters.LOW_Q)) {
            grating_num = MichelleParameters.LOWQ;
        } else if (_gratingName.equals(MichelleParameters.MED_N1)) {
            grating_num = MichelleParameters.MEDN1;
        } else if (_gratingName.equals(MichelleParameters.MED_N2)) {
            grating_num = MichelleParameters.MEDN2;
        } else if (_gratingName.equals(MichelleParameters.ECHELLE_N)) {
            grating_num = MichelleParameters.ECHELLEN;
        } else if (_gratingName.equals(MichelleParameters.ECHELLE_Q)) {
            grating_num = MichelleParameters.ECHELLEQ;
        }
        return grating_num;
    }

    public double getGratingResolution() {
        return ((Integer) _resolvingPowerArray.get(getGratingNumber())).intValue();
    }

    public String getGratingName() {
        return (String) _gratingNameArray.get(getGratingNumber());
    }

    public double getGratingBlaze() {
        return ((Integer) _blazeArray.get(getGratingNumber())).intValue();
    }

    public double getGratingDispersion_nm() {
        return ((Double) _resolutionArray.get(getGratingNumber())).doubleValue();
    }

    public double getGratingDispersion_nmppix() {
        //System.out.println("Grating Num: "+ getGratingNumber() + "pixval" +
        //		((Double)_dispersionArray.get(getGratingNumber())).doubleValue());
        return ((Double) _dispersionArray.get(getGratingNumber())).doubleValue()
                * _spectralBinning;
    }

    //    public double getGrismResolution()
//     {
// 	int grism_res=0;

// 	if (_grismName.equals(NiriParameters.JGRISM)){
// 	    if (_cameraName.equals(NiriParameters.F6)) {
// 		grism_res=
// 		    ((Integer)_F6_res_values.get(NiriParameters.J)).intValue();
// 	    } else {
// 		grism_res=
// 		    ((Integer)_F14_res_values.get(NiriParameters.J)).intValue();
// 	    }
// 	} else if (_grismName.equals(NiriParameters.HGRISM)){
// 	    if (_cameraName.equals(NiriParameters.F6)) {
// 		grism_res=
// 		    ((Integer)_F6_res_values.get(NiriParameters.H)).intValue();
// 	    } else {
// 		grism_res=
// 		    ((Integer)_F14_res_values.get(NiriParameters.H)).intValue();
// 	    }
// 	} else if (_grismName.equals(NiriParameters.KGRISM)){
// 	    if (_cameraName.equals(NiriParameters.F6)) {
// 		grism_res=
// 		    ((Integer)_F6_res_values.get(NiriParameters.K)).intValue();
// 	    } else {
// 		grism_res=
// 		    ((Integer)_F14_res_values.get(NiriParameters.K)).intValue();
// 	    }
// 	} else if (_grismName.equals(NiriParameters.LGRISM)){
// 	    if (_cameraName.equals(NiriParameters.F6)) {
// 		grism_res=
// 		    ((Integer)_F6_res_values.get(NiriParameters.L)).intValue();
// 	    } else {
// 		grism_res=
// 		    ((Integer)_F14_res_values.get(NiriParameters.L)).intValue();
// 	    }
// 	} else if (_grismName.equals(NiriParameters.MGRISM)){
// 	    if (_cameraName.equals(NiriParameters.F6)) {
// 		grism_res=
// 		    ((Integer)_F6_res_values.get(NiriParameters.M)).intValue();
// 	    } else {
// 		grism_res=
// 		    ((Integer)_F14_res_values.get(NiriParameters.M)).intValue();
// 	    }
// 	}
// 	return grism_res;
//     }

    public String toString() { // return "Grism Optics: " +
// 				               Niri.getPrefix() +
// 					       _grismName + "_" +
// 					       _cameraName +
// 					       Instrument.getSuffix();
//    }
        return "Grating Optics: " + _gratingName;
    }

}
