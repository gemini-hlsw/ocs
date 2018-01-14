package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.shared.util.DateTimeUtils;

public class OverAllocationListener extends MarkerModelListener<Variant> {

	public void propertyChange(PropertyChangeEvent evt) {
		
		Variant v = (Variant) evt.getSource();
		MarkerManager mm = getMarkerManager(v);		
		mm.clearMarkers(this, v);

		Map<Prog, Long> time = new HashMap<Prog, Long>();
		Iterable<Alloc> allocs = v.getAllocs();
		
		// First determine the remaining time after subtracting what's in the
		// plan so far.
		for (Alloc a: allocs) {
			Prog p = a.getObs().getProg();
			if (time.get(p) == null)
				time.put(p, 0L);
			switch (a.getObs().getObsClass()) {
			case ACQ:
			case PROG_CAL:
			case SCIENCE:
				time.put(p, time.get(p) + a.getLength());
			}
			
		}
		
		// Now report errors
		for (Alloc a: allocs) {
			Prog p = a.getObs().getProg();
            //HACK: Don't check Engineering or daily calibration programs
            if ( p.isEngOrCal() ) continue;

            switch (a.getObs().getObsClass()) {
            case ACQ:
            case PROG_CAL:
            case SCIENCE:
                long used = time.get(p);
                if (used > p.getRemainingProgramTime()) {
                    String msg = "Program " + p + " is over-allocated by " + DateTimeUtils.msToHMMSS(used - p.getRemainingProgramTime()) + ".";
                    mm.addMarker(true, this, Severity.Warning, msg, v, a);
                } else if (p.getBand3RemainingTime() != null && used > p.getBand3RemainingTime()) {
                    String msg = "Program " + p + " is allocated " + DateTimeUtils.msToHMMSS(used - p.getBand3RemainingTime()) + " past its Band 3 minumum success time.";
                    mm.addMarker(true, this, Severity.Warning, msg, v, a);
                }
            }
		}
		
		
	}

	@Override
	protected MarkerManager getMarkerManager(Variant t) {
		return t.getSchedule().getMarkerManager();
	}
	
}
