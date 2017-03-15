package edu.gemini.dbTools.tigratable;

import edu.gemini.pot.sp.*;
import edu.gemini.sp.vcs2.VcsService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObservationStatus;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This class contains the data required to write a single table row.
 */
public class TigraTableRow implements Serializable {

    private static final Integer ONE = 1;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        dateFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
    }

    private static synchronized String format(Date d) {
        return dateFormat.format(d);
    }

    // because, unfortunately, ObservationStatus does not implement
    // Comparable
    private static class ObservationStatusComparator implements Comparator<ObservationStatus>, Serializable {
        public int compare(final ObservationStatus os1, final ObservationStatus os2) {
            // not important to actually sort them -- just need this for the
            // TreeMap keyed by ObservationStatus
            return os1.ordinal() - os2.ordinal();
        }
    }

    /**
     * Creates a TigraTableRow using the information in the given program.
     *
     * @param prog the program whose data will be used to populate the
     * TigraTableRow
     *
     * @return newly created TigraTableRow
     */
    public static TigraTableRow create(final ISPProgram prog, final VcsService vcs)  {
        final TigraTableRow ttr = new TigraTableRow();
        ttr._setProgramId(prog.getProgramID());

        // Extract information from the Science Program data object.
        final SPProgram progDataObject = (SPProgram) prog.getDataObject();
        if (progDataObject != null) {
            // PI Name
            final SPProgram.PIInfo piInfo = progDataObject.getPIInfo();
            if (piInfo != null) {
                ttr._setPiName(piInfo.getLastName());
            }

            // NGO Contacts
            String[] emailAddrs = _getEmails(progDataObject.getPrimaryContactEmail());
            ttr._setNgoEmails(emailAddrs);
            ttr._setNgoContacts(_getContacts(emailAddrs));

            // Gemini Contact
            emailAddrs = _getEmails(progDataObject.getContactPerson());
            ttr._setGeminiContacts(_getContacts(emailAddrs));
        }

        ttr._lastSync = SyncTimestamp.lookupDateOrNull(prog, vcs);

        // Count up the observations and add their instrument
        final List<ISPObservation> obsList = prog.getAllObservations();
        if (obsList == null) return ttr;
        for (final ISPObservation anObsList : obsList) {
            // Increment the count of observations according to status.
            final ObservationStatus obsStatus;
            obsStatus = _getObsStatus(anObsList);
            if (obsStatus != null) ttr._incrementObsCount(obsStatus);

            // Add this observation's instrument(s).
            _addInstruments(ttr, anObsList);
        }

        return ttr;
    }

    // Replaces any newlines with spaces and trims the string.
    private static String strip(final String s) {
        return (s == null) ? null : s.replace('\n', ' ').trim();
    }

    /**
     * Splits the given String containing comma, semicolon, or space separated
     * email addresses into an array of email addresses.
     */
    private static String[] _getEmails(final String emailAddrsStr) {
        if (emailAddrsStr == null) return EMPTY_STRING_ARRAY;

        final String[] splitEmailAddrs = emailAddrsStr.split("[,; ]");
        final List<String> emailList = new ArrayList<>();
        for (final String emailAddr : splitEmailAddrs) {
            final int at = emailAddr.indexOf('@');
            if (at == -1) continue; // some entries will be empty, some are just names

            // Some email addresses will look like: <name@somewhere.edu>
            // Strip off the leading "<" in that case.
            int start = 0;
            if (emailAddr.startsWith("<")) start = 1;
            int end = emailAddr.length();
            if (emailAddr.endsWith(">")) end -= 1;
            emailList.add(strip(emailAddr.substring(start, end)));
        }
        return emailList.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Takes the given array of email address strings and retuns just the
     * username id portion (striping off the @domain part of the address).
     */
    private static String[] _getContacts(final String[] emailAddrs) {
        final List<String> resList = new ArrayList<>(emailAddrs.length);

        for (final String emailAddr : emailAddrs) {
            final int at = emailAddr.trim().indexOf('@');
            if (at == -1) continue;

            resList.add(emailAddr.substring(0, at));
        }

        return resList.toArray(EMPTY_STRING_ARRAY);
    }


    /**
     * Gets the observation status for the given observation, if there is one.
     */
    private static ObservationStatus _getObsStatus(final ISPObservation obs) {
        return obs.getDataObject() == null ? null : ObservationStatus.computeFor(obs);
    }

    /**
     * Determines the instrument in use by the given observation.
     */
    private static void _addInstruments(final TigraTableRow row, final ISPObservation obs) {
        final List<ISPObsComponent> obsComps = obs.getObsComponents();
        if (obsComps == null) return;

     final SPComponentBroadType instType = SPComponentBroadType.INSTRUMENT;
        for (final ISPObsComponent obsComp : obsComps) {
            final SPComponentType type = obsComp.getType();
            if (!instType.equals(type.broadType)) continue;

            row._addInstrument(type.readableStr);
        }
    }

    private SPProgramID _progId;

    private String _piName;
    private final List<String> _instruments = new ArrayList<>();

    private String[] _ngoContacts;
    private String[] _ngoEmails;
    private String[] _geminiContacts;

    private Date _lastSync;

    private final Map<ObservationStatus, Integer> _obsCounts = new TreeMap<>(new ObservationStatusComparator());
    private int _total;

    private TigraTableRow() {
    }

    public SPProgramID getProgramId() {
        return _progId;
    }

    private void _setProgramId(final SPProgramID id) {
        _progId = id;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String getPiName() {
//        return _piName;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private void _setPiName(final String piName) {
        _piName = piName;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String[] getInstruments() {
//        return _instruments.toArray(EMPTY_STRING_ARRAY);
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private void _addInstrument(final String instrument) {
        if (_instruments.contains(instrument)) return;
        _instruments.add(instrument);
        Collections.sort(_instruments);
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String[] getNgoContacts() {
//        final int len = _ngoContacts.length;
//        final String[] res = new String[len];
//        System.arraycopy(_ngoContacts, 0, res, 0, len);
//        return res;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private void _setNgoContacts(String[] ngoContacts) {
        if (ngoContacts == null) ngoContacts = EMPTY_STRING_ARRAY;
        _ngoContacts = ngoContacts;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String[] getNgoEmails() {
//        final int len = _ngoEmails.length;
//        final String[] res = new String[len];
//        System.arraycopy(_ngoEmails, 0, res, 0, len);
//        return res;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private void _setNgoEmails(String[] ngoEmails) {
        if (ngoEmails == null)  ngoEmails = EMPTY_STRING_ARRAY;
        _ngoEmails = ngoEmails;
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String[] getGeminiContacts() {
//        final int len = _geminiContacts.length;
//        final String[] res = new String[len];
//        System.arraycopy(_geminiContacts, 0, res, 0, len);
//        return res;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private void _setGeminiContacts(final String[] geminiContacts) {
        if (geminiContacts == null) _geminiContacts = EMPTY_STRING_ARRAY;
        _geminiContacts = geminiContacts;
    }

    int getObsCount(final ObservationStatus status) {
        final Integer obsCount =  _obsCounts.get(status);
        if (obsCount == null) return 0;
        return obsCount;
    }

    private void _incrementObsCount(final ObservationStatus status) {
        _total += 1;

        final Integer obsCount = _obsCounts.get(status);
        if (obsCount == null) {
            _obsCounts.put(status, ONE);
            return;
        }

        final int newValue = obsCount + 1;
        _obsCounts.put(status, newValue);
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public double getObsPercentage(final ObservationStatus status) {
//        return (double) getObsCount(status) / (double) _total;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    private int[] _getObsStatusPercentages() {
        final int[] totals = new int[] {
            getObsCount(ObservationStatus.PHASE2),
            getObsCount(ObservationStatus.FOR_REVIEW) +
                getObsCount(ObservationStatus.IN_REVIEW),
            getObsCount(ObservationStatus.FOR_ACTIVATION),
            getObsCount(ObservationStatus.ON_HOLD),
            getObsCount(ObservationStatus.INACTIVE),
            getObsCount(ObservationStatus.READY) +
                getObsCount(ObservationStatus.ONGOING),
            0
        };

        int totalNonObserved = 0;
        for (final int total : totals) {
            totalNonObserved += total;
        }

        totals[totals.length - 1] = (_total - totalNonObserved);

        return PercentUtil.getPercents(totals);
    }
    private static final String SEP = "\",\"";

    public String toString() {
        final StringBuilder buf = new StringBuilder();

        final int[] obsPercents = _getObsStatusPercentages();

        // append the program id
        buf.append("[\"").append(_progId.toString()).append(SEP);

        // append the pi name
        buf.append(_piName).append(SEP);

        // append the instruments
        final Iterator<String> it = _instruments.iterator();
        if (it.hasNext()) {
            buf.append(it.next());
        }
        while (it.hasNext()) {
            buf.append(",").append(it.next());
        }

        // append the last fetch date
        if (_lastSync != null) {
            final String dateString = format(_lastSync);
            buf.append(SEP).append(dateString);
        } else {
            buf.append(SEP).append("Never");
        }

        // append the observation percents
        buf.append("\",'").append(obsPercents[0]).append("'");
        for (int i=1; i<obsPercents.length; ++i) {
            buf.append(",'").append(obsPercents[i]).append("'");
        }

        // append the ngo contacts
        buf.append(",\"");
        if ((_ngoContacts != null) && (_ngoContacts.length > 0)) {
            buf.append(_ngoContacts[0]);
            for (int i=1; i<_ngoContacts.length; ++i) {
                buf.append(", ").append(_ngoContacts[i]);
            }
        }

        // append the gemini contacts
        buf.append(SEP);
        if ((_geminiContacts != null) && (_geminiContacts.length > 0)) {
            buf.append(_geminiContacts[0]);
            for (int i=1; i<_geminiContacts.length; ++i) {
                buf.append(", ").append(_geminiContacts[i]);
            }
        }

        // append the ngo emails
        buf.append(SEP);
        if ((_ngoEmails != null) && (_ngoEmails.length > 0)) {
            buf.append(_ngoEmails[0]);
            for (int i=1; i<_ngoEmails.length; ++i) {
                buf.append(", ").append(_ngoEmails[i]);
            }
        }
        buf.append("\"]");

        return buf.toString();
    }
}
