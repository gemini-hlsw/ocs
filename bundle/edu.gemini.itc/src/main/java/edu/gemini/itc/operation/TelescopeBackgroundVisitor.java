// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: TelescopeBackgroundVisitor.java,v 1.5 2004/01/12 16:53:55 bwalls Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.*;

/**
 * The TelescopeBackgroundVisitor class is designed to adjust the SED for the
 * background given off by the telescope.
 */
public class TelescopeBackgroundVisitor implements SampledSpectrumVisitor {
    private String _setup;
    private String _filename_base;

    private ArraySpectrum _telescopeBack = null;

    /**
     * Constructs TelescopeBackgroundVisitor with specified port and coating.
     * We will use a different background file for different
     * ports and coatings.
     */
    public TelescopeBackgroundVisitor(String coating, String port, String site, String wavelenRange) throws Exception {
        /*if (port.equals("up")){
            _setup = "_2";
            if (coating.equals("aluminium"))
                _setup= _setup + "al_ph";
            else if (coating.equals("silver"))
                _setup = _setup +"ag_ph";
        } else if (port.equals("side")&&coating.equals("silver"))
            _setup= "_3ag_ph";
        else _setup = "_2al+1ag_ph";
         */


        /********TEST*********

         if (port.equals("up")){
         _setup = "_2";
         if (coating.equals("aluminium")){
         _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup= _setup + "al_ph";
         }
         else if (coating.equals("silver")){
         _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = _setup +"ag_ph";}
         } else if (port.equals("side")&&coating.equals("silver")){
         _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup= "_3ag_ph";
         } else if (port.equals("side")&&coating.equals("aluminium")){
         _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = "_2al+1ag_ph";
         } else if (port.equals("upGS")&&coating.equals("silver")){
         _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = "_ag1_al1";
         } else if (port.equals("upGS")&&coating.equals("aluminium")){
         _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = "_al2";
         } else if (port.equals("sideGS")&&coating.equals("silver")){
         _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = "_ag1_al2";
         } else if (port.equals("sideGS")&&coating.equals("aluminium")){
         _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
         _setup = "_al3";
         }

         _telescopeBack = new DefaultArraySpectrum(
         ITCConstants.TELESCOPE_BACKGROUND_LIB + "/" +
         _filename_base
         + _setup +
         ITCConstants.DATA_SUFFIX);
         //System.out.println("Telescope Back ave: " +_telescopeBack.getAverage());
         *
         *
         *****/

        this(coating, port, "", site, wavelenRange);
    }

    public TelescopeBackgroundVisitor(String coating, String port, String filename_base, String site, String wavelenRange) throws Exception {

        String _fullBackgroundResource;
         /*
        if (filename_base.equals(ITCConstants.MID_IR_TELESCOPE_BACKGROUND_FILENAME_BASE)) {
            _filename_base = filename_base;
            
            if (coating.equals("aluminium")) {
                _setup = "Emiss_10pc";
            } else if (coating.equals("silver")) {
                _setup = "Emiss_6pc";
            }
        } else {
            _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
            if (port.equals("up")){
                _setup = "_2";
                if (coating.equals("aluminium"))
                    _setup= _setup + "al_ph";
                else if (coating.equals("silver"))
                    _setup = _setup +"ag_ph";
            } else if (port.equals("side")&&coating.equals("silver")){
                _setup= "_3ag_ph";
            } else if (port.equals("side")&&coating.equals("aluminium")){
                _setup = "_2al+1ag_ph";
            } else if (port.equals("upGS")&&coating.equals("silver")){
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al1";
            } else if (port.equals("upGS")&&coating.equals("aluminium")){
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al2";
            } else if (port.equals("sideGS")&&coating.equals("silver")){
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al2";
            } else if (port.equals("sideGS")&&coating.equals("aluminium")){
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al3";
            }
            
        }
        */
        if (!wavelenRange.equals(ITCConstants.VISIBLE)) {

            String _filename_base_test = "/HI-Res/" + site + wavelenRange + ITCConstants.TELESCOPE_BACKGROUND_LIB + "/"
                    + ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;

            if (port.equals("up")) {
                _setup = "_2";
                if (coating.equals("aluminium"))
                    _setup = _setup + "al";
                else if (coating.equals("silver"))
                    _setup = _setup + "ag";
            } else if (port.equals("side") && coating.equals("silver")) {
                _setup = "_3ag";
            } else if (port.equals("side") && coating.equals("aluminium")) {
                _setup = "_2al+1ag";
            }

            //System.out.println("new filename: "+ _filename_base_test + _setup);
            _fullBackgroundResource = _filename_base_test + _setup + ITCConstants.DATA_SUFFIX;

        } else {
            _filename_base = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
            if (port.equals("up")) {
                _setup = "_2";
                if (coating.equals("aluminium"))
                    _setup = _setup + "al_ph";
                else if (coating.equals("silver"))
                    _setup = _setup + "ag_ph";
            } else if (port.equals("side") && coating.equals("silver")) {
                _setup = "_3ag_ph";
            } else if (port.equals("side") && coating.equals("aluminium")) {
                _setup = "_2al+1ag_ph";
            } else if (port.equals("upGS") && coating.equals("silver")) {
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al1";
            } else if (port.equals("upGS") && coating.equals("aluminium")) {
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al2";
            } else if (port.equals("sideGS") && coating.equals("silver")) {
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al2";
            } else if (port.equals("sideGS") && coating.equals("aluminium")) {
                _filename_base = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al3";
            }

            //System.out.println("new vis: "+ ITCConstants.TELESCOPE_BACKGROUND_LIB + "/" +
            //  		_filename_base
            //                + _setup +
            //              ITCConstants.DATA_SUFFIX);
            _fullBackgroundResource = ITCConstants.TELESCOPE_BACKGROUND_LIB + "/" +
                    _filename_base
                    + _setup +
                    ITCConstants.DATA_SUFFIX;
        }

        //_telescopeBack = new DefaultArraySpectrum(
        //ITCConstants.TELESCOPE_BACKGROUND_LIB + "/" +
        //_filename_base
        //+ _setup +
        //ITCConstants.DATA_SUFFIX);

        _telescopeBack = new DefaultArraySpectrum(_fullBackgroundResource);


    }


    /**
     * @return the airmass used by this calculation
     */
    public String getSetup() {
        return _setup;
    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) throws Exception {
        for (int i = 0; i < sed.getLength(); i++) {
            //System.out.println("X: "+sed.getX(i)+" teleback: "+_telescopeBack.getY(sed.getX(i))+" sed: "+sed.getY(i));
            sed.setY(i, _telescopeBack.getY(sed.getX(i)) + sed.getY(i));
            //System.out.println("X: "+sed.getX(i)+" teleback: "+_telescopeBack.getY(sed.getX(i))+" sed: "+sed.getY(i));
        }
    }


    public String toString() {
        return "TelescopeBackgroundVisitor using setup " + _setup;
    }
}
