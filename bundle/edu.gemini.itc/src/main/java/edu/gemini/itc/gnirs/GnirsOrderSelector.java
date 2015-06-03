package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.DatFile;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

public final class GnirsOrderSelector {

    private static final String orderFilename = "orders";
    private static final List<Integer> _order_list;      //list of the order numbers
    private static final List<Integer> _lambda1_list;    //start wavelenght for that order
    private static final List<Integer> _lambda2_list;    //end wavelength fot that order

    static {
        _order_list = new ArrayList<>();
        _lambda1_list = new ArrayList<>();
        _lambda2_list = new ArrayList<>();
        final String file = "/" + Gnirs.INSTR_DIR + "/" + Gnirs.INSTR_PREFIX + orderFilename + Gnirs.DATA_SUFFIX;
        try (final Scanner scan = DatFile.scanFile(file)) {
            while (scan.hasNext()) {
                _order_list.add(scan.nextInt());
                _lambda1_list.add(scan.nextInt());
                _lambda2_list.add(scan.nextInt());
            }
        }
    }

    private GnirsOrderSelector() {}

    public static int getOrder(final double centralWavelength) {

        final ListIterator<Integer> lambdaStart = _lambda1_list.listIterator();
        final ListIterator<Integer> lambdaEnd = _lambda2_list.listIterator();
        final ListIterator<Integer> order = _order_list.listIterator();

        int selectedOrder = -1;

        while (lambdaStart.hasNext() && lambdaEnd.hasNext()) {
            final int wavelenStart = lambdaStart.next();
            final int wavelenEnd = lambdaEnd.next();
            if (centralWavelength >= wavelenStart && centralWavelength <= wavelenEnd) {
                selectedOrder = order.next();
            } else {
                order.next();
            }
        }

        if (selectedOrder == -1) {
            throw new IllegalArgumentException("Order could not be found for centralWavelength" + centralWavelength);
        }

        return selectedOrder;
    }

}
