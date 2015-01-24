// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

/*
 * GnirsOrderSelector.java
 *
 * Created on January 14, 2004, 12:09 PM
 */

package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.TransmissionElement;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/*
 *
 * @author  bwalls
 */
public class GnirsGratingsTransmission {

    private final TransmissionElement _order1Transmission;
    private final TransmissionElement _order2Transmission;
    private final TransmissionElement _order3Transmission;
    private final TransmissionElement _order4Transmission;
    private final TransmissionElement _order5Transmission;
    private final TransmissionElement _order6Transmission;
    private final TransmissionElement _order7Transmission;
    private final TransmissionElement _order8Transmission;

    DefaultArraySpectrum test;


    /**
     * Creates a new instance of GnirsOrderSelector
     */
    public GnirsGratingsTransmission(String directory, String prefix, String suffix, String gratingOrdersTransmission) throws Exception {
        TextFileReader dfr = new TextFileReader(directory + "/" +
                prefix +
                gratingOrdersTransmission +
                suffix);

        List<Double> _wavelength1_list = new ArrayList<>();
        List<Double> _wavelength2_list = new ArrayList<>();
        List<Double> _wavelength3_list = new ArrayList<>();
        List<Double> _wavelength4_list = new ArrayList<>();
        List<Double> _wavelength5_list = new ArrayList<>();
        List<Double> _wavelength6_list = new ArrayList<>();
        List<Double> _wavelength7_list = new ArrayList<>();
        List<Double> _wavelength8_list = new ArrayList<>();
        List<Double> _order1_list = new ArrayList<>();
        List<Double> _order2_list = new ArrayList<>();
        List<Double> _order3_list = new ArrayList<>();
        List<Double> _order4_list = new ArrayList<>();
        List<Double> _order5_list = new ArrayList<>();
        List<Double> _order6_list = new ArrayList<>();
        List<Double> _order7_list = new ArrayList<>();
        List<Double> _order8_list = new ArrayList<>();

        int wavelength1 = 0;
        int wavelength2 = 0;
        int wavelength3 = 0;
        int wavelength4 = 0;
        int wavelength5 = 0;
        int wavelength6 = 0;
        int wavelength7 = 0;
        int wavelength8 = 0;
        double order1 = 0;
        double order2 = 0;
        double order3 = 0;
        double order4 = 0;
        double order5 = 0;
        double order6 = 0;
        double order7 = 0;
        double order8 = 0;

        try {
            while (true) {

                wavelength1 = dfr.readInt();
                _wavelength1_list.add(new Double(wavelength1));
                order1 = dfr.readDouble();
                _order1_list.add(new Double(order1));
                wavelength2 = dfr.readInt();
                _wavelength2_list.add(new Double(wavelength2));
                order2 = dfr.readDouble();
                _order2_list.add(new Double(order2));
                wavelength3 = dfr.readInt();
                _wavelength3_list.add(new Double(wavelength3));
                order3 = dfr.readDouble();
                _order3_list.add(new Double(order3));
                wavelength4 = dfr.readInt();
                _wavelength4_list.add(new Double(wavelength4));
                order4 = dfr.readDouble();
                _order4_list.add(new Double(order4));
                wavelength5 = dfr.readInt();
                _wavelength5_list.add(new Double(wavelength5));
                order5 = dfr.readDouble();
                _order5_list.add(new Double(order5));
                wavelength6 = dfr.readInt();
                _wavelength6_list.add(new Double(wavelength6));
                order6 = dfr.readDouble();
                _order6_list.add(new Double(order6));
                wavelength7 = dfr.readInt();
                _wavelength7_list.add(new Double(wavelength7));
                order7 = dfr.readDouble();
                _order7_list.add(new Double(order7));
                wavelength8 = dfr.readInt();
                _wavelength8_list.add(new Double(wavelength8));
                order8 = dfr.readDouble();
                _order8_list.add(new Double(order8));

            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            //normal eof
        }
        _order1Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength1_list, _order1_list));
        _order2Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength2_list, _order2_list));
        _order3Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength3_list, _order3_list));
        _order4Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength4_list, _order4_list));
        _order5Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength5_list, _order5_list));
        _order6Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength6_list, _order6_list));
        _order7Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength7_list, _order7_list));
        _order8Transmission = new TransmissionElement(new DefaultArraySpectrum(_wavelength8_list, _order8_list));

    }

    private TransmissionElement getOrder1Transmission() throws Exception {
        return _order1Transmission;
    }

    private TransmissionElement getOrder2Transmission() throws Exception {
        return _order2Transmission;
    }

    private TransmissionElement getOrder3Transmission() throws Exception {
        return _order3Transmission;
    }

    private TransmissionElement getOrder4Transmission() throws Exception {
        return _order4Transmission;
    }

    private TransmissionElement getOrder5Transmission() throws Exception {
        return _order5Transmission;
    }

    private TransmissionElement getOrder6Transmission() throws Exception {
        return _order6Transmission;
    }

    private TransmissionElement getOrder7Transmission() throws Exception {
        return _order7Transmission;
    }

    private TransmissionElement getOrder8Transmission() throws Exception {
        return _order8Transmission;
    }

    public TransmissionElement getOrderNTransmission(int order) throws Exception {
        switch (order) {
            case 1:
                return getOrder1Transmission();
            case 2:
                return getOrder2Transmission();
            case 3:
                return getOrder3Transmission();
            case 4:
                return getOrder4Transmission();
            case 5:
                return getOrder5Transmission();
            case 6:
                return getOrder6Transmission();
            case 7:
                return getOrder7Transmission();
            case 8:
                return getOrder8Transmission();
            default:
                throw new Exception("Data for Order " + order + " not found,  Please contact the Helpdesk.");
        }
    }
}
