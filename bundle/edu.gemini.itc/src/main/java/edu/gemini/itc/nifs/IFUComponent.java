// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.ApertureComponent;
import edu.gemini.itc.shared.ApertureComposite;
import edu.gemini.itc.shared.RectangularAperture;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Instrument;

import java.util.List;
import java.util.ArrayList;

/**
 *  This is the specific setup for the IFU of Nifs.
 *  It uses Hexagonal Apertures that can be in a line radialy
 *  from an arbitrary point in a gaussian.
 */

public class IFUComponent extends TransmissionElement {
    /**
     * IFU Aperture length in the X direction
     */
    public static final double IFU_LEN_X = 0.103;
    /**
     * IFU Length in the Y direction
     */
    public static final double IFU_LEN_Y = 0.042;
    /**
     * Diameter of the IFU
     */
    public static final double IFU_DIAMETER = Math.sqrt(IFU_LEN_X * IFU_LEN_Y);  //effective area
    /**
     * Spacing between IFU elements
     */
    public static final double IFU_SPACING = 0.0;
    private ApertureComposite IFUApertures;
    private List IFUOffsets;
    
    
    /**
     *  Constructor for a user defined radial ifu set.
     * @param IFURadialMin First IFU element when the radial set is selected
     * @param IFURadialMax Last IFU element when the radial set is selected
     * @param pixel_size pixel size in arcsec/pixel
     * @throws java.lang.Exception Thown if IFUComponent cannot be created
     */
    
    public IFUComponent(double IFURadialMin, double IFURadialMax, double pixel_size) throws Exception {
        
        super(ITCConstants.LIB + "/" + Nifs.INSTR_DIR + "/" + Nifs.INSTR_PREFIX + "ifu_trans" + Instrument.DATA_SUFFIX);
        int Napps;
        
        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList();
        
        
        Napps = new Double((IFURadialMax - IFURadialMin) / (IFU_LEN_X + IFU_SPACING) + 1).intValue();
        
        if (Napps < 0) Napps = 1;
        
        for (int i = 0; i < Napps; i++) {
            
            double Xpos = IFURadialMin + (IFU_LEN_X + IFU_SPACING) * i;
            
            IFUApertures.addAperture(new RectangularAperture(IFU_LEN_X, IFU_LEN_Y, Xpos, 0 ));
            IFUOffsets.add(new Double(Xpos));
        }
    }
    
    /**
     *  Constructor for a user defined rectangular aperture set centered around an arbitrary center.
     * @param numX Number of IFU elements in the X direction
     * @param numY Number of IFU elements in the Y direction
     * @param centerX X center for the IFU elements
     * @param centerY Y Center for the IFU elements
     * @param pixel_size Pixel size in arcsec/pixel
     * @throws java.lang.Exception Thrown if IFU cannot be created
     */
    public IFUComponent(int numX, int numY, double centerX, double centerY, double pixel_size) throws Exception {
        
        super(ITCConstants.LIB + "/" + Nifs.INSTR_DIR + "/" + Nifs.INSTR_PREFIX + "ifu_trans" + Instrument.DATA_SUFFIX);
        
        double xStart, xEnd;            //starting/ending positions in arcsecs for apertures
        double yStart, yEnd;            //starting/ending positions in arcsecs for apertures
        int Napps=numX*numY;            //Total number of apertures
        
        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList();
        
        if (Napps == 0) Napps = 1;
        
        xStart = centerX - Math.floor(numX/2)*(IFU_LEN_X+IFU_SPACING);
        xEnd = centerX + Math.floor(numX/2)*(IFU_LEN_X+IFU_SPACING);
        
        yStart = centerY - Math.floor(numY/2)*(IFU_LEN_Y+IFU_SPACING);
        yEnd= centerY + Math.floor(numY/2)*(IFU_LEN_Y+IFU_SPACING);
        
        for (int i = 0; i < numY; i++) {
            double yPos = yStart + (IFU_LEN_Y + IFU_SPACING) * i;
            for( int j=0; j < numX; j++) {
                double xPos = xStart + (IFU_LEN_X+IFU_SPACING) * j;
                
                IFUApertures.addAperture(new RectangularAperture(IFU_LEN_X, IFU_LEN_Y, xPos, yPos ));
                IFUOffsets.add(new Double(xPos));
                IFUOffsets.add(new Double(yPos));
            }
        }
        
        
        
        
    }
    
    /**
     * Constructor for a single IFU element
     * @param IFUOffsetX the X position of the IFU element
     * @param pixel_size Pixel size in arcsec/pixel
     * @throws java.lang.Exception Thrown if IFU cannot be created
     */
    public IFUComponent(double IFUOffsetX, double pixel_size) throws Exception {
        super(ITCConstants.LIB + "/" + Nifs.INSTR_DIR + "/" + Nifs.INSTR_PREFIX + "ifu_trans" + Instrument.DATA_SUFFIX);
        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList();
        
        IFUApertures.addAperture(new RectangularAperture(IFU_LEN_X, IFU_LEN_Y, IFUOffsetX, 0));
        IFUOffsets.add(new Double(IFUOffsetX));
    }
    
    
    /**
     * Method to get the Aperture Composite
     * @return Returns the composite of all apertures constructed
     */
    public ApertureComponent getAperture() {
        return IFUApertures;
    }
    
    /**
     * Method to get a list of the fraction of source contained in each aperture 
     * defined in the aperture composite.
     * @return List of source fractions
     */
    public List getFractionOfSourceInAperture() {
        return IFUApertures.getFractionOfSourceInAperture();
    }
    
    /**
     * Method to clear all of the source fractions from each aperture.
     */
    public void clearFractionOfSourceInAperture() {
        IFUApertures.clearFractionOfSourceInAperture();
    }
    
    /**
     * Get a list of each of the apertures and thier offsets
     * @return List of the apertures and offsets.  If the mode is single or radial then there 
     * will be one offset per aperture.  If it is summed there will be two.. one for
     * X and one for Y.
     */
    public List getApertureOffsetList() {
        return IFUOffsets;
    }
    
    /**
     * Human readable string for optical components list.
     * @return String for Optical components
     */
    public String toString() {
        return "IFU Transmission";
    }
    
}
