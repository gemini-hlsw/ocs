package jsky.science;

import java.util.*;

/**
 * Passband class provides support for range of wavelengths.  Including a static
 * list of standard passbands (U B V for example).
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	2000.05.03
 * @author	T. Brooks and S. Grosvenor
 *
 **/
public class Passband extends AbstractScienceObject {

    /** serial id needed to ensure backward compatibility when serialized **/
    private static final long serialVersionUID = 7848426480608091945L;

    /**
     * Wavelength marking the bottom of the Passband
     */
    private Wavelength pLow;

    /**
     * Wavelength marking the top of the Passband
     */
    private Wavelength pHigh;

    /**
     * Wavelength marking the middle of the Passband.  By default, this
     * is the average of the low and high.  But may be set to a different
     * central point at developer's discretion
     */
    private Wavelength pMiddle;

    /**
     * bound property change name for change in the LowWavelength
     */
    public static final String LOW_PROPERTY = "Low";
    /**
     * bound property change name for change in the MiddleWavelength
     */
    public static final String MIDDLE_PROPERTY = "Middle";
    /**
     * bound property change name for change in the HighWavelength
     */
    public static final String HIGH_PROPERTY = "High";

    private static Map<String, Passband> sStandardPassbands = new HashMap<>();

    static {
        sStandardPassbands.put("H-Alpha",
                               new Passband("H-Alpha",
                                            new Wavelength(645, Wavelength.NANOMETER),
                                            new Wavelength(670, Wavelength.NANOMETER),
                                            new Wavelength(658.1, Wavelength.NANOMETER)));
        sStandardPassbands.put("O III",
                               new Passband("O III",
                                            new Wavelength(495, Wavelength.NANOMETER),
                                            new Wavelength(510, Wavelength.NANOMETER),
                                            new Wavelength(502.5, Wavelength.NANOMETER)));
        sStandardPassbands.put("Ne V",
                               new Passband("Ne V",
                                            new Wavelength(338, Wavelength.NANOMETER),
                                            new Wavelength(350, Wavelength.NANOMETER),
                                            new Wavelength(343.4, Wavelength.NANOMETER)));

        // These are the standard broadbands
        sStandardPassbands.put("U",
                               new Passband("U",
                                            new Wavelength(180, Wavelength.NANOMETER),
                                            new Wavelength(350, Wavelength.NANOMETER),
                                            new Wavelength(265, Wavelength.NANOMETER)));
        sStandardPassbands.put("B",
                               new Passband("B",
                                            new Wavelength(391, Wavelength.NANOMETER),
                                            new Wavelength(489, Wavelength.NANOMETER),
                                            new Wavelength(440, Wavelength.NANOMETER)));
        sStandardPassbands.put("V",
                               new Passband("V",
                                            new Wavelength(450, Wavelength.NANOMETER),
                                            new Wavelength(750, Wavelength.NANOMETER),
                                            new Wavelength(600, Wavelength.NANOMETER)));
        sStandardPassbands.put("R",
                               new Passband("R",
                                            new Wavelength(530, Wavelength.NANOMETER),
                                            new Wavelength(720, Wavelength.NANOMETER),
                                            new Wavelength(632, Wavelength.NANOMETER)));
        sStandardPassbands.put("I",
                               new Passband("I",
                                            new Wavelength(680, Wavelength.NANOMETER),
                                            new Wavelength(980, Wavelength.NANOMETER),
                                            new Wavelength(833, Wavelength.NANOMETER)));

        //sStandardPassbands.put( "H", new Passband( "H", new Wavelength( 1600, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "J", new Passband( "J", new Wavelength( 1200, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "K", new Passband( "K", new Wavelength( 2200, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "L", new Passband( "L", new Wavelength( 3600, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "M", new Passband( "M", new Wavelength( 4800, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "N", new Passband( "N", new Wavelength( 10800, Wavelength.NANOMETER)));
        //sStandardPassbands.put( "Q", new Passband( "Q", new Wavelength( 15000, Wavelength.NANOMETER)));
    }

    /**
     * Creates a new Passband with low, high and middle values specified in Wavelengths.
     * Name is taken from first parameter
     **/
    public Passband(String inName, Wavelength inLowWL, Wavelength inHighWL, Wavelength inMiddleWL) {
        super(inName);
        pLow = inLowWL;
        pHigh = inHighWL;
        pMiddle = inMiddleWL;
    }

    /**
     * Creates a new Passband from a string specifying the range in nanometers.
     * The middle is set to the average of the low and high.  If only a low
     * is specified, middle and high values are set to the low value.
     *
     * @param val String containing the range in the form "low-high" or "low"
     * @throws NumberFormatException if it finds invalid numbers
     **/
    public Passband(String val) throws NumberFormatException {
        super(val);
        double low;
        double middle;
        double high;

        Passband findBand = sStandardPassbands.get(val);

        if (findBand != null) {
            pLow = findBand.getLowWavelength();
            pMiddle = findBand.getMiddleWavelength();
            pHigh = findBand.getHighWavelength();
        } else {
            // input name is not in standard list, parse it for min/max numbers
            int locDash = val.indexOf('-');

            if (locDash >= 0) {
                // has dash is this a negative exponent?
                if (locDash > 0) {
                    String lowStr = (val.substring(0, locDash)).trim();
                    String highStr = (val.substring(locDash + 1)).trim();

                    if (lowStr.endsWith("e") || lowStr.endsWith("E")) {
                        // have dash immediately after an exponent, assume is minus for exponent
                        int nextDash = highStr.indexOf('-');
                        locDash = (nextDash >= 0) ? locDash + nextDash + 1 : -1;
                    }
                }
            }

            if (locDash >= 0) {
                // has a dash, assume is 'low-high'
                String lowStr = (val.substring(0, locDash)).trim();
                String highStr = (val.substring(locDash + 1)).trim();

                if (lowStr.length() == 0) {
                    lowStr = "0";
                }

                if (highStr.length() == 0) {
                    highStr = "10000";
                }
                low = new Double(lowStr);
                high = new Double(highStr);
                middle = (high - low) / 2;
            } else {
                // no dash, assume a single value
                low = new Double(val);
                middle = low;
                high = low;
            }
            pLow = new Wavelength(low);
            pMiddle = new Wavelength(middle);
            pHigh = new Wavelength(high);
        }
    }

    public boolean equals(Object inO) {
        if (!super.equals(inO)) return false;

        Passband that = (Passband) inO;

        if ((pLow == null) ? (that.pLow != null) : !(pLow.equals(that.pLow))) return false;
        if ((pHigh == null) ? (that.pHigh != null) : !(pHigh.equals(that.pHigh))) return false;
        if ((pMiddle == null) ? (that.pMiddle != null) : !(pMiddle.equals(that.pMiddle))) return false;
        return true;
    }


    /**
     * Returns String array containing the names of the defined standard passbands
     **/
    public static String[] getStdBands() {
        Iterator<String> iter = sStandardPassbands.keySet().iterator();
        String[] bandList = new String[sStandardPassbands.size()];

        for (int i = 0; i < sStandardPassbands.size(); i++)
            bandList[i] = iter.next();

        return bandList;
    }

    /**
     * Returns array of Strings containing (arbitrarily) only the names of
     * Passbands where the length of the name is a single character
     **/
    public static String[] getStandardBroadBands() {
        Iterator<String> iter = sStandardPassbands.keySet().iterator();
        int len = 0;

        while (iter.hasNext()) {
            String b = iter.next();
            if (b.length() == 1) len++;
        }
        String[] bandList = new String[len];

        iter = sStandardPassbands.keySet().iterator();
        len = 0;
        while (iter.hasNext()) {
            String b = iter.next();
            if (b.length() == 1) bandList[len++] = b;
        }

        return bandList;
    }

    /**
     * Replaces the standard pass band list with elements parsed from the
     * specified array of strings.  Each string is assumed to contain a
     * passband parsable by the constructor new Passband( String).
     **/
    public static void setStdBands(String[] bands) {
        sStandardPassbands.clear();
        for (int i = 0; i < bands.length; i += 2) {
            Passband newP = new Passband(bands[i + 1]);
            newP.setName(bands[i]);
            sStandardPassbands.put(bands[i], newP);
        }
    }

    /**
     * Returns the Passband in the standard list by name.  Returns null if bandname is not found
     **/
    public static Passband getStandardPassband(String pName) {
        return sStandardPassbands.get(pName);
    }

    /**
     * Returns the first standard passband that contains the
     * specified wavelength.  Returns null if no match is found.
     **/
    public static Passband findStandardContaining(Wavelength inWL) {
        for (Passband p : sStandardPassbands.values()) {
            if (p.contains(inWL)) return p;
        }
        return null;
    }

    /**
     * Returns true if passband contains specified wavelength
     **/
    public boolean contains(Wavelength inWL) {
        double inNano = inWL.getValue(Wavelength.NANOMETER);
        return ((pLow.getValue(Wavelength.NANOMETER) <= inNano) &&
                (pHigh.getValue(Wavelength.NANOMETER) >= inNano));
    }

    /**
     * Returns high end of the passband as a Wavelength
     **/
    public Wavelength getHighWavelength() {
        return pHigh;
    }

    /**
     * Sets the Wavelength marking the high end of the Passband.
     * @param inWL middle value as a Wavelength
     **/
    public void setHighWavelength(Wavelength inWL) {
        Wavelength oldVal = pHigh;
        pHigh = inWL;
        firePropertyChange(HIGH_PROPERTY, oldVal, pHigh);
    }

    /**
     * Returns low end of the passband as a Wavelength
     **/
    public Wavelength getLowWavelength() {
        return pLow;
    }


    /**
     * Sets the Wavelength marking the low end of the Passband.
     * @param inWL middle value as a Wavelength
     **/
    public void setLowWavelength(Wavelength inWL) {
        Wavelength oldVal = pLow;
        pLow = inWL;
        firePropertyChange(LOW_PROPERTY, oldVal, pLow);
    }

    /**
     * Returns middle or central Wavelength.
     **/
    public Wavelength getMiddleWavelength() {
        return pMiddle;
    }

    /**
     * Sets the Wavelength marking the middle of the Passband.  By default, this
     * is the average of the low and high.  But may be set to a different
     * central point at developer's discretion
     * @param inWL middle value as a Wavelength
     **/
    public void setMiddleWavelength(Wavelength inWL) {
        Wavelength oldval = pMiddle;
        pMiddle = inWL;
        firePropertyChange(MIDDLE_PROPERTY, oldval, pMiddle);
    }

}
