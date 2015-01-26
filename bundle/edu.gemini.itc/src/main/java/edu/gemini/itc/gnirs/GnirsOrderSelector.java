package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.DatFile;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

public class GnirsOrderSelector {

    private final String orderFilename = "orders";
    private final List<Integer> _order_list;      //list of the order numbers
    private final List<Integer> _lambda1_list;    //start wavelenght for that order
    private final List<Integer> _lambda2_list;    //end wavelength fot that order

    /**
     * Creates a new instance of GnirsOrderSelector
     */
    public GnirsOrderSelector(String directory, String prefix, String suffix) throws Exception {
        final String file = directory + "/" + prefix + orderFilename + suffix;
        try (final Scanner scan = DatFile.scan(file)) {
            _order_list = new ArrayList<>();
            _lambda1_list = new ArrayList<>();
            _lambda2_list = new ArrayList<>();

            while (scan.hasNext()) {
                _order_list.add(scan.nextInt());
                _lambda1_list.add(scan.nextInt());
                _lambda2_list.add(scan.nextInt());
            }
        }
    }

    private int findOrder(double centralWavelength) throws Exception {

        int centralWavelengthInt = (new Double(centralWavelength)).intValue();
        ListIterator lambdaStart = _lambda1_list.listIterator();
        ListIterator lambdaEnd = _lambda2_list.listIterator();
        ListIterator order = _order_list.listIterator();

        int selectedOrder = -1;

        while (lambdaStart.hasNext() && lambdaEnd.hasNext()) {
            int wavelenStart = ((Integer) lambdaStart.next()).intValue();
            int wavelenEnd = ((Integer) lambdaEnd.next()).intValue();
            if (centralWavelength >= wavelenStart && centralWavelength <= wavelenEnd) {
                selectedOrder = ((Integer) order.next()).intValue();
            } else {
                order.next();
            }
        }

        if (selectedOrder == -1) {
            throw new Exception("Order could not be found for centralWavelength" + centralWavelength);
        }

        return selectedOrder;
    }

    int getOrder(double centralWavelength) throws Exception {
        return findOrder(centralWavelength);
    }

}
