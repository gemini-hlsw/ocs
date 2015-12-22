package edu.gemini.spModel.gemini.bhros.ech;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * UI-independent representation of a bHROS Echellogram. Given a target wavelength and 
 * centreX/Y positions, this class produces the data points that can be used to draw
 * an Echellogram. The class is immutable; that is, you construct an instance and just
 * read its properties.
 * <p>
 * Note that fairly large chunks of this class were adapted from the orginal C code, and
 * have been left in a form that will make it easier to return to the original C for
 * comparison as needed. This need will go away once the instrument has been in use for
 * a while.
 * @author rnorris
 */
public class Echellogram implements HROSHardwareConstants {

    private static final Logger LOGGER = Logger.getLogger(Echellogram.class.getName());
    private static final int POINTSPERORDER = 6;

    private static final Matrix SYSTEM_DATA = new Matrix(11, 7);
    static {
        SYSTEM_DATA.read(Echellogram.class.getResourceAsStream("/resources/conf/system.dat"));
    }

    private final OrderData[] echellePlotData = new OrderData[SpectralOrder.ELEMENTS.length];
    private final double wavelength;
    private int cenOrder;
    private final double centreXpos;
    private final double centreYpos;
    private final int minOrder, maxOrder;
    private final double minWavelength, maxWavelength;

    // TODO: all should be final, but the C code is a little convoluted and I didn't
    // get around to pulling all the assignments up into the constructor. These don't
    // change after the constructor has run.
    private double echAlt;
    private double echAz;
    private double goniAng;
    private final Set<Integer> gapOrders;

    /**
     * ADT for plotted data representing a single spectral order.
     */
    public static class OrderData {
        public int orderNumber;
        public int numPoints;
        public double[] xpos = new double[POINTSPERORDER];
        public double[] ypos = new double[POINTSPERORDER];
        public double fsrLimitLow;
        public double fsrLimitHigh;
    }

    public OrderData[] getOrderData() {
        return echellePlotData;
    }

    public Echellogram(double wavelength, double centreXpos, double centreYpos, double goniOffset) {

        this.wavelength = wavelength;
        this.centreXpos = centreXpos;
        this.centreYpos = centreYpos;

        // Initialize order records with high and low limits, and find the order
        // on which we're focusing.
        double lastCo = 0.0;
        for (int index = 0; index < SpectralOrder.ELEMENTS.length; index++) {
            int order = SpectralOrder.ELEMENTS[index].order - 1;
            double changeOver = SpectralOrder.ELEMENTS[index].changeOver;
            if (index > 0) {
                echellePlotData[index] = new OrderData();
                echellePlotData[index].fsrLimitHigh = lastCo;
                echellePlotData[index].fsrLimitLow = changeOver;
                if (wavelength <= lastCo && wavelength >= changeOver)
                    cenOrder = order;
            }
            lastCo = changeOver;
        }

        long start = System.currentTimeMillis();

        plotEchelle(centreXpos, centreYpos, cenOrder, wavelength, goniOffset);

        // Some delightful call-by-reference code for your enjoyment.
        int[] maxorder = { 0 };
        int[] minorder = { 0 };
        double[] maxwavel = { 0 };
        double[] minwavel = { 0 };
        Set<Integer> gapOrders = new TreeSet<>();
        getChipLimits(echAlt, echAz, goniAng, maxorder, maxwavel, minorder, minwavel, gapOrders);
        this.minOrder = minorder[0];
        this.maxOrder = maxorder[0];
        this.maxWavelength = maxwavel[0];
        this.minWavelength = minwavel[0];
        this.gapOrders = gapOrders;

        LOGGER.fine("Update took " + (System.currentTimeMillis() - start) + "ms.");

    }

    public static int getOrder(double wavelength) {

        // Initialize order records with high and low limits, and find the order
        // on which we're focusing.
        double lastCo = 0.0;
        for (int index = 0; index < SpectralOrder.ELEMENTS.length; index++) {
            int order = SpectralOrder.ELEMENTS[index].order - 1;
            double changeOver = SpectralOrder.ELEMENTS[index].changeOver;
            if (index > 0) {
                if (wavelength <= lastCo && wavelength >= changeOver)
                    return order;
            }
            lastCo = changeOver;
        }

        throw new IllegalArgumentException("Wavelength out of range: " + wavelength);

    }

    private Matrix image_mid = new Matrix(2, 1);
    private Matrix image_top = new Matrix(2, 1);
    private Matrix image_bot = new Matrix(2, 1);

    private void plotEchelle(double centreXpos, double centreYpos, int cenOrder, double wavelength, double goniOffset) {

        SpectralOrder[] orderData = SpectralOrder.ELEMENTS;

        double[] settings = BRayLib.echellePos(wavelength, cenOrder, centreXpos, centreYpos, goniOffset);

        double Ech_alt = settings[0];
        double Ech_az = settings[1];
        double GoniAng = settings[2];

        for (int index = 2; index <= orderData.length - 3; index++) {

            int order = orderData[index].order - 1;
            double FSRlow = echellePlotData[index].fsrLimitLow;
            double FSRhi = echellePlotData[index].fsrLimitHigh;
            double FSRange = FSRhi - FSRlow;
            int point = 0;
            for (double wavel = FSRlow; wavel <= FSRhi + 0.0001; wavel += (FSRange / 5.0)) {

                BRayLib.b_ray2(SYSTEM_DATA, 1, GoniAng, order, wavel, Ech_alt, Ech_az, image_mid, image_top, image_bot);

                echellePlotData[index].xpos[point] = image_mid.cell(0, 0);
                echellePlotData[index].ypos[point] = image_mid.cell(1, 0);
                point++;

            }

            echellePlotData[index].orderNumber = order;
            echellePlotData[index].numPoints = point;

        }

        echAlt = Ech_alt;
        echAz = Ech_az;
        goniAng = GoniAng;

    }

    public double getEchAlt() {
        return echAlt;
    }

    public double getEchAz() {
        return echAz;
    }

    public double getGoniAng() {
        return goniAng;
    }

    public int getMaxOrder() {
        return maxOrder;
    }

    public Set<Integer> getGapOrders() {
        return Collections.unmodifiableSet(gapOrders);
    }

    public double getMaxWavelength() {
        return maxWavelength;
    }

    public int getMinOrder() {
        return minOrder;
    }


    public double getMinWavelength() {
        return minWavelength;
    }

    public double getWavelength() {
        return wavelength;
    }

    public int getOrder() {
        return cenOrder;
    }


    /**
     * Get the maximum and minimum order & wavelength that are currently on the chips.
     * Also get the order(s) that might be in the inter-chip gap
     */
    private void getChipLimits(double ech_alt, double ech_az, double goniAng, int[] maxorder,
            double[] maxwavel, int[] minorder, double[] minwavel, Set<Integer> gapOrders) {

        double xcen = 0, ycen = 0;
        int j, points, index, order, midOrder;
        double topRightY, lowerLeftY, xpos, ypos;
        double plotXhi, plotXlow, plotYhi, plotYlow;
        double cenWavelength;
        double Maxwavel = 0.0;
        double Minwavel = 100000.0;
        double[] hiEdgeWavel = { 0 }, loEdgeWavel = { 0 }, yEdgePos = { 0 };
        int Maxorder = 0;
        int Minorder = 10000;
        double minY, maxY; /* Min/Max calculated Y values along a selected order */

        /* TR and LL chip corner co-ordinates have the extremes of spectral range covered */
        topRightY = ycen + CHIP_GAP / 2.0 + RED_CHIP_YSIZE;
        lowerLeftY = ycen - CHIP_GAP / 2.0 - BLUE_CHIP_YSIZE;

        /* Scan DOWN the echellogram, trying to guess the first on-chip order */
        for (j = 1; j < echellePlotData.length; j++) {
            /* Compute this order's mid-point Y value and check whether its on-chip */
            if (echellePlotData[j].numPoints > 1) {
                points = echellePlotData[j].numPoints;
                plotXlow = echellePlotData[j].xpos[0];
                plotXhi = echellePlotData[j].xpos[points - 1];
                plotYlow = echellePlotData[j].ypos[0];
                plotYhi = echellePlotData[j].ypos[points - 1];
                minY = plotYhi > plotYlow ? plotYlow : plotYhi;
                if ((minY < topRightY) && ((plotXlow <= xcen + CHIP_XSIZE / 2.0) || /* Ensure at least one */
                    (plotXhi >= xcen - CHIP_XSIZE / 2.0))) { 						/* end is on the chip */
                    break;
                }
            }
        }

        /*
         * It's on chip (possibly) :  scan through this order and +/-3 adjacent to deal
         * with slopes. Calculate wavelength at RH edge of chip  and save max. wavelength
         * and order
         */
        for (index = (j > 3 ? j - 3 : 2); index < (j < echellePlotData.length - 2 ? j + 3 : echellePlotData.length); index++) {
            if (echellePlotData[index].numPoints > 1) {
                order = echellePlotData[index].orderNumber;
                cenWavelength = (echellePlotData[index].fsrLimitLow + echellePlotData[index].fsrLimitHigh) / 2.0;
                try {
                    BRayLib.Xwavelength(order, cenWavelength, CHIP_XSIZE / 2.0, ech_alt, ech_az, goniAng, hiEdgeWavel, yEdgePos);
                    if (hiEdgeWavel[0] > 0.2 && hiEdgeWavel[0] < 1.3) {
                        if (hiEdgeWavel[0] > Maxwavel) {
                            Maxwavel = hiEdgeWavel[0];
                            Minorder = order;
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    // this is ok.
                }
            }
        }


        // Scan UP the echellogram, trying to guess the first on-chip order
        for (j = echellePlotData.length-1; j >= 0; j--) {
            /* Compute this order's mid-point Y value and check whether its on-chip */
            if (echellePlotData[j].numPoints > 1) {
                points = echellePlotData[j].numPoints;
                plotXlow = echellePlotData[j].xpos[0];
                plotXhi = echellePlotData[j].xpos[points - 1];
                plotYlow = echellePlotData[j].ypos[0];
                plotYhi = echellePlotData[j].ypos[points - 1];
                maxY = plotYhi > plotYlow ? plotYhi : plotYlow;
                if ((maxY > lowerLeftY) && ((plotXlow <= xcen + CHIP_XSIZE / 2.0) || /* Ensure at least one */
                        (plotXhi >= xcen - CHIP_XSIZE / 2.0))) { 					 /* end is on the chip */
                    break;
                }
            }
        }


        /* Now scan through this order and +2/-14 adjacent to deal with slopes.
         Calculate wavelength at LH edge of chip  and save min. wavelength and order   */
        for (index = (j > 15 ? j - 14 : 2); index < (j < echellePlotData.length - 2 ? j + 2 : echellePlotData.length); index++) {
            if (echellePlotData[index].numPoints > 1) {
                order = echellePlotData[index].orderNumber;
                cenWavelength = (echellePlotData[index].fsrLimitLow + echellePlotData[index].fsrLimitHigh) / 2.0;
                try {
                    BRayLib.Xwavelength(order, cenWavelength, -CHIP_XSIZE / 2.0, ech_alt, ech_az, goniAng, loEdgeWavel, yEdgePos);
                    if (loEdgeWavel[0] > 0.2 && loEdgeWavel[0] < 1.3) {
                        if (loEdgeWavel[0] < Minwavel) {
                            Minwavel = loEdgeWavel[0];
                            Maxorder = order;
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    // this is ok
                }
            }
        }


        /* Search for order(s) in the inter-chip gap. First locate the 'centre' order */
//		Gapcount = 0;
        midOrder = (Maxorder + Minorder) / 2;
        for (j = 1; j < echellePlotData.length; j++)
            if (echellePlotData[j].numPoints > 1 && echellePlotData[j].orderNumber == midOrder)
                break;

        /* Now scan through this order and +/- 3 adjacent to deal with non-uniform spacing.
         Interpolate, check each point to see if it is in the gap?   */
        for (index = (j > 3 ? j - 3 : 2); index < (j < echellePlotData.length - 2 ? j + 3 : echellePlotData.length); index++) {
            if (echellePlotData[index].numPoints > 1) {
                points = echellePlotData[index].numPoints;
                order = echellePlotData[index].orderNumber;
                plotXlow = echellePlotData[index].xpos[0];
                plotXhi = echellePlotData[index].xpos[points - 1];
                plotYlow = echellePlotData[index].ypos[0];
                plotYhi = echellePlotData[index].ypos[points - 1];

                /* Compute for 200 points along each order, interpolating Y, check in gap and if so,
                 remember this order number */
                for (xpos = plotXlow + 1e-8; xpos <= plotXhi - 1e-8; xpos += ((plotXhi - plotXlow) / 201.0)) {
                    ypos = BRayLib.interp(plotXlow, plotYlow, plotXhi, plotYhi, xpos);
                    if (onChip(xcen, ycen, xpos, ypos) == -1) {
                        gapOrders.add(order);
                        break;
                    }
                }
            }
        }

        /* Set output values */
        minorder[0] = Minorder;
        maxwavel[0] = Maxwavel;
        maxorder[0] = Maxorder;
        minwavel[0] = Minwavel;

    }


    /* Returns 1 if supplied (X,Y) is on the chip(s), 0 if not and -1
     if inside the inter-chip gap */
    private int onChip(double xcen, double ycen, double x, double y) {
        if ((Math.abs(x - xcen) <= CHIP_XSIZE / 2.0) && (Math.abs(y - ycen) < CHIP_GAP))
            return (-1);
        else if ((Math.abs(x - xcen) <= CHIP_XSIZE / 2.0) && (y >= -BLUE_CHIP_YSIZE) && (y <= RED_CHIP_YSIZE))
            return (1);
        else
            return (0);
    }

    public double getCentreXpos() {
        return centreXpos;
    }


    public double getCentreYpos() {
        return centreYpos;
    }

}
