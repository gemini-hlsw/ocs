//
// $Id: ProgramState.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * The program status report is simply a map of observation id to
 * matching observation status for a particular program.
 */
public final class ProgramState implements Serializable {
    static final long serialVersionUID = 1;

    private static final String XML_PROGRAM_ELEMENT = "prog";
    private static final String XML_PROGRAM_ID_ATTR = "id";
    private static final String XML_PROGRAM_AFF_ATTR = "aff";

    public static final ProgramState[] EMPTY_STATE_ARRAY =
            new ProgramState[0];

    private SPProgramID _progId;
    private Affiliate _affiliate;
    private final ProgramContact _contact;
    private final ProgramHistoryItem[] _history;
    private final TreeMap<SPObservationID, ObservationState> _obsMap = new TreeMap<SPObservationID, ObservationState>();

    public ProgramState(final ISPProgram prog)  {
        if (prog == null) throw new NullPointerException();
        _progId = prog.getProgramID();
        final SPProgram progDataObj = (SPProgram) prog.getDataObject();
        _affiliate = progDataObj.getPIAffiliate();
        _contact = new ProgramContact(prog);
        _history = ProgramHistoryItem.createList();

        final List<ISPObservation> obsList = prog.getAllObservations();
        if ((obsList == null) || (obsList.size() == 0)) return;

        for (final ISPObservation obs : obsList) {
            final ObservationState obsState = new ObservationState(obs);
            _obsMap.put(obsState.getObservationId(), obsState);
        }
    }

    public ProgramState(final Element prog) throws XmlException {
        if (prog == null) throw new NullPointerException();

        // Set the program id.
        final String idStr = prog.attributeValue(XML_PROGRAM_ID_ATTR);
        if (idStr == null) {
            throw new XmlException("Missing program id: " + XML_PROGRAM_ID_ATTR);
        }
        try {
            _progId = SPProgramID.toProgramID(idStr);
        } catch (SPBadIDException e) {
            throw new XmlException("Illegal prog id: " + idStr);
        }

        // Set the affiliate.
        final Element aff = prog.element(XML_PROGRAM_AFF_ATTR);
        if (aff != null) {
            _affiliate = Affiliate.fromString(aff.getTextTrim());
        }

        // Set the contact information.
        final Element contact = prog.element(ProgramContact.XML_CONTACT_ELEMENT);
        _contact = new ProgramContact(contact);

        // Set the history list.
        final Element eventList = prog.element(ProgramHistoryItem.XML_EVENT_LIST_ELEMENT);
        _history = ProgramHistoryItem.createList(eventList);

        // Set each observation.
        final List<Element> obsList = prog.elements(ObservationState.XML_OBS_ELEMENT);
        if ((obsList == null) || (obsList.size() == 0)) return;

        for (final Element obsElement : obsList) {
            final ObservationState obsState = new ObservationState(obsElement);
            _obsMap.put(obsState.getObservationId(), obsState);
        }
    }

    public Element toElement(final DocumentFactory fact) {
        final Element prog = fact.createElement(XML_PROGRAM_ELEMENT);
        prog.addAttribute(XML_PROGRAM_ID_ATTR, _progId.toString());
        if (_affiliate != null) {
            prog.addAttribute(XML_PROGRAM_AFF_ATTR, _affiliate.displayValue);
        }

        final Element contactElement = _contact.toElement(fact);
        prog.add(contactElement);

        final Element historyElement = ProgramHistoryItem.toElement(fact, _history);
        prog.add(historyElement);

        for (final Object o : _obsMap.values()) {
            final ObservationState os = (ObservationState) o;
            final Element obsElement = os.toElement(fact);
            prog.add(obsElement);
        }
        return prog;
    }

    public SPProgramID getProgramId() {
        return _progId;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public Affiliate getAffiliate() {
//        return _affiliate;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    public ProgramContact getProgramContact() {
        return _contact;
    }

    /**
     * Gets a SortedMap, ordered by observation id, of {@link ObservationState}
     * objects for each observation in the program.
     *
     * @return SortedMap of {@link ObservationState} objects
     */
    public SortedMap<SPObservationID, ObservationState> getObservations() {
        return Collections.unmodifiableSortedMap(_obsMap);
    }

    /*
    public int compareTo(Object other) {
        ProgramState that = (ProgramState) other;

        int res;
        res = _progId.compareTo(that._progId);
        if (res != 0) return res;

        if (_affiliate == null) {
            if (that._affiliate != null) return -1;
        } else {
            if (that._affiliate == null) return 1;
            res = _affiliate.compareTo(that._affiliate);
        }
        if (res != 0) return res;

        res = _contact.compareTo(that._contact);
        if (res != 0) return res;

        res = _obsMap.size() - that._obsMap.size();
        if (res != 0) return res;

        Iterator it1 = _obsMap.values().iterator();
        Iterator it2 = that._obsMap.values().iterator();


        return 0;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;

        ProgramState that = (ProgramState) other;

        if (!_progId.equals(that._progId)) return false;

        if (_affiliate == null) {
            if (that._affiliate != null) return false;
        } else {
            if (!_affiliate.equals(that._affiliate)) return false;
        }

        if (!_contact.equals(that._contact)) return false;
        if (!_obsMap.equals(that._obsMap)) return false;

        return true;
    }

    public int hashCode() {
        int res = _progId.hashCode();
        if (_affiliate != null) res = 37*res + _affiliate.hashCode();
        res = 37*res + _contact.hashCode();
        res = 37*res + _obsMap.hashCode();
        return res;
    }
    */
}
