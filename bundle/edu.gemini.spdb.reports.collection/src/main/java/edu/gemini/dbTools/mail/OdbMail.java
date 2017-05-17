package edu.gemini.dbTools.mail;

import edu.gemini.dbTools.odbState.ObservationState;
import edu.gemini.dbTools.odbState.ProgramEmailAddresses;
import edu.gemini.dbTools.odbState.ProgramState;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.too.TooType;

import java.util.*;
import java.util.logging.Logger;

public class OdbMail {
    private static final OdbMail[] EMPTY_MAIL_ARRAY = new OdbMail[0];

    private final SPProgramID _progId;
    private final OdbMailEvent _mailEvent;
    private final List<SPObservationID> _obsIds;

    private ProgramInternetAddresses _inetAddresses;

    private OdbMail(final SPProgramID progId, final OdbMailEvent mailEvent, final List<SPObservationID> obsIds) {
        _progId = progId;
        _mailEvent = mailEvent;
        _obsIds = obsIds;
    }

    private OdbMail(final SPProgramID progId, final OdbMailEvent mailEvent, final List<SPObservationID> obsIds,
                    final ProgramInternetAddresses inetAddresses) {
        this(progId, mailEvent, obsIds);
        _inetAddresses = inetAddresses;
    }

    public SPProgramID getProgramId() {
        return _progId;
    }

    public OdbMailEvent getMailEvent() {
        return _mailEvent;
    }

    public List<SPObservationID> getObsIds() {
        return Collections.unmodifiableList(_obsIds);
    }

    public ProgramInternetAddresses getInternetAddresses() {
        return _inetAddresses;
    }

    /**
     * Creates OdbMail instances for a particular program.  Compares the last
     * known status of each observation in the program to the current status
     * to determine which email events have occurred.
     *
     * @param oldProg the last known status of each observation in the prog
     * @param newProg the current status of each observation in the prog
     *
     * @return any OdbMail instances which should be sent as a result of
     * observation status updates; may be an empty arrary if there are none
     */
    public static OdbMail[] create(final Logger log,
                                   final ProgramState oldProg,
                                   final ProgramState newProg) {

        // Compare each observation in the new report to the matching obs
        // in the old report.  If the status doesn't match, check whether
        // an email event should be generated.
        final SortedMap<SPObservationID, ObservationState> oldObsMap = oldProg.getObservations();
        final SortedMap<SPObservationID, ObservationState> newObsMap = newProg.getObservations();

        final Map<OdbMailEvent, List<SPObservationID>> eventMap = new HashMap<>();  // key is OdbMailEvent
        for (final Object o : newObsMap.values()) {
            final ObservationState newObs = (ObservationState) o;

            // Get the last known status of the observation.  If not
            // previously known, then assume it was Phase2.

            ObservationStatus oldStatus = ObservationStatus.PHASE2;
            final SPObservationID obsId = newObs.getObservationId();
            final ObservationState oldObs = oldObsMap.get(obsId);
            if (oldObs != null) oldStatus = oldObs.getStatus();

            // Okay, compare the status values.
            final ObservationStatus newStatus = newObs.getStatus();
            if (newStatus.equals(oldStatus)) continue; // nothing changed

            // Okay, they have different status values.  See if there is
            // an email event associated with this change.
            final OdbMailEvent event = OdbMailEvent.lookup(oldStatus, newStatus);
            if (event == null) continue;  // nobody cares about this change

            // Filter out "On Hold" messages unless this is a ToO observation.
            if ((newStatus == ObservationStatus.ON_HOLD) && (newObs.getTooType() == TooType.none)) continue;

            // Finally, we know that we have to send an email for this
            // observation.  Record that fact.
            List<SPObservationID> obsIdList = eventMap.get(event);
            if (obsIdList == null) {
                obsIdList = new ArrayList<>();
                eventMap.put(event, obsIdList);
            }
            obsIdList.add(obsId);
        }

        // Now, if there were any events, then get the email addresses for
        // this program.  They will be used for all events.
        final int eventCount = eventMap.size();
        if (eventCount == 0) return EMPTY_MAIL_ARRAY;

        final ProgramEmailAddresses emailAddrs = newProg.getProgramContact().getEmailAddresses();

        final ProgramInternetAddresses inetAddrs;
        inetAddrs = new ProgramInternetAddresses(log, emailAddrs);

        // For each email event, create an OdbEmail object.
        final OdbMail[] res = new OdbMail[eventMap.size()];
        int i = 0;
        for (Iterator<OdbMailEvent> it = eventMap.keySet().iterator(); it.hasNext(); ++i) {
            final OdbMailEvent evt = it.next();
            final List<SPObservationID> obsIdList = eventMap.get(evt);
            Collections.sort(obsIdList);
            final OdbMail mail = new OdbMail(newProg.getProgramId(), evt,
                                       obsIdList, inetAddrs);
            res[i] = mail;
        }
        return res;
    }
}

