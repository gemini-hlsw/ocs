//
// $Id: PrivateVisit.java 8169 2007-10-10 14:09:26Z swalker $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obsclass.ObsClass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation class that holds the events for a single visit, plus
 * a few methods for working with them.
 */
final class PrivateVisit implements Serializable {
    private static final long serialVersionUID = -7796307171519915226L;

    List<ObsExecEvent> _events = new ArrayList<>();

    PrivateVisit() {
    }

    PrivateVisit(PrivateVisit that) {
        // each event is immutable, no need to copy them
        _events = new ArrayList<>(that._events);
    }

    void add(ObsExecEvent evt) {
        _events.add(evt);
    }

    ObsExecEvent getFirstEvent() {
        int sz = _events.size();
        if (sz == 0) return null;
        return _events.get(0);
    }

    ObsExecEvent getLastEvent() {
        int sz = _events.size();
        if (sz == 0) return null;
        return _events.get(sz-1);
    }

    boolean isEmpty() {
        return _events.isEmpty();
    }

    boolean nonEmpty() {
        return !isEmpty();
    }

    boolean endsBefore(ObsExecEvent evt) {
        ObsExecEvent lastEvt = getLastEvent();
        if (lastEvt == null) return true;
        return lastEvt.getTimestamp() < evt.getTimestamp();
    }

    long getTotalTime() {
        ObsExecEvent firstEvent = getFirstEvent();
        ObsExecEvent lastEvent  = getLastEvent();
        if ((firstEvent == null) || (lastEvent == null)) return 0;
        return lastEvent.getTimestamp() - firstEvent.getTimestamp();
    }

    ObsExecEvent[] getEvents() {
        return _events.toArray(ObsExecEvent.EMPTY_ARRAY);
    }

    // Tries to compute the SiteDesc for this visit based upon the events (if
    // any).  Uses the prefix of the observation id (GN or GS) to guess at the
    // site.  May return <code>null</code> if there are no events or if the
    // events do not have valid observation ids.
    Site divineSite() {
        ObsExecEvent e = getFirstEvent();
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

    ObsVisit toObsVisit(
        Option<Instrument>   instrument,
        ObsClass             oc,
        ObsQaRecord          qa,
        ConfigStore          store,
        VisitTimes           visitTimes
    ) {
        final List<UniqueConfig> uniqueConfigs = new ArrayList<>();

        Config lastConfig = null;
        long starttime = -1;
        long curtime   = -1;
        final List<DatasetLabel> uniqueConfigLabels = new ArrayList<>();

        for (ObsExecEvent event : _events) {

            if (event instanceof StartDatasetEvent) {
                curtime = event.getTimestamp();
                continue;
            }

            // Only end datasets matter, when it comes to forming unique
            // configs.  If there is no end dataset, then the dataset was
            // never completed.

            if (!(event instanceof EndDatasetEvent)) continue;

            EndDatasetEvent ede = (EndDatasetEvent) event;
            DatasetLabel label = ede.getDatasetLabel();

            Config config = store.getConfigForDataset(label);
            if (config == null) {
                // was removed or never existed
                continue;
            }

            if ((lastConfig != null) && !lastConfig.equals(config)) {
                // add the last unique config to the list
                DatasetLabel[] labels;
                labels = uniqueConfigLabels.toArray(DatasetLabel.EMPTY_ARRAY);
                uniqueConfigs.add(new UniqueConfig(lastConfig, starttime, labels));
                uniqueConfigLabels.clear();
                starttime = -1;
            }

            lastConfig = config;
            uniqueConfigLabels.add(label);
            if (starttime == -1) {
                if (curtime == -1) {
                    // there was no start dataset event for some reason,
                    // so just use the end dataset time
                    curtime = event.getTimestamp();
                }
                starttime = curtime;
            }
        }

        if (lastConfig != null) {
            DatasetLabel[] labels;
            labels = uniqueConfigLabels.toArray(DatasetLabel.EMPTY_ARRAY);
            uniqueConfigs.add(new UniqueConfig(lastConfig, starttime, labels));
        }

        final UniqueConfig[] uconfigs;
        uconfigs = uniqueConfigs.toArray(UniqueConfig.EMPTY_ARRAY);

        return new ObsVisit(getEvents(), uconfigs, visitTimes);
    }

    // Implement as done in 2013B June release.  StartSequence is the only
    // event that triggers ONGOING and EndSequence triggers OBSERVED.
    ObsExecStatus getExecStatus() {
        ObsExecStatus result = ObsExecStatus.PENDING;
        for (int i=_events.size()-1; i>=0; --i) {
            final ObsExecEvent evt = _events.get(i);
            if (evt instanceof EndSequenceEvent) {
                result = ObsExecStatus.OBSERVED;
                break;
            } else if (evt instanceof StartSequenceEvent) {
                result = ObsExecStatus.ONGOING;
                break;
            }
        }
        return result;
    }
}
