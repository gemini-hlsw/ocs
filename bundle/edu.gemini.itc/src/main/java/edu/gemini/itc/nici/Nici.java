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
// Will need to mess with how the filters work.  Currently uses acqcam code
// for this.  Also, NICI has two detectors.  Implement here? 
// Or just call writeoutput() twice with different filters? mdillman 2/9/2009

package edu.gemini.itc.nici;

import java.io.PrintWriter;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Filter;
import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.FixedOptics;
import edu.gemini.itc.shared.PupilStopWheel;
import edu.gemini.itc.shared.SpiderMask;
import edu.gemini.itc.shared.Dichroic;

/**
 * Aquisition Camera specification class
 */
public class Nici extends Instrument {
    /** Related files will be in this subdir of lib */
    public static final String INSTR_DIR = "nici";
    
    /** Related files will start with this prefix */
    public static final String INSTR_PREFIX = "";
    
    // Instrument reads its configuration from here.
    private static final String FILENAME = "nici" + getSuffix();
    
    // Well Depth
    // 65000 e- at at medium bias.  Will we need other values?
    private static final double WELL_DEPTH = 65000;
    
    
    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _colorFilter;
    private Filter _filter;
    private Filter _filterC2;

    // Each nici object is really a single NICI channel.  Need to know which.
    private String _channel;
    
    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;
    
    /**
     * construct a single-channel NICI.  Filter and detector are chosen based
     * on which channel this is.  Value of channel is either 1 or 2
     */
    public Nici(String filter, String channel, String pupilMask, String dichroic)
    throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.
        // Since this instrument always has a filter, the filter-passed
        // range is used for _observingStart, _observingEnd.
        // old way of getting the observing start and end
        //_observingStart = WavebandDefinition.getStart(filterBand);
        //_observingEnd = WavebandDefinition.getEnd(filterBand);
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        //_colorFilter = new ColorFilter(filterBand, getDirectory()+"/");

        //_print("<pre> NICI Constructor: </pre>");

	_channel = channel;

        addComponent(new SpiderMask(getDirectory() + "/"));

	addComponent(new PupilStopWheel(getDirectory() + "/", pupilMask));

	addComponent(new Dichroic(getDirectory() + "/", dichroic, channel));

        _filter = new Filter(getPrefix(), filter, getDirectory() + "/",
                Filter.CALC_EFFECTIVE_WAVELEN);

        //_print("<pre>" _filterC1.toString() + "</pre>");
        
        //addComponent(new NDFilterWheel(filterChannel2, getDirectory() + "/"));
        //_print("Filter 2 added");

        addComponent(new FixedOptics(getDirectory() + "/", getPrefix()));
	addComponent(_filter);
        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector" + channel,
                "1024x1024 Aladdin III InSb"));

        //Grab starting and ending wavelengths from instrument file
        //Will be used if there is no filter in a given channel
        _observingStart = super.getStart();
        _observingEnd = super.getEnd();

        //New way (Directly from the filter)
        _observingStart = _filter.getStart();
        _observingEnd = _filter.getEnd();
    }
    
    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _filter.getEffectiveWavelength();
    }
    
    /** Returns the subdirectory where this instrument's data files are. */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }
    
    public double getObservingStart() {
        return _observingStart;
    }
    
    public double getObservingEnd() {
        return _observingEnd;
    }
    
    /** The prefix on data file names for this instrument. */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }
    
    public double getWellDepth() {
        return WELL_DEPTH;
    }

    //Unlike other ITC instruments, NICI's read noise is dependent
    //on exposure time.  Overriding the getReadNoise() function
    //from the parent Instrument class.
    //This truely is a "total noise" function since it incorporates
    //the noise from light leaks
    public double getReadNoise(double expTime) {
	int N;  // number of non-destructive reads
	double sigma_read_sqrd, sigma_dark_sqrd, totalNoise;
	double sigma1 = 34.2; //e-
	double sigma2 = 14.5; //e-

	N = (int)(expTime/0.38)+1;

	//readNoise = 35.5 * Math.pow((double)(nonDestructiveReads/6.0),-0.34);

	//New SUR read noise equation
	sigma_read_sqrd = ((12*(N-1))/(N*(N+1)))*sigma1*sigma1 + sigma2*sigma2;

	sigma_dark_sqrd = (6/5)*(N*N+1)/(N*N+N)*this.getDarkCurrent()*0.38*(N-1);

	totalNoise = Math.sqrt(sigma_read_sqrd + sigma_dark_sqrd);

	return totalNoise;
	
    }

    //NICI's dark current is dependent on exposure time and is different
    //in the red and blue channels (higher in the red).  Overriding the
    //getDarkCurrent function from the parent Instrument class
    //
    //Still a little confused as to what the equation I've been given represents
    //Just using the basic 4 or 1 e-/s for now, based on channel.
    public double getDarkCurrent() {
	int N; // number of non-destructive reads
	double F; //background flux in e-/s

	if (_channel.equals("1"))
	    F = 4; // e-/s
	else
	    F = 1; // e-/s
	   
	
	//darkCurrent = Math.sqrt((6/5)*(N*N+1)/(N*N+N)*F*0.38*(N-1));

	return F;
    }
    

    //Calculate FWHM of an AO corrected core
    //
    //No information on this currently, so just using a method similar to Altair
    public double getAOCorrectedFWHM() {
	double fwhmAO = Math.sqrt(6.817E-10*Math.pow(this.getEffectiveWavelength(),2)+6.25E-4);
        return fwhmAO;
    }

    //Calculate Strehl value
    //
    //No information on this currently.  Testing by setting Strehl to
    //0, which should result in a S2N equal to a non-AO exposure
    public double getStrehl() {
	return 0.0;
    }
}
