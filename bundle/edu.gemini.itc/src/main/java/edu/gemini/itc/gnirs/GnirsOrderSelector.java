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

import edu.gemini.itc.shared.TextFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author bwalls
 */
public class GnirsOrderSelector {

    private int _order = 0;
    private String orderFilename = "orders";
    private List _order_list;      //list of the order numbers
    private List _lambda1_list;    //start wavelenght for that order
    private List _lambda2_list;    //end wavelength fot that order

    /**
     * Creates a new instance of GnirsOrderSelector
     */
    public GnirsOrderSelector(String directory, String prefix, String suffix) throws Exception {
        TextFileReader dfr = new TextFileReader(directory + "/" +
                prefix +
                orderFilename +
                suffix);
        _order_list = new ArrayList();
        _lambda1_list = new ArrayList();
        _lambda2_list = new ArrayList();

        int order = 0;
        int lambda1 = 0;
        int lambda2 = 0;

        try {
            while (true) {
                order = dfr.readInt();
                _order_list.add(new Integer(order));
                lambda1 = dfr.readInt();
                _lambda1_list.add(new Integer(lambda1));
                lambda2 = dfr.readInt();
                _lambda2_list.add(new Integer(lambda2));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
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
