package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Block;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.qpt.core.util.Interval.Overlap;

/**
 * Generates Alloc markers if the alloc does not fall within a scheduling block.
 * @author rnorris
 */
public class OutsideBlockListener extends MarkerModelListener<Variant> {

    public void propertyChange(PropertyChangeEvent evt) {
        Variant v = (Variant) evt.getSource();
        Schedule s = v.getSchedule();
        MarkerManager mm = s.getMarkerManager();
        mm.clearMarkers(this, v);
        
        // Find allocs that overlap with a block partially.
        Set<Alloc> outsiders = new TreeSet<Alloc>();
        for (final Block b: s.getBlocks()) {
            SortedSet<Alloc> allocs = v.getAllocs(b, Overlap.PARTIAL);
            outsiders.addAll(allocs);
        }
        for (Alloc a: outsiders) {
            mm.addMarker(false, this, Marker.Severity.Warning, "Observation extends past scheduling block boundary.", v, a);
        }
        
        // Find allocs that don't fall into any block
        Set<Alloc> orphans = new TreeSet<Alloc>(v.getAllocs());
        for (final Block b: s.getBlocks()) {
            SortedSet<Alloc> allocs = v.getAllocs(b, Overlap.EITHER);
            orphans.removeAll(allocs);
        }
        for (Alloc a: orphans) {
            mm.addMarker(false, this, Marker.Severity.Warning, "Observation lies outside scheduling block.", v, a);
        }
        
    }
    
    @Override
    protected MarkerManager getMarkerManager(Variant t) {
        return t.getSchedule().getMarkerManager();
    }
    
}
