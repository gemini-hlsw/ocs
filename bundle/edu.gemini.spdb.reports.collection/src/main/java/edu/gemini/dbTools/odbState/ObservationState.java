//
// $Id: ObservationState.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.util.SPTreeUtil;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ObservationState implements Serializable {
    static final long serialVersionUID = 1;

    public static final String XML_OBS_ELEMENT = "obs";
    private static final String XML_OBS_END_ELEMENT = "obsEnd";
    private static final String XML_OBS_UTC_ELEMENT = "utc";
    private static final String XML_OBS_NIGHT_ELEMENT = "night";
    private static final String XML_OBS_TOTAL_TIME_ELEMENT = "totalTime";

    private static final String XML_OBS_ID_ATTR = "id";
    private static final String XML_OBS_STATUS_ATTR = "status";

    private static final DateFormat FORMAT = new SimpleDateFormat("MMM yyyy");

    private SPObservationID _obsId;
    private final ObservationStatus _status;

    private long _obsEnd;
    private long _totalTime;
    private String _night;

    public ObservationState(final ISPObservation obs)  {
        _obsId = obs.getObservationID();

//        SPObservation dataObj = (SPObservation) obs.getDataObject();
        _status = ObservationStatus.computeFor(obs);

        final ObsExecRecord obsRec = SPTreeUtil.getObsRecord(obs);
        if (obsRec != null) {
            _totalTime = obsRec.getTotalTime();
            _obsEnd    = obsRec.getLastEventTime();
            _night = _getNight(_obsEnd);
        }
    }

    public ObservationState(final Element obs) throws XmlException {
        final String obsIdStr = obs.attributeValue(XML_OBS_ID_ATTR);
        if (obsIdStr == null) {
            throw new XmlException("Missing obs id: " + XML_OBS_ID_ATTR);
        }
        try {
            _obsId = new SPObservationID(obsIdStr);
        } catch (SPBadIDException e) {
            throw new XmlException("Illegal obsId: " + obsIdStr);
        }

        final String statusStr = obs.attributeValue(XML_OBS_STATUS_ATTR);
        if (statusStr == null) {
            throw new XmlException("Missing obs status: " + XML_OBS_STATUS_ATTR);
        }
        _status = ObservationStatus.getObservationStatus(statusStr, null);
        if (_status == null) {
            throw new XmlException("Unknown status id " + statusStr);
        }

        // Process the "obsEnd" element.
        final Element obsEnd = obs.element(XML_OBS_END_ELEMENT);
        if (obsEnd == null) return; // not done

        final Element utc = obsEnd.element(XML_OBS_UTC_ELEMENT);
        String timeStr = utc.getTextTrim();
        try {
            _obsEnd = Long.parseLong(timeStr);
        } catch (NumberFormatException ex) {
            throw new XmlException("Invalid timestamp: " + timeStr);
        }

        final Element night = obsEnd.element(XML_OBS_NIGHT_ELEMENT);
        if (night == null) {
            throw new XmlException("Missing element: " + XML_OBS_NIGHT_ELEMENT);
        }
        _night = night.getTextTrim();

        final Element totalTime = obsEnd.element(XML_OBS_TOTAL_TIME_ELEMENT);
        if (totalTime == null) {
            throw new XmlException("Missing element: " + XML_OBS_TOTAL_TIME_ELEMENT);
        }
        timeStr = totalTime.getTextTrim();
        try {
            _totalTime = Long.parseLong(timeStr);
        } catch (NumberFormatException ex) {
            throw new XmlException("Invalid timestamp: " + timeStr);
        }
    }

    public Element toElement(final DocumentFactory fact) {

        final Element obs = fact.createElement(XML_OBS_ELEMENT);
        obs.addAttribute(XML_OBS_ID_ATTR, _obsId.toString());
        obs.addAttribute(XML_OBS_STATUS_ATTR, _status.name());

        if (_obsEnd > 0) {
            final Element obsEnd = obs.addElement(XML_OBS_END_ELEMENT);
            final Element utc = obsEnd.addElement(XML_OBS_UTC_ELEMENT);
            utc.setText(String.valueOf(_obsEnd));

            final Element night = obsEnd.addElement(XML_OBS_NIGHT_ELEMENT);
            night.setText(_night);

            final Element totalTime = obsEnd.addElement(XML_OBS_TOTAL_TIME_ELEMENT);
            totalTime.setText(String.valueOf(_totalTime));
        }

        return obs;
    }

    public SPObservationID getObservationId() {
        return _obsId;
    }

    public ObservationStatus getStatus() {
        return _status;
    }

// --Commented out by Inspection START (9/3/13 10:55 AM):
//    public long getObsEnd() {
//        return _obsEnd;
//    }
// --Commented out by Inspection STOP (9/3/13 10:55 AM)

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public long getTotalTime() {
//        return _totalTime;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

// --Commented out by Inspection START (9/3/13 10:55 AM):
//    public String getNight() {
//        return _night;
//    }
// --Commented out by Inspection STOP (9/3/13 10:55 AM)


// --Commented out by Inspection START (9/3/13 10:55 AM):
//    public final boolean hasBeenObserved() {
//        if (ObservationStatus.OBSERVED == _status) return true;
//        return ObservationStatus.OBSERVED.isLessThan(_status);
//    }
// --Commented out by Inspection STOP (9/3/13 10:55 AM)

    /**
     * Gets a formated string that indicates the night during which the
     * observation took place.
     * @param time utc time of the last observation event message
     * @return formatted string indicating the night during which the
     * observation took place.
     */
    private static String _getNight(final long time) {
        final Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(time);

        // The calendar is in the local time zone.  If the time is before
        // noon, then the night started on the previous day.  If the time
        // is after noon, then the night ends on the next day.
        final int ampm = cal.get(Calendar.AM_PM);

        final Calendar endCal;

        final int startDay;
        final int endDay;
        if (ampm == Calendar.AM) {
            endCal = (Calendar) cal.clone();
            endDay = cal.get(Calendar.DAY_OF_MONTH);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            startDay = cal.get(Calendar.DAY_OF_MONTH);
        } else {
            startDay = cal.get(Calendar.DAY_OF_MONTH);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            endCal = cal;
            endDay = cal.get(Calendar.DAY_OF_MONTH);
        }

        final StringBuilder buf = new StringBuilder();
        buf.append(startDay).append("/").append(endDay).append(" ");
        buf.append(FORMAT.format(endCal.getTime()));

        return buf.toString();
    }

    /*
    public int compareTo(Object o) {
        ObservationState that = (ObservationState) o;

        int res = _obsId.compareTo(that._obsId);
        if (res != 0) return res;

        if (_status.isLessThan(that._status)) return -1;
        return (_status.isGreaterThan(that._status)) ? 1 : 0;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;

        ObservationState that = (ObservationState) other;
        if (!_obsId.equals(that._obsId)) return false;
        if (!_status.equals(that._status)) return false;
        return true;
    }

    public int hashCode() {
        int res = _obsId.hashCode();
        res = res*37 + _status.hashCode();
        return res;
    }
    */

}
