package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;

/**
 * A row that contains a Coordinates object. Should not be movable.
 */
abstract class CoordinatesRow extends Row {
    private final Coordinates coordinates;
    private final Option<Double> distance;

    CoordinatesRow(final boolean enabled,
                   final boolean editable,
                   final String tag,
                   final String name,
                   final Coordinates coordinates,
                   final Option<Coordinates> baseCoords) {
        super(enabled,
                editable,
                tag,
                name,
                null,
                None.instance());
        this.coordinates = coordinates;

        this.distance = baseCoords.map(bc -> bc.angularDistance(coordinates).toArcmins());
    }

    public Coordinates coordinates() {
        return coordinates;
    }

    // The coordinates here don't have a concept of "when", so distance will
    // not be exact.
    @Override
    public Option<Double> distance() {
        return distance;
    }

    @Override
    public boolean movable() {
        return false;
    }

    public String raStringExtractor() {
        return coordinates.ra().toAngle().formatHMS();
    }

    public String decStringExtractor() {
        return coordinates.dec().toAngle().formatHMS();
    }
}
