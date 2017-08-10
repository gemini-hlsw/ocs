package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.qpt.shared.util.TimeUtils;

/**
 * Generates Alloc markers for Allocs that overlap.
 * @author rnorris
 */
public class SetupListener extends MarkerModelListener<Variant> {

	public void propertyChange(PropertyChangeEvent evt) {
		Variant v = (Variant) evt.getSource();
		MarkerManager mm = getMarkerManager(v);		
		mm.clearMarkers(this, v);

		Alloc prev = null;
		for (Alloc a: v.getAllocs()) {

			boolean compatible = compatible(prev, a);

            // give an informational message about setup that's going to be used for compatible allocations
            if (compatible) {
                switch (a.getSetupType()) {
                    case NONE:
                        mm.addMarker(true, this, Severity.Info, "Acquisition overhead removed for this visit.", v, a);
                        break;
                    case FULL:
                        mm.addMarker(true, this, Severity.Info, String.format("It may be possible to skip the setup (%s minutes) for this visit.", TimeUtils.msToMMSS(a.getSetupTime())), v, a);
                        break;
                    case REACQUISITION:
                        mm.addMarker(true, this, Severity.Info, String.format("Using the re-acquisition overhead (%s minutes) for this visit.", TimeUtils.msToMMSS(a.getSetupTime())), v, a);
                        break;
                }

            // for incompatible allocations we must always have the full setup
            } else {
                if (a.getSetupType() == Alloc.SetupType.NONE) {
                    mm.addMarker(false, this, Severity.Error, "Setup cannot be skipped here.", v, a);
                } else if (a.getSetupType() == Alloc.SetupType.REACQUISITION) {
                    mm.addMarker(false, this, Severity.Error, "Reacquisition is not enough, full setup is required here.", v, a);
                }
            }
			
			prev = a;
		}
		
	}

	private boolean compatible(Alloc pred, Alloc succ) {
		return 
			pred != null &&
			pred.getObs().getInstrumentString().equals(succ.getObs().getInstrumentString()) &&
			pred.getObs().getCoords(pred.getVariant().getSchedule().getMiddlePoint()).equals(succ.getObs().getCoords(pred.getVariant().getSchedule().getMiddlePoint()));
	}

	@Override
	protected MarkerManager getMarkerManager(Variant t) {
		return t.getSchedule().getMarkerManager();
	}
	
}
