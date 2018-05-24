package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.skycalc.Night;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.event.EndDatasetEvent;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.event.OverlapEvent;
import edu.gemini.spModel.event.StartDatasetEvent;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.time.ChargeClass;

import java.util.List;
import java.util.function.Function;
import java.time.Instant;

/**
 * This is the algorithm that has been in use since the beginning of time
 * accounting.  It is confusing, hard to follow, and suffers from several (now)
 * known bugs:
 *
 * 1. It fails to charge for the portion of a dataset after a start event but
 * before an overlap event.
 *
 * 2. In general, intervening dataset events that split start/end events are not
 * charged correctly in all cases.
 *
 * 3. If the first event should happen to be a start dataset event then the
 * first dataset isn't charged.
 *
 * Nevertheless, we cannot change time accounting for past observations so none
 * of these bugs will be fixed in the PrimordialVisitCalculator below.  The
 * code is essentially unchanged but instead simply lifted from `PrivateVisit`
 * and given a calc method with fewer dependencies.
 */
enum PrimordialVisitCalculator {
    instance;

    // Tries to compute the SiteDesc for this visit based upon the events (if
    // any).  Uses the prefix of the observation id (GN or GS) to guess at the
    // site.  May return <code>null</code> if there are no events or if the
    // events do not have valid observation ids.
    public static Site divineSite(ObsExecEvent e) {
        if (e == null) return null;

        SPObservationID obsId = e.getObsId();
        String obsIdStr = obsId.stringValue().toLowerCase();
        if (obsIdStr.startsWith("gs-")) {
            return Site.GS;
        } else if (obsIdStr.startsWith("gn-")) {
            return Site.GN;
        }

        return null;
    }

    private static class EventData {
        ObsExecEvent evt;         // event itself
        Site site;
        ObservingNight obsNight;  // observing night of the event
        Night darkNight;          // subset of the ObservingNight that was dark

        EventData(Site site) {
            this.site = site;
        }

        void setEvent(ObsExecEvent evt) {
            this.evt = evt;
            if ((site == null) ||
                ((obsNight != null) && obsNight.includes(evt.getTimestamp()))) {
                // no need to reset the obsNight or dark night
                return;
            }

            obsNight  = new ObservingNight(site, evt.getTimestamp());
            darkNight = TwilightBoundedNight.forObservingNight(TwilightBoundType.NAUTICAL, obsNight);
        }

        void updateToMatch(EventData that) {
            this.evt       = that.evt;
            this.site      = that.site;
            this.obsNight  = that.obsNight;
            this.darkNight = that.darkNight;
        }
    }

    private static class EventInterval {
        long totalTime;
        long nightTime;

        long getDaytime() {
            return totalTime - nightTime;
        }
    }

    static void computeEventInterval(EventData e1, EventData e2, EventInterval inv) {
        long t1 = e1.evt.getTimestamp();
        long t2 = e2.evt.getTimestamp();

        // assertion
        if (t1 > t2) {
            throw new IllegalArgumentException("out of order events");
        }

        inv.totalTime = t2 - t1;
        inv.nightTime = 0;

        if (e1.obsNight == null) {
            // no site information, so just assume it is all night time
            // this shouldn't happen in practice
            inv.nightTime = inv.totalTime;
            return;
        }

        long n1start = e1.darkNight.getStartTime();
        long n1end   = e1.darkNight.getEndTime();

        long start = t1;  // assume t1 is in the dark part of the obs night 1
        if (t1 < n1start) {
            // was before the dark time begins on obs night 1
            start = n1start;
        } else if (t1 >= n1end) {
            // was after the dark time is over on obs night 1
            start = -1;
        }

        long n2start = e2.darkNight.getStartTime();
        long n2end   = e2.darkNight.getEndTime();

        long end = t2; // assume t2 is in the dark park of the obs night 2
        if (t2 < n2start) {
            // was before the dark time begins on obs night 2
            end = -1;
        } else if (t2 >= n2end) {
            end = n2end;
        }

        // First handle the common case, both events are in the same observing
        // night.
        if (e1.obsNight.includes(t2)) {
            if ((start != -1) && (end != -1)) {
                inv.nightTime = end - start;
            } // else no nighttime for this event
            return;
        }

        // Now handle the unusual case, events on different observing nights.
        long nighttime = 0;

        // First get the amount of nighttime on the first night.
        if (start != -1) {
            nighttime = n1end - start;
        }

        // Add in the amount of nighttime on the last night
        if (end != -1) {
            nighttime += end - n2start;
        }

        // Now, if there are observing nights between the two events, add in
        // all the night time for those nights.
        Site site = e1.obsNight.getSite();
        ObservingNight curNight = new ObservingNight(site, e1.obsNight.getEndTime());
        while (curNight.getStartTime() < e2.obsNight.getStartTime()) {
            Night darkNight = TwilightBoundedNight.forObservingNight(TwilightBoundType.NAUTICAL, curNight);
            nighttime += darkNight.getTotalTime();
            curNight   = new ObservingNight(site, curNight.getEndTime());
        }
        inv.nightTime = nighttime;
    }

    public VisitTimes calc(
        List<ObsExecEvent>                     events,
        Function<DatasetLabel, DatasetQaState> qaState,
        Function<DatasetLabel, ObsClass>       obsClass
    ) {

        VisitTimes res = new VisitTimes();
        if (events.size() <= 1) return res;

        // First get the site.  If we can't figure out the site, then this is
        // a non-standard observation and we'll ignore daytime.
        Site site = divineSite(events.get(0));
        EventData ed1 = new EventData(site);
        ed1.setEvent(events.get(0));
        EventData ed2 = new EventData(site);
        ed2.updateToMatch(ed1);

        StartDatasetEvent start = null;  // BUG - if the first event is a start event it will be ignored
        EventInterval inv = new EventInterval();

        int sz = events.size();
        for (int i=1; i<sz && !(ed1.evt instanceof OverlapEvent); ++i) {
            ObsExecEvent curEvt = events.get(i);

            // compute the total, dark, and day times between the two events
            ed2.setEvent(curEvt);
            computeEventInterval(ed1, ed2, inv);
            ed1.updateToMatch(ed2);

            // Add in the daytime to the non-charged column.
            res.addClassifiedTime(ChargeClass.NONCHARGED, inv.getDaytime());

            // Now add in the nighttime for the event
            if ((curEvt instanceof EndDatasetEvent) && (start != null)) {
                EndDatasetEvent end = (EndDatasetEvent) curEvt;
                DatasetLabel label = end.getDatasetLabel();
                if (!label.equals(start.getDataset().getLabel())) {
                    continue;
                }

                final ObsClass       oc = obsClass.apply(label);
                final DatasetQaState qa = qaState.apply(label);

                // Charge for datasets that were in the PASS or
                // some non-final state.  Don't charge for anything else.
                if (DatasetQaState.PASS.equals(qa) || !qa.isFinal()) {
                    res.addClassifiedTime(oc.getDefaultChargeClass(), inv.nightTime);
                } else {
                    res.addClassifiedTime(ChargeClass.NONCHARGED, inv.nightTime);
                }
            } else {
                // Add the amount of time that passed during the night to the
                // main charge class.
                // BUG: events that fall between start and end cause the time
                // to be unclassified
                res.addUnclassifiedTime(inv.nightTime);
                if (curEvt instanceof StartDatasetEvent) {
                    start = (StartDatasetEvent) curEvt;
                }
            }
        }

        // Add any overlapped time to NONCHARGED.  All the time between the
        // overlap event (if there is one) and the last event is counted as
        // non-charged.
        // BUG: if the overlap event falls between start and end, the time
        // between start and overlap is dropped
        if (ed1.evt instanceof OverlapEvent) {
            ObsExecEvent curEvt = events.get(sz-1);
            long time = curEvt.getTimestamp() - ed1.evt.getTimestamp();
            res.addClassifiedTime(ChargeClass.NONCHARGED, time);
        }

        return res;
    }

}
