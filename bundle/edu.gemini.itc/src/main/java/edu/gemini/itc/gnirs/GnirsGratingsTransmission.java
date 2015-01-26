package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.TransmissionElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GnirsGratingsTransmission {

    private final TransmissionElement _order1Transmission;
    private final TransmissionElement _order2Transmission;
    private final TransmissionElement _order3Transmission;
    private final TransmissionElement _order4Transmission;
    private final TransmissionElement _order5Transmission;
    private final TransmissionElement _order6Transmission;
    private final TransmissionElement _order7Transmission;
    private final TransmissionElement _order8Transmission;

    /**
     * Creates a new instance of GnirsOrderSelector
     */
    public GnirsGratingsTransmission(String directory, String prefix, String suffix, String gratingOrdersTransmission) throws Exception {

        final List<Double> _wavelength1_list = new ArrayList<>();
        final List<Double> _wavelength2_list = new ArrayList<>();
        final List<Double> _wavelength3_list = new ArrayList<>();
        final List<Double> _wavelength4_list = new ArrayList<>();
        final List<Double> _wavelength5_list = new ArrayList<>();
        final List<Double> _wavelength6_list = new ArrayList<>();
        final List<Double> _wavelength7_list = new ArrayList<>();
        final List<Double> _wavelength8_list = new ArrayList<>();
        final List<Double> _order1_list = new ArrayList<>();
        final List<Double> _order2_list = new ArrayList<>();
        final List<Double> _order3_list = new ArrayList<>();
        final List<Double> _order4_list = new ArrayList<>();
        final List<Double> _order5_list = new ArrayList<>();
        final List<Double> _order6_list = new ArrayList<>();
        final List<Double> _order7_list = new ArrayList<>();
        final List<Double> _order8_list = new ArrayList<>();

        final String file = directory + "/" + prefix + gratingOrdersTransmission + suffix;
        try (final Scanner scan = DatFile.scan(file)) {
            while (scan.hasNext()) {
                _wavelength1_list.add(scan.nextDouble());
                _order1_list.add(scan.nextDouble());
                _wavelength2_list.add(scan.nextDouble());
                _order2_list.add(scan.nextDouble());
                _wavelength3_list.add(scan.nextDouble());
                _order3_list.add(scan.nextDouble());
                _wavelength4_list.add(scan.nextDouble());
                _order4_list.add(scan.nextDouble());
                _wavelength5_list.add(scan.nextDouble());
                _order5_list.add(scan.nextDouble());
                _wavelength6_list.add(scan.nextDouble());
                _order6_list.add(scan.nextDouble());
                _wavelength7_list.add(scan.nextDouble());
                _order7_list.add(scan.nextDouble());
                _wavelength8_list.add(scan.nextDouble());
                _order8_list.add(scan.nextDouble());
            }
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
