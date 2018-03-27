package edu.gemini.spModel.gemini.plan;

import edu.gemini.pot.sp.ISPCloneable;
import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class NightlyRecord extends AbstractDataObject implements ISPDataObject, ISPCloneable, Serializable {

    /**
     * This component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.PLAN_BASICPLAN;

    public static final ISPNodeInitializer<ISPNightlyRecord, NightlyRecord> NI =
            new SimpleNodeInitializer<>(SP_TYPE, () -> new NightlyRecord());

    private static final Logger LOG = Logger.getLogger(NightlyRecord.class.getName());

    // Attributes used for for paramset and change events
    private static final String OBSERVATION_LIST = "observationList";
    private static final String WEATHER_LOG = "weatherLog";
    private static final String DAY_OBSERVERS = "dayObservers";
    private static final String NIGHT_OBSERVERS = "nightObservers";
    private static final String SSA = "ssa";
    private static final String DATA_PROC = "dataProc";
    private static final String FILE_PREFIX = "filePrefix";
    private static final String CC_SOFTWARE_VERSION = "ccSoftwareVersion";
    private static final String DC_SOFTWARE_VERSION = "dcSoftwareVersion";
    private static final String SOFTWARE_VERSION_NOTE = "softwareVersionNote";
    private static final String NIGHT_COMMENT = "nightComment";


    // The list of observations observed during the night
    private List<SPObservationID> _observationsList;

    // List of WeatherInfo at different times during the night
    private List<WeatherInfo> _weatherLog;

    private final String DEFAULT_VALUE = "";

    // Besides weather the boilerplate of the observing log contains some information
    // about who is observing and system configuration.
    private String _dayObservers = DEFAULT_VALUE;
    private String _nightObservers = DEFAULT_VALUE;
    private String _ssa = DEFAULT_VALUE;
    private String _dataProc = DEFAULT_VALUE;
    private String _filePrefix = DEFAULT_VALUE;
    private String _ccSoftwareVersion = DEFAULT_VALUE;
    private String _dcSoftwareVersion = DEFAULT_VALUE;
    private String _softwareVersionNote = DEFAULT_VALUE;
    private String _nightComment = DEFAULT_VALUE;   // also called notes in the examples


    /**
     * Default constructor
     */
    public NightlyRecord() {
        super(SP_TYPE);
    }

    /**
     * Override clone to make sure the observation list is correctly
     * initialized.
     */
    public Object clone() {
        NightlyRecord obsLog = (NightlyRecord) super.clone();
        if (_observationsList != null) {
            obsLog._observationsList = new ArrayList<>(_observationsList);
        }
        if (_weatherLog != null) {
            obsLog._weatherLog = new ArrayList<>(_weatherLog);
        }
        return obsLog;
    }


    // -- observation list methods --

    // private routine to create this list lazily
    private synchronized List<SPObservationID> _getObservationsList() {
        if (_observationsList == null) {
            _observationsList = new ArrayList<>();
        }
        return _observationsList;
    }

    /**
     * Add an observation to the list of observations attempted.
     */
    public boolean addObservation(SPObservationID obsID) {
        List<SPObservationID> l = _getObservationsList();
        List<SPObservationID> oldList;
        List<SPObservationID> newList;
        synchronized (l) {
            oldList = Collections.unmodifiableList(new ArrayList<>(l));
            l.add(obsID);
            newList = Collections.unmodifiableList(new ArrayList<>(l));
        }
        firePropertyChange(OBSERVATION_LIST, oldList, newList);
        return true;
    }

    /**
     * Remove a specific observation id
     * @param obsID the <code>SPObservationID</code> of the observation that should be removed.
     * @return true or false telling whether it was successfull or not.
     */
    public boolean removeObservation(SPObservationID obsID) {
        List<SPObservationID> l = _getObservationsList();
        List<SPObservationID> oldList;
        List<SPObservationID> newList;
        boolean worked;
        synchronized (l) {
            oldList = Collections.unmodifiableList(new ArrayList<>(l));
            worked = l.remove(obsID);
            newList = Collections.unmodifiableList(new ArrayList<>(l));
        }
        firePropertyChange(OBSERVATION_LIST, oldList, newList);
        return worked;
    }

    // Initialize the list of observations
    private synchronized void _clearObservationList() {
        List<SPObservationID> l = _getObservationsList();
        l.clear();
    }

    /**
     * Empty the observation list
     */
    public void clearObservationList() {
        _clearObservationList();
    }
    /**
     * Get the current observations list.
     * @return a copy of the current observations list
     */
    public List<SPObservationID> getObservationList() {
        List<SPObservationID> l = _getObservationsList();
        return new ArrayList<>(l);
    }

    /**
     * A convenience method to return the number of observations in the observations list.
     * @return size of list
     */
    public int getObservationListSize() {
        return _getObservationsList().size();
    }

    // --- weather log methods --

    // private routine to create this list lazily
    private synchronized List<WeatherInfo> _getWeatherLog() {
        if (_weatherLog == null) {
            _weatherLog = new ArrayList<>();
        }
        return _weatherLog;
    }


    /**
     * Add an item item to the weather log
     */
    public boolean addWeatherInfo(WeatherInfo weatherInfo) {
        List<WeatherInfo> l = _getWeatherLog();
        List<WeatherInfo> oldList;
        List<WeatherInfo> newList;
        synchronized (l) {
            oldList = Collections.unmodifiableList(new ArrayList<>(l));
            l.add(weatherInfo);
            newList = Collections.unmodifiableList(new ArrayList<>(l));
        }
        firePropertyChange(WEATHER_LOG, oldList, newList);
        return true;
    }

    /**
     * Add a comment to an already existing weather log
     * @param entryID  the entry in the <tt>WeatherInfo</tt> list to update
     * @param comment the weather comment
     */
    public void setWeatherComment(int entryID, String comment) {
        List <WeatherInfo> l = _getWeatherLog();
        if (entryID >= l.size()) return;
        l.get(entryID).setComment(comment);
    }

    /**
     * Remove a specific weather log item.
     * @param weatherInfo the item that should be removed.
     * @return true or false telling whether it was successfull or not.
     */
    public boolean removeWeatherlog(WeatherInfo weatherInfo) {
        List<WeatherInfo> l = _getWeatherLog();
        List<WeatherInfo> oldList;
        List<WeatherInfo> newList;
        boolean worked;
        synchronized (l) {
            oldList = Collections.unmodifiableList(new ArrayList<>(l));
            worked = l.remove(weatherInfo);
            newList = Collections.unmodifiableList(new ArrayList<>(l));
        }
        firePropertyChange(WEATHER_LOG, oldList, newList);
        return worked;
    }

    /**
     * Clear out the weather log.
     */
    public void clearWeatherLog() {
        List<WeatherInfo> l = _getWeatherLog();
        l.clear();
    }

    /**
     * Get the current weather log.
     * @return a copy of the current weather log.
     */
    public List<WeatherInfo> getWeatherLog() {
        List<WeatherInfo> l = _getWeatherLog();
        return new ArrayList<>(l);
    }

    /**
     * A convenience method to return the number of items in the weather log.
     */
    public int getWeatherLogSize() {
        return _getWeatherLog().size();
    }


    // -- other access methods --


    public String getDayObservers() {
        return _dayObservers;
    }

    public void setDayObservers(String dayObservers) {
        String oldValue = _dayObservers;
        _dayObservers = dayObservers;
        firePropertyChange(DAY_OBSERVERS, oldValue, _dayObservers);
    }

    public String getNightObservers() {
        return _nightObservers;
    }

    public void setNightObservers(String nightObservers) {
        String oldValue = _nightObservers;
        _nightObservers = nightObservers;
        firePropertyChange(NIGHT_OBSERVERS, oldValue, _nightObservers);
    }

    public String getSSA() {
        return _ssa;
    }

    public void setSSA(String ssa) {
        String oldValue = _ssa;
        _ssa = ssa;
        firePropertyChange(SSA, oldValue, _ssa);
    }

    public String getDataProc() {
        return _dataProc;
    }

    public void setDataProc(String dataProc) {
        String oldValue = _dataProc;
        _dataProc = dataProc;
        firePropertyChange(DATA_PROC, oldValue, _dataProc);
    }

    public String getFilePrefix() {
        return _filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        String oldValue = _filePrefix;
        _filePrefix = filePrefix;
        firePropertyChange(FILE_PREFIX, oldValue, _filePrefix);
    }

    public String getCCSoftwareVersion() {
        return _ccSoftwareVersion;
    }

    public void setCCSoftwareVersion(String ccSoftwareVersion) {
        String oldValue = _ccSoftwareVersion;
        _ccSoftwareVersion = ccSoftwareVersion;
        firePropertyChange(CC_SOFTWARE_VERSION, oldValue, _ccSoftwareVersion);
    }

    public String getDCSoftwareVersion() {
        return _dcSoftwareVersion;
    }

    public void setDCSoftwareVersion(String dcSoftwareVersion) {
        String oldValue = _dcSoftwareVersion;
        _dcSoftwareVersion = dcSoftwareVersion;
        firePropertyChange(DC_SOFTWARE_VERSION, oldValue, _dcSoftwareVersion);
    }

    public String getSoftwareVersionNote() {
        return _softwareVersionNote;
    }

    public void setSoftwareVersionNote(String softwareVersionNote) {
        String oldValue = _softwareVersionNote;
        _softwareVersionNote = softwareVersionNote;
        firePropertyChange(SOFTWARE_VERSION_NOTE, oldValue, _softwareVersionNote);
    }

    public String getNightComment() {
        return _nightComment;
    }

    public void setNightComment(String nightComment) {
        String oldValue = _nightComment;
        _nightComment = nightComment;
        firePropertyChange(NIGHT_COMMENT, oldValue, _nightComment);
    }


    private ParamSet _getWeatherLogParamSet(PioFactory factory) {
        List<WeatherInfo> l = _getWeatherLog();
        int size = l.size();
        if (size == 0) {
            return null;
        }

        ParamSet paramSet = factory.createParamSet(WEATHER_LOG);
        for (int i = 0; i < size; i++) {
            WeatherInfo weatherInfo = l.get(i);
            ParamSet p = weatherInfo.getParamSet(factory, "weather");
            p.setSequence(i);
            paramSet.addParamSet(p);
        }

        return paramSet;
    }

    private void _setWeatherLogParamSet(ParamSet paramSet) {
        if (paramSet == null) {
            return;
        }
        clearWeatherLog();
        for (ParamSet p : paramSet.getParamSets()) {
            WeatherInfo weatherInfo = new WeatherInfo();
            weatherInfo.setParamSet(p);
            _weatherLog.add(weatherInfo);
        }
    }

    private Param _getObservationListParam(PioFactory factory) {
        List<SPObservationID> l = _getObservationsList();
        int size = l.size();
        if (size == 0) {
            return null;
        }

        Param p = factory.createParam(OBSERVATION_LIST);
        for (SPObservationID obsID : l) {
            p.addValue(obsID.toString());
        }

        return p;
    }

    private void _setObservationListParam(Param param) {
        if (param == null) {
            return;
        }
        clearObservationList();
        for (String value : param.getValues()) {
            try {
                _observationsList.add(new SPObservationID(value));
            } catch (SPBadIDException ex) {
                LOG.log(Level.WARNING, "Bad observation id: " + value + " while importing.");
            }
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);
        paramSet.addParam(_getObservationListParam(factory));
        paramSet.addParamSet(_getWeatherLogParamSet(factory));
        if (_dayObservers != null) {
            Pio.addParam(factory, paramSet, DAY_OBSERVERS, _dayObservers);
        }
        if (_nightObservers != null) {
            Pio.addParam(factory, paramSet, NIGHT_OBSERVERS, _nightObservers);
        }
        if (_ssa != null) {
            Pio.addParam(factory, paramSet, SSA, _ssa);
        }
        if (_dataProc != null) {
            Pio.addParam(factory, paramSet, DATA_PROC, _dataProc);
        }
        if (_filePrefix != null) {
            Pio.addParam(factory, paramSet, FILE_PREFIX, _filePrefix);
        }
        if (_ccSoftwareVersion != null) {
            Pio.addParam(factory, paramSet, CC_SOFTWARE_VERSION, _ccSoftwareVersion);
        }
        if (_dcSoftwareVersion != null) {
            Pio.addParam(factory, paramSet, DC_SOFTWARE_VERSION, _dcSoftwareVersion);
        }
        if (_softwareVersionNote != null) {
            Pio.addParam(factory, paramSet, SOFTWARE_VERSION_NOTE, _softwareVersionNote);
        }
        if (_nightComment != null) {
            Pio.addParam(factory, paramSet, NIGHT_COMMENT, _nightComment);
        }
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        _setObservationListParam(paramSet.getParam(OBSERVATION_LIST));
        _setWeatherLogParamSet(paramSet.getParamSet(WEATHER_LOG));

        _dayObservers = Pio.getValue(paramSet, DAY_OBSERVERS);
        _nightObservers = Pio.getValue(paramSet, NIGHT_OBSERVERS);
        _ssa = Pio.getValue(paramSet, SSA);
        _dataProc = Pio.getValue(paramSet, DATA_PROC);
        _filePrefix = Pio.getValue(paramSet, FILE_PREFIX);
        _ccSoftwareVersion = Pio.getValue(paramSet, CC_SOFTWARE_VERSION);
        _dcSoftwareVersion = Pio.getValue(paramSet, DC_SOFTWARE_VERSION);
        _softwareVersionNote = Pio.getValue(paramSet, SOFTWARE_VERSION_NOTE);
        _nightComment = Pio.getValue(paramSet, NIGHT_COMMENT);
    }
}
