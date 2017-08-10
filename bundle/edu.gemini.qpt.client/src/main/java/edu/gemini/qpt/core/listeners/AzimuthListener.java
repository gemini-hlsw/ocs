package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.ApproximateAngle;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.core.util.Union;
import edu.gemini.qpt.ui.util.AzimuthSolver;

/**
 * Generates Alloc markers if the alloc reaches elevation or airmass limits.
 * @author rnorris
 */
public class AzimuthListener extends MarkerModelListener<Variant> {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(AzimuthListener.class.getName());
	
	public void propertyChange(PropertyChangeEvent evt) {
	
		// Get the variant and schedule, then clear the markers.
		Variant variant = (Variant) evt.getSource();
		Schedule schedule = variant.getSchedule();
		MarkerManager markerManager = schedule.getMarkerManager();
		markerManager.clearMarkers(this, variant);

		// Add a new marker if needed
		ApproximateAngle windConstraint = variant.getWindConstraint();
		if (windConstraint != null) {
			for (Alloc a: variant.getAllocs()) {
				AzimuthSolver solver = new AzimuthSolver(schedule.getSite(), a.getObs().getCoords(schedule.getMiddlePoint()), windConstraint);
				Union<Interval> windyBits = solver.solve(a.getInterval());
				if (!windyBits.isEmpty()) {
					long timePointedIntoWind = 0; // ms
					for (Interval i: windyBits) timePointedIntoWind += i.getLength();
					if (timePointedIntoWind > TimeUtils.MS_PER_SECOND) {
						String msg = "Telescope will be pointed into the wind for " + TimeUtils.msToHHMMSS(timePointedIntoWind) + ".";
						markerManager.addMarker(false, this, Severity.Warning, msg, variant, a);
					}
				}
			}
		
		}
			
	}
	
	@Override
	protected MarkerManager getMarkerManager(Variant t) {
		return t.getSchedule().getMarkerManager();
	}
	
}
