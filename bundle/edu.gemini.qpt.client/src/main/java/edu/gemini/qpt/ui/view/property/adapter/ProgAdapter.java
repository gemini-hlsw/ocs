package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;
import edu.gemini.shared.util.DateTimeUtils;

public class ProgAdapter implements Adapter<Prog> {

	public void setProperties(Variant variant, Prog target, PropertyTable table) {
		table.put(PROP_TYPE, "Science Program " + target.getStructuredProgramId() + " (Band " + target.getBand() +")");
		table.put(PROP_TITLE, target.getTitle());
		if (target.getBand3RemainingTime() != null) {
			table.put(PROP_REMAINING_PROGRAM_TIME, 
					DateTimeUtils.msToHMMSS(target.getRemainingProgramTime()) +
					" (Band 3 Min. Rem. " +
					DateTimeUtils.msToHMMSS(target.getBand3RemainingTime()) +
					")");
		} else {
			table.put(PROP_REMAINING_PROGRAM_TIME, DateTimeUtils.msToHMMSS(target.getRemainingProgramTime()));
		}
	}

}
