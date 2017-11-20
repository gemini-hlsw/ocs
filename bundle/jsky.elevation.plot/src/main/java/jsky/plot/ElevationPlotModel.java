package jsky.plot;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.ImprovedSkyCalcMethods;
import edu.gemini.skycalc.Interval;
import edu.gemini.skycalc.SunRiseSet;
import edu.gemini.skycalc.Union;
import edu.gemini.spModel.core.Site;
import jsky.plot.util.CalendarUtil;
import jsky.util.Preferences;
import jsky.util.StringUtil;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;

import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A model class for plotting elevation vs local sidereal time for a given list of target
 * positions. This class can be used to display the graph using JFreeChart.
 */
public class ElevationPlotModel {

    /** Constant for local sidereal time */
    public static final String LST = "LST";

    /** Constant for UT time */
    public static final String UT = "UT";

    /** Constant for telescope site time */
    public static final String SITE_TIME = "Site Time";

    // Keys for saving and restoring user settings between sessions
    static final String SITE_PREF_KEY = ElevationPlotModel.class.getName() + ".site";
    static final String TIMEZONE_DISPLAY_NAME_PREF_KEY = ElevationPlotModel.class.getName() + ".timeZoneDisplayName";
    static final String TIMEZONE_ID_PREF_KEY = ElevationPlotModel.class.getName() + ".timeZoneId";

    // The name and location of the observatory site
    private Site _site;

    // The starting date for the plot
    private Date _startDate;

    // The end date for the plot
    private Date _endDate;

    // The target objects, whose elevations are being plotted
    private TargetDesc[] _targets;

    // the X values are UT or LST dates
    private Date[][] _xDate;

    // Used to format the dates in the table
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // Used to format titles.
    private static final SimpleDateFormat TITLE_FORMAT = new SimpleDateFormat("yyyy MMM d");

    // the Y values are elevation (angle between 0 and 90 degrees)
    private double[][] _yData;

    // the secondary Y axis values are airmass
    private double[][] _yDataAirmass;

    // the other secondary Y axis values are the parallactic angles
    private double[][] _yDataPa;

    // For each target index, contains the approximate max elevation
    private double[] _maxElevation;

    // For each target index, contains the approximate time of the max elevation in ms
    private double[] _maxElevationTime;

    // The time zone display name to use for the X values
    private String _timeZoneDisplayName = UT;

    // The time zone id to use for the X values
    private String _timeZoneId = UT;

    // The time zone for the X values
    private TimeZone _timeZone = TimeZone.getTimeZone(UT);

    // list of listeners for change events
    private EventListenerList _listenerList = new EventListenerList();

    // An array of table models corresponding to the targets
    private ElevationPlotTableModel[] _tableModels;

    // Threshold in degrees above the horizon where targets are considered observable
    private static double _obsThreshold = 30.0;

    // Utility class responsible for elevation calculations
    private ElevationPlotUtil _plotUtil;

    // Utility class responsible for sunrise/sunset/twilight calculations
    private SunRiseSet _sunRiseSet;


    /**
     * Initialize an elevation plot model for the given date, location, and target coordinates.
     *
     * @param site the name and location of the observatory site
     * @param date the date for which the plot should be calculated
     * @param targets an array of target object descriptions
     * @param timeZoneDisplayName the display name for the time zone for the X axis
     * @param timeZoneId the time zone id for the X axis (one of "UT", "LST", or some standard time zone id)
     */
    public ElevationPlotModel(Site site, Date date, TargetDesc[] targets,
                              String timeZoneDisplayName, String timeZoneId) {
        _setSite(site);
        _setDate(date);
        _setTargets(targets);
        _setTimeZone(timeZoneDisplayName, timeZoneId);

        _updateModel();
    }

    /**
     * Set the sample interval for the plot.
     */
    public void setSampleInterval(int minutes) {
        ElevationPlotUtil.setDefaultNumSteps((24 * 60) / minutes);
        _updateModel();
        _fireChangeEvent();
    }


    /** Return a title based on the site and the date */
    public String getTitle() {
        return _site.mountain + ": Night of "
                + TITLE_FORMAT.format(_sunRiseSet.nauticalTwilightStart)
                + " ⟶ "
                + TITLE_FORMAT.format(_sunRiseSet.nauticalTwilightEnd);
    }


    /** Return the threshold in degrees above the horizon where targets are considered observable */
    public static double getObsThreshold() {
        return _obsThreshold;
    }

    // Update the model data based on the current settings
    private void _updateModel() {
        _plotUtil = new ElevationPlotUtil(_startDate, _site, _targets);
        int numSteps = _plotUtil.getNumSteps();

        _maxElevation = new double[_targets.length];
        _maxElevationTime = new double[_targets.length];

        _tableModels = new ElevationPlotTableModel[_targets.length];
        for (int i = 0; i < _targets.length; i++)
            _tableModels[i] = new ElevationPlotTableModel(i);

        _sunRiseSet = new SunRiseSet(_startDate.getTime(), _site);

        _xDate = _plotUtil.getXData();
        _yData = _plotUtil.getYData();
        _yDataAirmass = _plotUtil.getYDataAirmass();
        _yDataPa = _plotUtil.getYDataPa();

        // Determine the min and max elevations for each target
        for (int i = 0; i < _targets.length; i++) {
            double[] el = _yData[i];          // elevation at each sample
            _maxElevation[i] = -99.0;
            _maxElevationTime[i] = 0.0;
            for (int j = 0; j < numSteps; j++) {
                if (el[j] > _maxElevation[i]) {
                    _maxElevation[i] = el[j];
                    _maxElevationTime[i] = _xDate[i][j].getTime();
                }
            }
        }
    }

    /** Return the name and location of the observatory site */
    public Site getSite() {
        return _site;
    }

    /** Set the observatory site description */
    public void setSite(Site site) {
        _setSite(site);
        if (_timeZoneId.equals(SITE_TIME)) {
            _setTimeZone(site.timezone().getDisplayName(), SITE_TIME);
        }
        _setDate(_startDate);
        _updateModel();
        _fireChangeEvent();
    }

    // Set the observatory site
    private void _setSite(Site site) {
        _site = site;
        Preferences.set(SITE_PREF_KEY, _site.name());
    }

    /** Return the time zone display name to use for the X values */
    public String getTimeZoneDisplayName() {
        return _timeZoneDisplayName;
    }

    /** Return the time zone id to use for the X values */
    public String getTimeZoneId() {
        return _timeZoneId;
    }

    /** Return the time zone to use for the X values */
    public TimeZone getTimeZone() {
        return _timeZone;
    }

    /**
     * Set the time zone to use to display the X values.
     * @param  timeZoneDisplayName name to display for the time zone
     * @param  timeZoneId the id of the time zone
     */
    public void setTimeZone(String timeZoneDisplayName, String timeZoneId) {
        _setTimeZone(timeZoneDisplayName, timeZoneId);
        _fireChangeEvent();
    }

    // Set the time zone to use to display the X values
    private void _setTimeZone(String timeZoneDisplayName, String timeZoneId) {
        _timeZoneDisplayName = timeZoneDisplayName;
        _timeZoneId = timeZoneId;
        Preferences.set(TIMEZONE_DISPLAY_NAME_PREF_KEY, _timeZoneDisplayName);
        Preferences.set(TIMEZONE_ID_PREF_KEY, _timeZoneId);

        _timeZone = ElevationPlotUtil.UT;
        if (! _timeZoneId.equals(LST) && ! _timeZoneId.equals(UT)) {
            _timeZone = _site.timezone();
        }
        DATE_FORMAT.setTimeZone(_timeZone);
    }


    /** Return the date for the plot */
    public Date getDate() {
        return _startDate;
    }

    /** Set the date of the plot */
    public void setDate(Date date) {
        _setDate(date);
        _updateModel();
        _fireChangeEvent();
    }

    /**
     * Set the starting date of the plot and enforce a base starting time in the Date
     * object of 12:00 noon at the telescope site
     */
    private void _setDate(Date date) {
        Calendar cal = Calendar.getInstance(_site.timezone());
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 12); // noon
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        _startDate = cal.getTime();

        cal.add(Calendar.HOUR_OF_DAY, 24); // noon next day
        _endDate = cal.getTime();
    }


    // Notify all DatasetChangeListeners that the model changed
    private void _fireChangeEvent() {
        // Notify the model's change listeners
        ChangeEvent changeEvent = new ChangeEvent(this);
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
        // Fire change events for the tables
        for (ElevationPlotTableModel _tableModel : _tableModels)
            _tableModel.fireTableModelEvent();
    }

    /** Return the number of target objects returned by {@link #getTargets}. */
    public int getNumTargets() {
        if (_targets == null)
            return 0;
        return _targets.length;
    }

    /** Return an array of target objects, whose elevations are being plotted */
    public TargetDesc[] getTargets() {
        return _targets;
    }


    /** Set the array of target objects, whose elevations are being plotted */
    public void setTargets(TargetDesc[] targets) {
        _setTargets(targets);
        _updateModel();
        _fireChangeEvent();
    }

    // Set the array of target objects, whose elevations are being plotted
    private void _setTargets(TargetDesc[] targets) {
        _targets = targets;
    }


    /** Return the TableModel corresponding to the given index in the array returned from {@link #getTargets}. */
    public TableModel getTableModel(int targetIndex) {
        return _tableModels[targetIndex];
    }

    /** Return an XYDataset for this model that shows altitude against time */
    public XYDataset getXYDataset() {
        TimeSeriesCollection tsc = new TimeSeriesCollection(_timeZone);
        int numSteps = _plotUtil.getNumSteps();
        for (int i = 0; i < _targets.length; i++) {
            TimeSeries ts = new TimeSeries(_targets[i].getName(), FixedMillisecond.class);
            Date[] times = _xDate[i];
            double[] elevations = _yData[i];
            for (int j = 0; j < numSteps; j++) {
                ts.add(new FixedMillisecond(times[j]), elevations[j]);
            }
            tsc.addSeries(ts);
        }
        return tsc;
    }

    /** Return an XYDataset for this model that shows parallactic angle against time */
    public XYDataset getSecondaryDataset() {
        TimeSeriesCollection tsc = new TimeSeriesCollection(_timeZone);
        int numSteps = _plotUtil.getNumSteps();
        for (int i = 0; i < _targets.length; i++) {
            TimeSeries ts = new TimeSeries(_targets[i].getName(), FixedMillisecond.class);
            Date[] times = _xDate[i];
            double[] pa = _yDataPa[i];
            for (int j = 0; j < numSteps; j++) {
                ts.add(new FixedMillisecond(times[j]), pa[j]);
            }
            tsc.addSeries(ts);
        }
        return tsc;
    }

    /**
     * Return an XYDataset that covers the time outside of the timing windows.
     * In other words, all time for the day except those periods where it is ok
     * to observe according to the timing windows.
     */
    public XYDataset getTimingWindowsDataset() {
        final TimeSeriesCollection tsc = new TimeSeriesCollection(_timeZone);
        final int steps                = _plotUtil.getNumSteps();

        final long start = _startDate.getTime();
        final long end   = _endDate.getTime();

        for (int i=0; i<_targets.length; ++i) {
            final TargetDesc        t = _targets[i];

            // Get intervals describing time outside of the timing windows.
            final Union<Interval>  ws = new Union<>(new Interval(start, end));
            ws.remove(t.getTimingWindows(start, end));

            final TimeSeries       ts = new TimeSeries(t.getName(), FixedMillisecond.class);
            final Date[]        times = _xDate[i];
            final double[] elevations = _yData[i];

            for (int j=0; j<steps; ++j) {
                final long time = times[j].getTime();
                ts.add(new FixedMillisecond(time), ws.contains(time) ? elevations[j] : 0.0);
            }
            tsc.addSeries(ts);
        }

        return tsc;
    }

    /** Returns an XYDataset that displays the area that meets the given elevation constraints */
    public XYDataset getConstraintsDataset() {
        TimeSeriesCollection tsc = new TimeSeriesCollection(_timeZone);
        int numSteps = _plotUtil.getNumSteps();
        for (int i = 0; i < _targets.length; i++) {
            if (_targets[i].getElType() == null) continue;
            double[] elRange = getElevationRange(_targets[i]);
            TimeSeries ts = new TimeSeries(_targets[i].getName(), FixedMillisecond.class);
            Date[] times = _xDate[i];
            double[] elevations = _yData[i];
            boolean rising = false;
            for (int j = 0; j < numSteps; j++) {
                if (j != numSteps - 1 && elevations[j] != elevations[j+1]) rising = elevations[j] < elevations[j+1];
                boolean matches = checkElevationConstraints(_targets[i], elRange[0], elRange[1], elevations[j], rising,
                        _maxElevation[i]);
                ts.add(new FixedMillisecond(times[j]), matches ? elevations[j] : 0.0);
            }
            tsc.addSeries(ts);
        }
        return tsc;
    }

    // Returns an array containing the start and end elevation constraints, given the target.
    // The return values are converted from either airmass or hour angle to elevation.
    // Due to the conversion, the first value may be greater than the second, since when the
    // values are specified as hour angles, a negative value means "on the rising arc".
    private double[] getElevationRange(TargetDesc target) {
        double elMin = target.getElMin();
        double elMax = target.getElMax();
        Double startAlt = null;
        Double endAlt = null;
        switch(target.getElType()) {
            case AIRMASS:
                startAlt = ImprovedSkyCalcMethods.getAltitude(elMax);
                endAlt   = ImprovedSkyCalcMethods.getAltitude(elMin);
                break;
            case HOUR_ANGLE:
                startAlt = altit(target, _startDate, elMin, _site.latitude).getOrElse(0.0);
                endAlt   = altit(target, _endDate,   elMax, _site.latitude).getOrElse(0.0);
                break;
            case NONE:
                startAlt = 0.0;
                endAlt = 0.0;
        }
        return new double[]{startAlt, endAlt};
    }

    // Altitude in degrees at the target's declination at the given time, the given hour angle, and
    // the observer's latitude, or None if the target's coordinates are unknown.
    private static Option<Double> altit(TargetDesc target, Date when, double ha, double lat) {
        return target.getCoordinates(new Some<>(when.getTime())).map(coords ->
            ImprovedSkyCalcMethods.altit(coords.getDecDeg(), ha, lat)
        );
    }

    // Returns true if the given elevation value meets the constraints, otherwise false.
    // If rising is true, the target elevation is increasing (needed for negative hour angles).
    // highestElevation is the peak elevation for the target.
    private boolean checkElevationConstraints(TargetDesc target, double elStart, double elEnd,
                                             double elevation, boolean rising, double highestElevation) {
        double minEl = Math.min(elStart, elEnd);
        double maxEl = Math.max(elStart, elEnd);
        switch (target.getElType()) {
            case AIRMASS:
                return elevation >= minEl && elevation <= maxEl;

            case NONE:
                return false;

            case HOUR_ANGLE:
                double minHA = target.getElMin();
                double maxHA = target.getElMax();
                if (minHA < 0 && maxHA <= 0) {
                    return rising && elevation >= minEl && elevation <= maxEl;
                } else if (minHA >= 0 && maxHA > 0) {
                    return !rising && elevation >= minEl && elevation <= maxEl;
                } else {
                    return (rising && elevation >= elStart && elevation <= highestElevation)
                            || (!rising && elevation >= elEnd && elevation <= highestElevation);
                }
        }
        return false;
    }


    /** Return the approximate maximum elevation for the given target index */
    public double getMaxElevation(int targetIndex) {
        return _maxElevation[targetIndex];
    }

    /** Return the approximate time in ms of the maximum elevation for the given target index */
    public double getMaxElevationTime(int targetIndex) {
        return _maxElevationTime[targetIndex];
    }

    /** Return an array of the available categories */
    public String[] getCategories() {
        TreeSet<String> set = new TreeSet<>();
        for (TargetDesc _target : _targets) {
            set.add(_target.getCategory());
        }
        String[] result = new String[set.size()];
        set.toArray(result);
        return result;
    }


    /** Return an IntervalCategoryDataset for this model for the given category */
    public IntervalCategoryDataset getIntervalCategoryDataset(String category) {
        TaskSeriesCollection collection = new TaskSeriesCollection();
        TaskSeries taskSeries = new TaskSeries("Targets");

        // calculate the target description field widths, so they can be lined up in columns
        int[] maxWidths = _calculateTargetDescriptionColumnWidths(_targets);
        int totalWidth = 0;
        for (int maxWidth : maxWidths) {
            totalWidth += maxWidth;
        }

        // make the dataset
        for (int i = 0; i < _targets.length; i++) {
            if (_targets[i].getCategory().equals(category)) {
                String name = _getTargetDescription(_targets[i], maxWidths, totalWidth);
                Date[] times = _xDate[i];
                double[] elevations = _yData[i];
                Date[] xTimes = _findCrossingPoints(times, elevations);
                if (xTimes.length == 2) {
                    // simple range
                    Task task = new Task(name, new SimpleTimePeriod(xTimes[0], xTimes[1]));
                    taskSeries.add(task);
                } else {
                    // range is split and wraps around graph
                    Task task = new Task(name, new SimpleTimePeriod(xTimes[0], xTimes[xTimes.length - 1]));
                    int n = xTimes.length / 2;
                    for (int j = 0; j < n; j++) {
                        Task subtask = new Task(name, new SimpleTimePeriod(xTimes[j * 2], xTimes[j * 2 + 1]));
                        task.addSubtask(subtask);
                    }
                    taskSeries.add(task);
                }
            }
        }

        collection.add(taskSeries);
        return collection;
    }


    // calculate the target description field widths, so they can be lined up in columns
    private int[] _calculateTargetDescriptionColumnWidths(TargetDesc[] targets) {
        String[] ar = targets[0].getDescriptionFields();
        int numDesc = ar.length;
        int[] maxWidths = new int[numDesc];

        for (int j = 0; j < numDesc; j++)
            maxWidths[j] = ar[j].length();

        for (int i = 1; i < targets.length; i++) {
            ar = targets[i].getDescriptionFields();
            for (int j = 0; j < numDesc; j++)
                if (ar[j].length() > maxWidths[j])
                    maxWidths[j] = ar[j].length();
        }

        return maxWidths;
    }

    // Return a formatted string describing the given target, using the given field widths
    private String _getTargetDescription(TargetDesc target, int[] maxWidths, int totalWidth) {
        String[] ar = target.getDescriptionFields();
        StringBuilder sb = new StringBuilder(totalWidth);
        for (int i = 0; i < maxWidths.length; i++) {
            sb.append(StringUtil.pad(ar[i], maxWidths[i], true));
            if (i != maxWidths.length - 1)
                sb.append(' ');
        }
        return sb.toString();
    }


    /** Return the LST date for the given UT date */
    public Date getLst(Date date) {
         return _plotUtil.getLst(date);
    }

    /** Return the minimum date value */
    public Date getStartDate() {
        return _startDate;
    }

    /** Return the maximum date value */
    public Date getEndDate() {
        return _endDate;
    }

    /** Return the date/time given the UT hour between 0 and 24 */
    public Date getDateForHour(double hour) {
        Calendar cal = Calendar.getInstance(ElevationPlotUtil.UT);
        cal.setTime(_startDate);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        boolean nextDay = (hour < h);
        CalendarUtil.setHours(cal, hour, nextDay);
        return cal.getTime();
    }

    /** Return sunrise time in the selected time zone */
    public Long getSunRise() {
        return _sunRiseSet.sunrise;
    }

    /** Return sunset in the selected time zone */
    public Long getSunSet() {
        return _sunRiseSet.sunset;
    }
    /** Return the start time of nautical twilight in the selected time zone */
    public Long getNauticalTwilightStart() {
        return _sunRiseSet.nauticalTwilightStart;
    }

    /** Return the end time of nautical twilight in the selected time zone */
    public Long getNauticalTwilightEnd() {
        return _sunRiseSet.nauticalTwilightEnd;
    }

    /** Return the start time of astronomical twilight in the selected time zone */
    public Long getAstronomicalTwilightStart() {
        return _sunRiseSet.astronomicalTwilightStart;
    }

    /** Return the end time of astronomical twilight in the selected time zone */
    public Long getAstronomicalTwilightEnd() {
        return _sunRiseSet.astronomicalTwilightEnd;
    }

    // Return the index of the lowest value in the given array, assuming that the array
    // contains values in increasing order, but may wrap around
    private int _getOffset(Date[] ar) {
        Date val = ar[0];
        for (int i=0; i<ar.length; i++) {
            if (ar[i].compareTo(val) < 0) return i;
            val = ar[i];
        }
        return 0;
    }


    // Return an array of date/time objects indicating the points in the given arrays
    // where the elevation crosses _obsThreshold. The return array will contain an even
    // number of dates: where the first date of each pair is the time the star goes above
    // the threshold, and the second is when it goes below. Since we are only looking
    // at one day, the beginning and end of the day are treated specially, if
    // the elevation is above the threshold there.
    // The return array is also sorted in increasing order of time.
    private Date[] _findCrossingPoints(Date[] times, double[] elevations) {
        // used to get times in increasing order
        int offset = _getOffset(times);

        List<Date> l = new ArrayList<>();
        boolean started = false;

        // start interval
        double val = elevations[offset];
        if (val > _obsThreshold) {
            l.add(times[offset]);
            started = true;
        }

        int n = elevations.length - 1;
        for (int i = 1; i < n; i++) {
            int index = (offset + i) % elevations.length;
            val = elevations[index];
            if (!started && val > _obsThreshold) {
                l.add(times[index]);
                started = true;
            } else if (started && val <= _obsThreshold) {
                l.add(times[index]);
                started = false;
            }
        }

        // close out any interval
        if (started)
            l.add(times[(offset + n) % elevations.length]);

        // make return array from list
        Date[] ar;
        if (l.size() != 0) {
            ar = new Date[l.size()];
            l.toArray(ar);
        } else {
            // no crossing points: add dummy start and stop point at 0
            ar = new Date[2];
            ar[0] = ar[1] = times[0];
        }

        return ar;
    }


    /**
     * register to receive change events from this object whenever the
     * model changes.
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }

    // Local table model class, used to view the graph data for a single target as a table
    private class ElevationPlotTableModel implements TableModel {
        private int _targetIndex;
        private EventListenerList _tableListenerList = new EventListenerList();

        public ElevationPlotTableModel(int targetIndex) {
            _targetIndex = targetIndex;
        }

        public int getRowCount() {
            if (_xDate == null)
                return 0;
            return _xDate[_targetIndex].length;
        }

        public int getColumnCount() {
            return 4;
        }

        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Time (hours)";
                case 1:
                    return "Elevation (degrees)";
                case 2:
                    return "Airmass";
                case 3:
                    return "Parallactic Angle";
            }
            throw new IndexOutOfBoundsException("columnIndex");
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
            }
            return Double.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    Date date = _xDate[_targetIndex][rowIndex];
                    if (_timeZoneId.equals(LST)) {
                         date = _plotUtil.getLst(date);
                    }
                    return DATE_FORMAT.format(date);
                case 1:
                    return _yData[_targetIndex][rowIndex];
                case 2:
                    return _yDataAirmass[_targetIndex][rowIndex];
                case 3:
                    return _yDataPa[_targetIndex][rowIndex];
            }
            throw new IndexOutOfBoundsException("columnIndex");
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        public void fireTableModelEvent() {
            TableModelEvent changeEvent = new TableModelEvent(this);
            Object[] listeners = _tableListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == TableModelListener.class) {
                    ((TableModelListener) listeners[i + 1]).tableChanged(changeEvent);
                }
            }
        }

        public void addTableModelListener(TableModelListener l) {
            _tableListenerList.add(TableModelListener.class, l);
        }

        public void removeTableModelListener(TableModelListener l) {
            _tableListenerList.remove(TableModelListener.class, l);
        }
    }
}
