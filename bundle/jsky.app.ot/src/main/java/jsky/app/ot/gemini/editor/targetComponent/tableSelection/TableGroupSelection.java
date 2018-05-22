package jsky.app.ot.gemini.editor.targetComponent.tableSelection;

import edu.gemini.spModel.target.env.IndexedGuideGroup;

final public class TableGroupSelection implements TableSelection {
    private final IndexedGuideGroup igg;

    public TableGroupSelection(final IndexedGuideGroup igg) {
        this.igg = igg;
    }

    public IndexedGuideGroup getGroup() {
        return igg;
    }
}
