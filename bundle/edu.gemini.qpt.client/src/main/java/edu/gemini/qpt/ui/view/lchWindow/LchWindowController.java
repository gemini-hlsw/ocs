package edu.gemini.qpt.ui.view.lchWindow;

import edu.gemini.lch.services.model.ClearanceWindow;
import edu.gemini.lch.services.model.Observation;
import edu.gemini.lch.services.model.ObservationTarget;
import edu.gemini.lch.services.model.ShutteringWindow;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

import static edu.gemini.qpt.ui.view.lchWindow.LchWindowType.clearance;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class LchWindowController implements GTableController<Schedule, LchWindow, LchWindowAttribute>, PropertyChangeListener {

    private GViewer<Schedule, LchWindow> viewer;
    private LchWindow[] lchWindows;
    private Alloc alloc;
    private Schedule schedule;
    private LchWindowType windowType;

    public LchWindowController(LchWindowType windowType) {
        this.windowType = windowType;
    }

    // An instance of this class is returned by getSubElement() instead of long, in order to
    // display the clearances that overlap the selected Alloc differently.
    class Element {
        long time;
        boolean overlap;
        String targetType;
        String targetName;
    }

    public Object getSubElement(LchWindow window, LchWindowAttribute subElement) {
        if (window != null && alloc != null) {
            Element e = new Element();
            long wStart = window.getStart().getTime();
            long wEnd = window.getEnd().getTime();
            long aStart = alloc.getStart();
            long aEnd = alloc.getEnd();
            e.overlap = alloc.overlaps(new Interval(wStart, wEnd), Interval.Overlap.EITHER);
            e.targetType = window.getTargetType();
            e.targetName = window.getTargetName();
            switch (subElement) {
                case Start:
                    e.time = wStart;
                    break;
                case End:
                    e.time = wEnd;
                    break;
                case Length:
                    e.time = wEnd - wStart;
                    break;
            }
            return e;
        }
        return null;
    }

    public LchWindow getElementAt(int row) {
        return row < lchWindows.length ? lchWindows[row] : null;
    }

    public int getElementCount() {
        return lchWindows == null ? 0 : lchWindows.length;
    }

    // Sets the currently selected Alloc (used to get currently selected obs)
    void setAlloc(Alloc alloc) {
        this.alloc = alloc;
    }

    public void modelChanged(GViewer<Schedule, LchWindow> viewer, Schedule oldModel, Schedule newModel) {
        this.viewer = viewer;
        this.schedule = newModel;
        fetchLchWindows();

    }

    public void propertyChange(PropertyChangeEvent evt) {
        fetchLchWindows();
        if (viewer != null) {
            viewer.refresh();
        }
    }

    // Gets the unique LCH (shutter or clearance) windows.
    // If there are duplicate windows, they are merged with names separated by commas (same for type field).
    private synchronized void fetchLchWindows() {
        lchWindows = new LchWindow[]{};
        if (alloc != null && alloc.getObs() != null && schedule != null) {
            Observation observation = LttsServicesClient.getInstance().getObservation(alloc.getObs());
            if (observation != null) {
                Map<Interval, LchWindow> map = new LinkedHashMap<Interval, LchWindow>(); // LCH-191: Used to ignore duplicate intervals
                for (ObservationTarget observationTarget : observation.getTargetsSortedByType()) {
                    if (windowType == clearance) {
                        for (ClearanceWindow w : observationTarget.getLaserTarget().getClearanceWindows()) {
                            addToMap(map, w.getStart(), w.getEnd(), observationTarget.getType(), observationTarget.getName());
                        }
                    } else {
                        for (ShutteringWindow w : observationTarget.getLaserTarget().getShutteringWindows()) {
                            addToMap(map, w.getStart(), w.getEnd(), observationTarget.getType(), observationTarget.getName());
                        }
                    }
                }
                if (map.size() != 0) {
                    lchWindows = map.values().toArray(new LchWindow[map.size()]);

                    // sort by time
                    Arrays.sort(lchWindows, new Comparator<LchWindow>() {
                        @Override
                        public int compare(LchWindow w1, LchWindow w2) {
                            return w1.getStart().compareTo(w2.getStart());
                        }
                    });
                }
            }
        }
    }

    // Adds an entry to the given map, or if the given interval is already there, adds the type and name
    // to the existing entry, separated by commas.
    private void addToMap(Map<Interval, LchWindow> map, Date start, Date end, String type, String name) {
        Interval interval = new Interval(start.getTime(), end.getTime());
        if (map.containsKey(interval)) {
            map.put(interval, merge(map.get(interval), type, name));
        } else {
            map.put(interval, new LchWindow(start, end, type, name));
        }
    }

    // Adds the observation target type and name to the given LchWindow (separated by comma) and returns the new LchWindow
    private LchWindow merge(LchWindow lchWindow, String type, String name) {
        return new LchWindow(lchWindow.getStart(), lchWindow.getEnd(),
                lchWindow.getTargetType() + ", " + type,
                lchWindow.getTargetName() + ", " + name);
    }
}
