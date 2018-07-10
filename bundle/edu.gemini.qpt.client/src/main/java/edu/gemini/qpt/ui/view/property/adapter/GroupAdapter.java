package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;

public class GroupAdapter implements Adapter<Group> {

    public void setProperties(Variant variant, Group group, PropertyTable table) {
        table.put(PROP_TYPE, "Scheduling Group");
        table.put(PROP_SUBTYPE, group.getType());
        table.put(PROP_TITLE, group.getName());        
    }

}
