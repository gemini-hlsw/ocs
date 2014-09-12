package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;

public class VariantAdapter implements Adapter<Variant> {

	public void setProperties(Variant variant, Variant target, PropertyTable table) {
		table.put(PROP_TYPE, "Plan Variant");
		table.put(PROP_TITLE, target.getName());
		table.put(PROP_CONSTRAINTS, target.getConditions());
		if (target.getWindConstraint() != null)
			table.put(PROP_WIND, target.getWindConstraint());
		else
			table.put(PROP_WIND, "\u00ABnone\u00BB");
	}

}
