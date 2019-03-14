//
// $Id: PrivateVisitList.java 6852 2005-12-29 18:07:14Z shane $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.event.ExecEvent;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.event.StartVisitEvent;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;

import java.io.Serializable;
import java.util.*;

/**
 * An implementation class that holds all the visits and handles event
 * ordering, etc.
 */
final class PrivateVisitList implements Serializable {
    private static final long serialVersionUID = -2686488059242714341L;

    private List<PrivateVisit> _visits;

    PrivateVisitList() {
        _visits  = new ArrayList<PrivateVisit>();
    }

    PrivateVisitList(PrivateVisitList that) {
        _visits = new ArrayList<PrivateVisit>(that._visits);
        for (ListIterator<PrivateVisit> it=_visits.listIterator(); it.hasNext(); ) {
            PrivateVisit iv = it.next();
            it.set(new PrivateVisit(iv));
        }
    }

    void add(ObsExecEvent evt) {
        // First, make sure that this event is in order.
        int numVisits = _visits.size();
        PrivateVisit lastVisit = null;
        if (numVisits > 0) {
            lastVisit = _visits.get(numVisits - 1);
            if (!lastVisit.endsBefore(evt)) {
                List<ObsExecEvent> allEvents = getAllEventList();
                allEvents.add(evt);
                _rebuild(allEvents);
                return;
            }
        }

        // Now, if this is a start visit event, then add a new InternalVisit
        // for it.  Otherwise, just add it to the last visit.
        if ((evt instanceof StartVisitEvent) || (lastVisit == null)) {
            lastVisit = new PrivateVisit();
            _visits.add(lastVisit);
        }
        lastVisit.add(evt);
    }

    Site divineSite() {
        if (_visits.size() == 0) return null;

        for (PrivateVisit pv : _visits) {
            Site sd = pv.divineSite();
            if (sd != null) return sd;
        }

        return null;
    }

    List<ObsExecEvent> getAllEventList() {
        List<ObsExecEvent> res = new ArrayList<ObsExecEvent>();
        for (PrivateVisit pv : _visits) {
            res.addAll(pv._events);
        }
        return res;
    }

    private void _rebuild(List<ObsExecEvent> eventList) {
        Collections.sort(eventList, ExecEvent.TIME_COMPARATOR);
        _visits.clear();

        PrivateVisit lastVisit = null;
        for (ObsExecEvent evt : eventList) {
            if ((lastVisit == null) || (evt instanceof StartVisitEvent)) {
                lastVisit = new PrivateVisit();
                _visits.add(lastVisit);
            }
            lastVisit.add(evt);
        }
    }

    long getTotalTime() {
        long time = 0;
        for (PrivateVisit pv : _visits ) {
            time += pv.getTotalTime();
        }
        return time;
    }

    long getLastEventTime() {
        int sz = _visits.size();
        for (int i=sz-1; i>=0; ++i) {
            PrivateVisit visit = _visits.get(i);
            ObsExecEvent evt = visit.getLastEvent();
            if (evt != null) return evt.getTimestamp();
        }

        return 0;
    }

    ObsTimeCharges getTimeCharges(
        Option<Instrument> instrument,
        ObsClass           oc,
        ChargeClass        mainChargeClass,
        ObsQaRecord        qa,
        ConfigStore        store
    ) {

        VisitTimes vt = new VisitTimes();
        for (PrivateVisit pv : _visits) {
            vt.addVisitTimes(pv.getTimeCharges(instrument, oc, qa, store));
        }

        return vt.getTimeCharges(mainChargeClass);
    }

    ObsVisit[] getObsVisits(Option<Instrument> instrument, ObsClass oc, ObsQaRecord qa, ConfigStore store) {
        List<ObsVisit> obsVisitList = new ArrayList<ObsVisit>();
        for (PrivateVisit pv : _visits) {
            obsVisitList.add(pv.toObsVisit(instrument, oc, qa, store));
        }
        return obsVisitList.toArray(ObsVisit.EMPTY_ARRAY);
    }

    ObsVisit[] getObsVisits(Option<Instrument> instrument, ObsClass oc, ObsQaRecord qa, ConfigStore store, long startTime, long endTime) {
        List<ObsVisit> obsVisitList = null;
        for (PrivateVisit pv : _visits) {
            ObsExecEvent evt = pv.getFirstEvent();
            if (evt == null) continue;

            long visitStart = evt.getTimestamp();
            if ((startTime <= visitStart) && (visitStart < endTime)) {
                if (obsVisitList == null) obsVisitList = new ArrayList<ObsVisit>();
                obsVisitList.add(pv.toObsVisit(instrument, oc, qa, store));
            }
        }
        if (obsVisitList == null) return ObsVisit.EMPTY_ARRAY;
        return obsVisitList.toArray(ObsVisit.EMPTY_ARRAY);
    }

    public ObsExecStatus getObsExecStatus() {
        // Walk through the events backwards.  The most relevant events are the
        // last ones.
        ObsExecStatus result = ObsExecStatus.PENDING;
        for (int i=_visits.size()-1; i>=0; --i) {
            final PrivateVisit pv = _visits.get(i);
            final ObsExecStatus s = pv.getExecStatus();
            if (s != ObsExecStatus.PENDING) {
                result = s;
                break;
            }
        }
        return result;
    }
}
