package jsky.app.ot.gemini.editor.targetComponent.tableSelection;

import edu.gemini.spModel.target.SPTarget;

final public class TableTargetSelection implements TableSelection {
    private final SPTarget t;

    public TableTargetSelection(final SPTarget t) {
        this.t = t;
    }

    public SPTarget getTarget() {
        return t;
    }
}
