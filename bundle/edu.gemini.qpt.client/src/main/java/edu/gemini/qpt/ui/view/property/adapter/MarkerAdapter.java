package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;

public class MarkerAdapter implements Adapter<Marker> {

    public static final String PROP_DESCRIPTION = "Description";
    public static final String PROP_RESOURCE = "Resource";
    public static final String PROP_SEVERITY = "Severity";

    public void setProperties(Variant variant, Marker target, PropertyTable table) {
        table.put(PROP_TYPE, "Problem Marker");
        table.put(PROP_SEVERITY, target.getSeverity());
        table.put(PROP_DESCRIPTION, target.getUnionText(variant.getSchedule().getSite()));
        table.put(PROP_RESOURCE, target.getTarget());
    }

}
