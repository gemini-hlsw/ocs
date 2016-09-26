package jsky.plot;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.ImprovedSkyCalcMethods;
import edu.gemini.spModel.core.Site;
import edu.gemini.skycalc.ImprovedSkyCalc;

import jsky.coords.WorldCoords;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * A utility class for calculating elevation vs time for a given list of target
 * positions.
 *
 * @version $Revision: 42349 $
 * @author Allan Brighton
 */
public class ElevationPlotUtil {

    public static final TimeZone UT = TimeZone.getTimeZone("UT");

    // Default number of data points in the plot
//    private static int _defaultNumSteps = 24*60/5;
    private static int _defaultNumSteps = 24*60;

    // Default number of data points in the plot
    private int _numSteps = _defaultNumSteps;

    // Number of minutes to add at each point in the plot
    private int _stepIncrement = 24*60/_numSteps;

    // The starting date of the plot
    private Date _date;

    // Describes the telescope site
    private Site _site;

    // Array of target positions to plot
    private TargetDesc[] _targets;

    // Utility object used for calculations
    private ImprovedSkyCalc _skyCalc;

    // The UT time for each target index and elevation
    private Date[][] _xData;

    // The elevation in degrees for each target index and time
    private double[][] _yData;

    // The airmass for each target index and time
    private double[][] _yDataAirmass;

    // The parallectic angle for each target index and time
    private double[][] _yDataPa;

    /**
     * Calculates the positions of the given target objects in the sky as a function
     * of time (UT), given the date and location on Earth.
     * The result is to set the values of the xData and yData arrays, to correspond
     * to the time (in UT) and elevation (in degrees). Arrays with airmass and parallactic
     * angle are also created.
     *
     * @param date (in) the date of interest
     * @param site (in) describes the observatory location
     * @param targets (in) an array describing the target objects
     */
    public ElevationPlotUtil(Date date, Site site, TargetDesc[] targets) {
        _date = date;
        _site = site;
        _targets = targets;
        _init();
    }

    // (Re)initialize the plot data.
    private void _init() {
        int numSteps = getNumSteps();
        _xData = new Date[_targets.length][numSteps];

        _yData = new double[_targets.length][numSteps];
        _yDataAirmass = new double[_targets.length][numSteps];
        _yDataPa = new double[_targets.length][numSteps];

        _skyCalc = new ImprovedSkyCalc(_site);

        // Set start of plot to noon time at the site, so night is at center
        Calendar cal = Calendar.getInstance(_site.timezone());
        cal.setTime(_date);
        cal.set(Calendar.HOUR_OF_DAY, 12); // noon
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        for (int j = 0; j < numSteps; j++) {
            for (int i = 0; i < _targets.length; i++) {
                Date utTime = cal.getTime();
                Option<WorldCoords> pos = _targets[i].getCoordinates(new Some<>(utTime.getTime()));
                _xData[i][j] = utTime;
                if (pos.isDefined()) {
                    _skyCalc.calculate(pos.getValue(), utTime, false);
                    _yData[i][j] = _skyCalc.getAltitude();
                    _yDataAirmass[i][j] = _skyCalc.getAirmass();
                    _yDataPa[i][j] = _skyCalc.getParallacticAngle();
                } // otherwise zero
            }
            cal.add(Calendar.MINUTE, _stepIncrement);
        }
    }


    /**
     * Return the number of steps to calculate for the plot.
     */
    public int getNumSteps() {
        return _numSteps;
    }

    /**
     * Set the default number of steps to calculate for the plot.
     * This is the value that will be used for future objects of this class.
     */
    public static void setDefaultNumSteps(int numSteps) {
        _defaultNumSteps = numSteps;
    }

    /** Return the airmass for the given elevation angle in degrees */
    public static double getAirmass(double elevation) {
        return ImprovedSkyCalcMethods.getAirmass(elevation);
    }

    /**
     * Return an array with the UT time for each target index and elevation
     * The first index is the target index (corresponding to the array of targets passed
     * to the constructor). The second index is 0 to NUM_STEPS-1.
     */
    public Date[][] getXData() {
        return _xData;
    }

    /**
     * Return an array with the elevation in degrees for each target index and time
     * The first index is the target index (corresponding to the array of targets passed
     * to the constructor). The second index is 0 to NUM_STEPS-1.
     */
    public double[][] getYData() {
        return _yData;
    }

    /**
     * Return an array with the airmass for each target index and time
     * The first index is the target index (corresponding to the array of targets passed
     * to the constructor). The second index is 0 to NUM_STEPS-1.
     */
    public double[][] getYDataAirmass() {
        return _yDataAirmass;
    }

    /**
     * Return an array with the parallactic angles for each target index and time
     * The first index is the target index (corresponding to the array of targets passed
     * to the constructor). The second index is 0 to NUM_STEPS-1.
     */
    public double[][] getYDataPa() {
        return _yDataPa;
    }

    /**
     * Return the LST time for the given UT time at the current site.
     */
    public Date getLst(Date date) {
        return _skyCalc.getLst(date);
    }
}
