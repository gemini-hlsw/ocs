package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;

/**
 * Generates Variant markers if the Variant contains no Allocs.
 * @author rnorris
 */
public class EmptyVariantListener extends MarkerModelListener<Variant> {

    public void propertyChange(PropertyChangeEvent evt) {
        Variant v = (Variant) evt.getSource();
        MarkerManager mm = v.getSchedule().getMarkerManager();
        mm.clearMarkers(this, v);

        if (v.isEmpty())
            mm.addMarker(true, this, Severity.Info, "Variant is empty.", v);
        
    }

    @Override
    protected MarkerManager getMarkerManager(Variant t) {
        return t.getSchedule().getMarkerManager();
    }
    
}

                    
