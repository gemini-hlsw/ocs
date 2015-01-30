package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * Neutral density color wheel?
 * This class exists so that the client can specify a ND filter number
 * instead of specifying the data file name specifically.
 */
public class NDFilterWheel extends TransmissionElement {
    private static final String FILENAME = AcquisitionCamera.getPrefix() + "ndfilt_";

    private final String _ndFilter;

    /**
     * @param ndFilter Should be one of <ul>
     *                 <li> clear
     *                 <li> NDa </li>
     *                 <li> NDb </li>
     *                 <li> NDc </li>
     *                 <li> NDd </li>
     *                 </ul>
     */
    public NDFilterWheel(String ndFilter, String dir) {
        super(dir + FILENAME + ndFilter + Instrument.getSuffix());
        _ndFilter = ndFilter;
    }

    public String toString() {
        return "Neutral Density Filter - " + _ndFilter;
    }
}
