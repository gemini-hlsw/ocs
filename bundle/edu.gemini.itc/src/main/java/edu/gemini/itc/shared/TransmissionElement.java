// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: TransmissionElement.java,v 1.4 2004/02/13 13:00:59 bwalls Exp $
//
package edu.gemini.itc.shared;

/**
 * A TransmissionElement has a transmission spectrum that can
 * be convolved with a spectrum.
 * Examples are water in the atmosphere and instrument filters.
 * This class can be used directly if the client knows the path
 * to the data file.  But usually a class will be derived from this
 * class that knows something about the data file naming conventions
 * for that type of element.
 */
public class TransmissionElement implements SampledSpectrumVisitor {
	private String _resourceName = null;

    // The transmission spectrum
    private ArraySpectrum _trans = null;

    /**
     * Default constructor for TransmissionElement.
     * Class will not be useful until transmission spectrum is set.
     */
    public TransmissionElement() {
    }

    /**
     * Constructs a TransmissionElement
     * @param name name of this component
     * @param transmission transmission spectrum for this component
     */
    public TransmissionElement(ArraySpectrum transmission) {
        _trans = (ArraySpectrum) transmission.clone();
    }

    /**
     * Constructs a TransmissionElement using specified transmission data file
     * @param name name of this component
     * @param transmissionFileName transmission spectrum for this component
     */
    public TransmissionElement(String resourceName) throws Exception {
        _resourceName = resourceName;
        setTransmissionSpectrum(resourceName);
    }

    /**
     * Attempts to load the specified transmision spectrum.
     */
    public void setTransmissionSpectrum(String resourceName) throws Exception {
        _resourceName = resourceName;
        _trans = new DefaultArraySpectrum(resourceName);
    }

    /**
     * Set the transmission spectrum.
     */
    public void setTransmissionSpectrum(ArraySpectrum spectrum) {
        _trans = spectrum;
    }

    /**
     * Apply the transmission convolution for this component.
     */
    public void visit(SampledSpectrum sed) {
        double multiplier = 0;
	System.out.println("Applying transmission visitor: "+_resourceName);
        for (int i = 0; i < sed.getLength(); i++) {
            double startval = sed.getX(i);
            multiplier = _trans.getY(startval);
	    //System.out.println(i + " % 100 = "+ (i%100));
	    // if (_resourceName.equals("/niri/CH4ice2275_G0243.dat")) {
	    //System.out.println(_resourceName +" SED x: "+ startval + " SED val: " + sed.getY(i) + " Trans mult: "+ multiplier + " Result: " + sed.getY(i)*multiplier);
		//System.out.println(" Current SED integral: " + sed.getIntegral());
	    //}
            sed.setY(i, sed.getY(i) * multiplier);
        }
    //System.out.println("After transmission application: "+sed.getIntegral());
    }

    public String toString() {
        if (_resourceName == null) {
            return "Transmission Element";
        } else {
            return "Transmission Element - data file " + _resourceName;
        }
    }

    public ArraySpectrum get_trans() {
		return _trans;
	}
}
