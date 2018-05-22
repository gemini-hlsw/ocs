package jsky.app.ot.gemini.editor.targetComponent.tableSelection;

import edu.gemini.spModel.core.Coordinates;

final public class TableCoordinateSelection implements TableSelection {
    private final Coordinates c;

    public TableCoordinateSelection(final Coordinates c) {
        this.c = c;
    }

    public Coordinates getCoordinates() {
        return c;
    }
}
