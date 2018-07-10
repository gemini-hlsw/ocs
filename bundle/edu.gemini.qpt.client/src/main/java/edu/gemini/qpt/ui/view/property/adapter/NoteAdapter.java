package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Note;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;

public class NoteAdapter implements Adapter<Note> {

    public void setProperties(Variant variant, Note target, PropertyTable table) {
        table.put(PROP_TYPE, "Note");
        table.put(PROP_TITLE, target.getTitle());
    }

}
