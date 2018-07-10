package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.qpt.core.util.Interval.Overlap;

/**
 * Generates Alloc markers for Allocs that overlap.
 * @author rnorris
 */
public class OverlappingAllocListener extends MarkerModelListener<Variant> {

    public void propertyChange(PropertyChangeEvent evt) {
        Variant v = (Variant) evt.getSource();
        MarkerManager mm = getMarkerManager(v);        
        mm.clearMarkers(this, v);

        // Allocs are sequential, so we only need to see if one overlaps with adjacent
        // ones in the array. TODO: optimize
        Alloc[] allocs = v.getAllocs().toArray(new Alloc[v.getAllocs().size()]);        
        for (int i = 0; i < allocs.length; i++) {
            for (int j = i + 1; j < allocs.length; j++) {
                if (allocs[i].overlaps(allocs[j], Overlap.EITHER)) {
                    mm.addMarker(false, this, Severity.Error, "Observation overlaps with " + allocs[j] + ".", v, allocs[i]);
                    mm.addMarker(false, this, Severity.Error, "Observation overlaps with " + allocs[i] + ".", v, allocs[j]);
                } else {
                    break;
                }
            }

        }
        
        
    }

    // TODO: add quick fixes
    

    @Override
    protected MarkerManager getMarkerManager(Variant t) {
        return t.getSchedule().getMarkerManager();
    }
    
}
