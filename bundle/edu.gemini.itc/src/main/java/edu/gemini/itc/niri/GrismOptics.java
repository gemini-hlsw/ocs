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
package edu.gemini.itc.niri;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.TransmissionElement;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the transmission of the Grism optics.
 */
public class GrismOptics extends TransmissionElement {


    private List _x_values;
    private List _F6_res_values;
    private List _F14_res_values;
    private List _spectralCoverageArray;
    private List _spectralPixelWidthArray;
    private List _resolvingPowerArray;
    private String _grismName;
    private String _cameraName;
    private String _focalPlaneMaskOffset;

    public GrismOptics(String directory, String grismName,
                       String cameraName, String focalPlaneMaskOffset,
                       String stringSlitWidth)
            throws Exception {

        super(directory + Niri.getPrefix() +
                grismName + "_" + cameraName + Instrument.getSuffix());

        _grismName = grismName;
        _cameraName = cameraName;
        _focalPlaneMaskOffset = focalPlaneMaskOffset;

        //Read The transmission file
        TextFileReader dfr = new TextFileReader(directory +
                Niri.getPrefix() +
                grismName + "_" +
                cameraName +
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

        //New read of Grism Resolving Power
        TextFileReader grismResolve = new TextFileReader(directory +
                Niri.getPrefix() +
                "grism-resolution-" +
                stringSlitWidth +
                "_" + cameraName +
                Instrument.getSuffix());
        _resolvingPowerArray = new ArrayList();
        //int _resolvingPower = 0;
        try {
            while (true) {
                _resolvingPowerArray.add(new Integer(grismResolve.readInt()));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }

        TextFileReader grismCoverage = new TextFileReader(directory +
                Niri.getPrefix() +
                "grism-coverage-" +
                focalPlaneMaskOffset +
                Instrument.getSuffix());
        _spectralCoverageArray = new ArrayList();
        _spectralPixelWidthArray = new ArrayList();


        //int _resolvingPower = 0;
        try {
            while (true) {
                _spectralCoverageArray.add(new Double(grismCoverage.readDouble()));
                _spectralCoverageArray.add(new Double(grismCoverage.readDouble()));
                _spectralPixelWidthArray.add(new Double(grismCoverage.readDouble()));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }

        //System.out.println ("Start: " +getStart() + " End: "+ getEnd());

        //Read the Grism Resolving power
        //       TextFileReader grp = new TextFileReader(directory +
        //				       Niri.getPrefix() +
        //				       "grism-resolve" +
        //				       Instrument.getSuffix());
        //_F6_res_values = new ArrayList();
        //_F14_res_values = new ArrayList();

        //      int F6_res=0;
        //int F14_res=0;

        //try {
        //	   while (true) {
        //	       F6_res= grp.readInt();
        //	       _F6_res_values.add(new Integer(F6_res));
        //	       F14_res= grp.readInt();
        //	       _F14_res_values.add(new Integer(F14_res));
        //	   }
        //      } catch (ParseException e) {
        //	  throw e;
        //      } catch (IOException e) {
        //	  // normal eof
        // }


    }

//    public double getStart ()
//    {
//       return ((Double)_x_values.get(2)).doubleValue();
//    }

//    public double getEnd()
//    {
//       return ((Double)_x_values.get(_x_values.size()-3)).doubleValue();
//    }

    public double getStart() {
        return ((Double) _spectralCoverageArray.get(getGrismNumber() * 2)).doubleValue();
    }

    public double getEnd() {
        return ((Double) _spectralCoverageArray.get(getGrismNumber() * 2 + 1)).doubleValue();
    }

    public double getEffectiveWavelength() {
        return (getStart() + getEnd()) / 2;
    }

    public double getPixelWidth() {
        return ((Double) _spectralPixelWidthArray.get(getGrismNumber())).doubleValue();
    }

    // for right now effective wavelen will just be the mid pt of the filter

    //    public double getEffectiveWavelength()
//    {
//       return ((Double)_x_values.get((int)_x_values.size()/2)).doubleValue();
//    }
    public int getGrismNumber() {
        int grism_num = 0;

        if (_grismName.equals(NiriParameters.JGRISM)) {
            grism_num = NiriParameters.J;
        } else if (_grismName.equals(NiriParameters.HGRISM)) {
            grism_num = NiriParameters.H;
        } else if (_grismName.equals(NiriParameters.KGRISM)) {
            grism_num = NiriParameters.K;
        } else if (_grismName.equals(NiriParameters.LGRISM)) {
            grism_num = NiriParameters.L;
        } else if (_grismName.equals(NiriParameters.MGRISM)) {
            grism_num = NiriParameters.M;
        }
        return grism_num;
    }

    public double getGrismResolution() {
        return ((Integer) _resolvingPowerArray.get(getGrismNumber())).intValue();
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
        return "Grism Optics: " + _grismName;
    }

}
