package edu.gemini.obslog.obslog;

import edu.gemini.obslog.instruments.WeatherSegment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class OlDefaultObservingLog implements IObservingLog, Serializable {

    private List<IObservingLogSegment> _logSegments;
    private OlLogInformation _logInformation;
    private IObservingLogSegment _weatherInfo;

    private synchronized List<IObservingLogSegment> _getLogSegments() {
        if (_logSegments == null) {
            _logSegments = new ArrayList<>();
        }
        return _logSegments;
    }


    public int getLogSegmentCount() {
        return _getLogSegments().size();
    }

    public List<IObservingLogSegment> getLogSegments() {
        return Collections.unmodifiableList(new ArrayList<>(_getLogSegments()));
    }

    public void setLogSegments(List<IObservingLogSegment> segments) {
        if (segments == null) segments = new ArrayList<>();
        _logSegments = segments;
    }

    public void addLogSegment(IObservingLogSegment segment) {
        List<IObservingLogSegment> logSegments = _getLogSegments();
        logSegments.add(segment);
    }

    public IObservingLogSegment getLogSegment(int segmentIndex) {
        if (segmentIndex < getLogSegmentCount()) {
            return getLogSegments().get(segmentIndex);
        }
        return null;
    }

    /**
     * The parameters that define the users during the night.
     *
     * @return log information
     */
    public OlLogInformation getLogInformation() {
        if (_logInformation == null) {
            _logInformation = new OlLogInformation();
        }
        return _logInformation;
    }

    public void setLogInformation(OlLogInformation logInformation) {
        _logInformation = logInformation;
    }

    public void addWeatherSegment(IObservingLogSegment segment) {
        if (segment == null) return;
        _weatherInfo = segment;
    }

    public IObservingLogSegment getWeatherSegment() {
        if (_weatherInfo == null) return WeatherSegment.EMPTY_WEATHER_SEGMENT;
        return _weatherInfo;
    }

    public void dump() {
        List segs = getLogSegments();
        for (int i = 0; i < getLogSegmentCount(); i++) {
            ((IObservingLogSegment) segs.get(i)).dump();
        }
    }
}
