//
// $Id: ProgramHistoryItem.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPProgram;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class ProgramHistoryItem implements Comparable<ProgramHistoryItem>, Serializable {
    static final long serialVersionUID = 1;

    private static final ProgramHistoryItem[] EMPTY_ARRAY = {};

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss a");

    static {
        DATE_FORMAT.setTimeZone(UTC);
        TIME_FORMAT.setTimeZone(UTC);
    }

    private static final String XML_EVENT_ELEMENT = "event";
    public static final String XML_EVENT_LIST_ELEMENT = "eventList";
    private static final String XML_EVENT_TYPE_ATTR = "type";
    private static final String XML_EVENT_WHO_ATTR = "who";

    private static final String XML_EVENT_UTC_ELEMENT = "utc";

    private static final String XML_EVENT_DATE_ELEMENT = "date";
    private static final String XML_EVENT_TIME_ELEMENT = "time";

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    /**
//     * Creates the ProgramHistoryItem from the given SPObservation.HistoryListItem,
//     * provided the HistoryListItem is a valid fetch/store event.  Otherwise,
//     * <code>null</code> is returned.
//     */
//    public static ProgramHistoryItem create(final HistoryList.HistoryListItem hli) {
//        final long time = hli.getTime();
//        final ObsEventMsg obsEvent = hli.getEvent();
//        if (obsEvent == null) return null;
//
//        final String eventStr;
//        if (obsEvent.equals(ObsEventMsg.PROGRAM_FETCH)) {
//            eventStr = "fetch";
//        } else if (obsEvent.equals(ObsEventMsg.PROGRAM_STORE)) {
//            eventStr = "store";
//        } else {
//            return null;
//        }
//
//        // This sucks, but this is the only way to figure out "who" fetched
//        // or stored.  We need to fix this.
//        String who = hli.getMessage();
//        if (who == null) return null;
//        final int junkPos = who.indexOf(": ");
//        who = who.substring(junkPos + 2);
//
//        return new ProgramHistoryItem(time, eventStr, who);
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    public static ProgramHistoryItem[] createList()
             {

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final List<ProgramHistoryItem> res = new ArrayList();

               // RCN: TODO: re-enable
//        SPProgram progData = (SPProgram) prog.getDataObject();
//        HistoryList historyList = progData.getHistoryList();
//        for (Iterator it = historyList.iterator(); it.hasNext();) {
//            HistoryList.HistoryListItem hli = (HistoryList.HistoryListItem) it.next();
//            ProgramHistoryItem phi = create(hli);
//            if (phi == null) continue;
//
//            res.add(phi);
//        }

        return res.toArray(EMPTY_ARRAY);
    }

    public static ProgramHistoryItem[] createList(final Element eventList)
            throws XmlException {
        if (eventList == null) return EMPTY_ARRAY;

        final List<Element> phiList = eventList.elements(XML_EVENT_ELEMENT);
        if (phiList == null) return EMPTY_ARRAY;

        final List<ProgramHistoryItem> res = new ArrayList<ProgramHistoryItem>();
        for (final Element phiElement : phiList) {
            res.add(new ProgramHistoryItem(phiElement));
        }
        return res.toArray(EMPTY_ARRAY);
    }

    public static Element toElement(final DocumentFactory fact, final ProgramHistoryItem[] history) {
        final Element res = fact.createElement(XML_EVENT_LIST_ELEMENT);
        if (history == null) return res;

        for (final ProgramHistoryItem aHistory : history) {
            res.add(aHistory.toElement(fact));
        }

        return res;
    }

    private final long _time;
    private final String _event;
    private final String _who;

// --Commented out by Inspection START (8/12/13 3:38 PM):
//    private ProgramHistoryItem(final long time, final String event, final String who) {
//        if (event == null) throw new NullPointerException("event is null");
//        if (who == null) throw new NullPointerException("who is null");
//        _time = time;
//        _event = event;
//        _who = who;
//    }
// --Commented out by Inspection STOP (8/12/13 3:38 PM)

    private ProgramHistoryItem(final Element phi) throws XmlException {
        final String type = phi.attributeValue(XML_EVENT_TYPE_ATTR);
        if (type == null) {
            throw new XmlException("Missing program history event type: " +
                                   XML_EVENT_TYPE_ATTR);
        }

        final String who = phi.attributeValue(XML_EVENT_WHO_ATTR);
        if (who == null) {
            throw new XmlException("Missing program history who: " +
                                   XML_EVENT_WHO_ATTR);
        }

        final Element utc = phi.element(XML_EVENT_UTC_ELEMENT);
        if (utc == null) {
            throw new XmlException("Missing program history utc element: " +
                                   XML_EVENT_UTC_ELEMENT);
        }

        final String timeStr = utc.getTextTrim();
        if (timeStr == null) {
            throw new XmlException("Missing program history utc time.");
        }

        final long time;
        try {
            time = Long.parseLong(timeStr);
        } catch (NumberFormatException ex) {
            throw new XmlException("Illegal program history utc time: " +
                                   timeStr);
        }

        _time = time;
        _event = type;
        _who = who;
    }

    Element toElement(final DocumentFactory fact) {
        final Element res = fact.createElement(XML_EVENT_ELEMENT);
        res.addAttribute(XML_EVENT_TYPE_ATTR, _event);
        res.addAttribute(XML_EVENT_WHO_ATTR, _who);

        final Element utc = res.addElement(XML_EVENT_UTC_ELEMENT);
        utc.setText(String.valueOf(_time));

        final Date d = new Date(_time);

        final Element date = res.addElement(XML_EVENT_DATE_ELEMENT);
        date.setText(DATE_FORMAT.format(d));

        final Element time = res.addElement(XML_EVENT_TIME_ELEMENT);
        time.setText(TIME_FORMAT.format(d));

        return res;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public long getTime() {
//        return _time;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String getEvent() {
//        return _event;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String getWho() {
//        return _who;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    public boolean equals(final Object other) {
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;

        final ProgramHistoryItem that = (ProgramHistoryItem) other;

        if (_time != that._time) return false;
        if (!_event.equals(that._event)) return false;
        return _who.equals(that._who);

    }

    public int hashCode() {
        int res = (int) (_time ^ (_time >>> 32));
        res = res * 37 + _event.hashCode();
        res = res * 37 + _who.hashCode();
        return res;
    }


    public int compareTo(final ProgramHistoryItem hi) {

        if (_time < hi._time) return -1;
        if (hi._time < _time) return 1;

        int res = _event.compareTo(hi._event);
        if (res != 0) return res;

        res = _who.compareTo(hi._who);
        return res;
    }
}
