package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TransmissionElement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Singleton for accessing GNIRS grating transmission files.
 */
public final class GnirsGratingsTransmission {

    private static final int OrdersCnt = 8;

    private static final HashMap<String, TransmissionElement[]> _orderTransmission = new HashMap<>();

    private GnirsGratingsTransmission() {}

    public static synchronized TransmissionElement getOrderNTransmission(final String grating, final int order) {
        assert order > 0;
        assert order <= OrdersCnt;
        if (!_orderTransmission.containsKey(grating)) {
            loadTransmission(grating);
        }
        return _orderTransmission.get(grating)[order-1];
    }

    @SuppressWarnings("unchecked")
    private static void loadTransmission(final String grating) {
        final TransmissionElement[] tes = new TransmissionElement[OrdersCnt];
        final List<Double>[] wavel = (List<Double>[]) Array.newInstance(List.class, OrdersCnt);
        final List<Double>[] order = (List<Double>[]) Array.newInstance(List.class, OrdersCnt);
        for (int i = 0; i < OrdersCnt; i++) {
            wavel[i] = new ArrayList<>();
            order[i] = new ArrayList<>();
        }

        final String file = "/" + Gnirs.INSTR_DIR + "/" + Gnirs.getPrefix() + grating + Gnirs.DATA_SUFFIX;
        try (final Scanner scan = DatFile.scan(file)) {
            while (scan.hasNext()) {
                for (int i = 0; i < OrdersCnt; i++) {
                    wavel[i].add(scan.nextDouble());
                    order[i].add(scan.nextDouble());
                }
            }
        }

        for (int i = 0; i < OrdersCnt; i++) {
            tes[i] = new TransmissionElement(new DefaultArraySpectrum(wavel[i], order[i]));
        }

        _orderTransmission.put(grating, tes);
    }

}
