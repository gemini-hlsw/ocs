package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TransmissionElement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GnirsGratingsTransmission {

    private static final int OrdersCnt = 8;

    private final TransmissionElement[] _orderTransmission = new TransmissionElement[OrdersCnt];

    /**
     * Creates a new instance of GnirsOrderSelector
     */
    @SuppressWarnings("unchecked")
    public GnirsGratingsTransmission(String directory, String prefix, String suffix, String gratingOrdersTransmission) throws Exception {

        final List<Double>[] _wavelength_list = (List<Double>[]) Array.newInstance(List.class, OrdersCnt);
        final List<Double>[] _order_list = (List<Double>[]) Array.newInstance(List.class, OrdersCnt);
        for (int i = 0; i < OrdersCnt; i++) {
            _wavelength_list[i] = new ArrayList<>();
            _order_list[i] = new ArrayList<>();
        }

        final String file = directory + "/" + prefix + gratingOrdersTransmission + suffix;
        try (final Scanner scan = DatFile.scan(file)) {
            while (scan.hasNext()) {
                for (int i = 0; i < OrdersCnt; i++) {
                    _wavelength_list[i].add(scan.nextDouble());
                    _order_list[i].add(scan.nextDouble());
                }
            }
        }

        for (int i = 0; i < OrdersCnt; i++) {
            _orderTransmission[i] = new TransmissionElement(new DefaultArraySpectrum(_wavelength_list[i], _order_list[i]));
        }
    }

    public TransmissionElement getOrderNTransmission(int order) {
        assert order > 0;
        assert order <= OrdersCnt;
        return _orderTransmission[order-1];
    }
}
