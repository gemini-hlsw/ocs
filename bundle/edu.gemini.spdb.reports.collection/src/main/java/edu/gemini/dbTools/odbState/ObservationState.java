//
// $Id: ObservationState.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.util.SPTreeUtil;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public final class ObservationState implements Serializable {
    private static final long serialVersionUID = 2;

    public static final String XML_OBS_ELEMENT = "obs";

    private static final String XML_OBS_ID_ATTR            = "id";
    private static final String XML_OBS_STATUS_ATTR        = "status";
    private static final String XML_TOO_TYPE_ATTR          = "too";

    private static final String XML_OBS_END_ELEMENT        = "obsEnd";
    private static final String XML_OBS_UTC_ELEMENT        = "utc";
    private static final String XML_OBS_NIGHT_ELEMENT      = "night";
    private static final String XML_OBS_TOTAL_TIME_ELEMENT = "totalTime";

    private final SPObservationID   _obsId;
    private final ObservationStatus _status;
    private final TooType           _too;

    private long   _obsEnd;
    private long   _totalTime;
    private String _night;

    public ObservationState(final ISPObservation obs)  {
        _obsId  = obs.getObservationID();
        _status = ObservationStatus.computeFor(obs);
        _too    = Too.get(obs);

        final ObsExecRecord obsRec = SPTreeUtil.getObsRecord(obs);
        if (obsRec != null) {
            _totalTime = obsRec.getTotalTime();
            _obsEnd    = obsRec.getLastEventTime();
            _night     = _getNight(_obsEnd);
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

        // Get the TooType, defaulting to none.
        final String s = obs.attributeValue(XML_TOO_TYPE_ATTR);
        _too = (s == null) ? TooType.none : TooType.getTooType(s);

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
        obs.addAttribute(XML_TOO_TYPE_ATTR, _too.name());

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

    public TooType getTooType() {
        return _too;
    }

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

        final DateFormat f = new SimpleDateFormat("MMM yyyy");
        buf.append(f.format(endCal.getTime()));

        return buf.toString();
    }
}
