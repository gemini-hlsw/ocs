package edu.gemini.qpt.ui.view.mask;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.shared.util.immutable.ImOption;

import edu.gemini.spModel.ictd.IctdSummary;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

import edu.gemini.qpt.core.Schedule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;


public final class MaskController implements GTableController<Schedule, Map.Entry<CustomMaskKey, Availability>, MaskAttribute>, PropertyChangeListener {

    private GViewer<Schedule, Map.Entry<CustomMaskKey, Availability>> viewer;
    @SuppressWarnings("rawtypes")
    private Map.Entry<CustomMaskKey, Availability>[] entries = new Map.Entry[0];
    private Schedule schedule;

    @Override
    public Object getSubElement(Map.Entry<CustomMaskKey, Availability> element, MaskAttribute subElement) {
        switch (subElement) {
            case Name:         return element.getKey().format();
            case Availability: return element.getValue();
            default:           throw new IllegalArgumentException(subElement.name());
        }
    }

    @Override
    public synchronized Map.Entry<CustomMaskKey, Availability> getElementAt(int row) {
        return entries[row];
    }

    @Override
    public synchronized int getElementCount() {
        return entries.length;
    }

    @Override
    public synchronized void modelChanged(GViewer<Schedule, Map.Entry<CustomMaskKey, Availability>> viewer, Schedule oldModel, Schedule newModel) {
        this.viewer = viewer;
        if (oldModel != null) oldModel.removePropertyChangeListener(Schedule.PROP_ICTD, this);
        this.schedule = newModel;
        if (newModel != null) newModel.addPropertyChangeListener(Schedule.PROP_ICTD, this);
        fetchMasks();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        fetchMasks();
        viewer.refresh();
    }

    private synchronized void fetchMasks() {
        final Map<CustomMaskKey, Availability> masks;
        masks = ImOption.apply(schedule)
                        .flatMap(s -> schedule.getIctd())
                        .map(IctdSummary::maskAvailabilityJava)
                        .getOrElse(Collections::emptyMap);

        this.entries = masks.entrySet().toArray(new Map.Entry[masks.size()]);

        Arrays.sort(entries, (e1, e2) -> CustomMaskKey.OrderingCustomMaskKey().compare(e1.getKey(), e2.getKey()));
    }
}
