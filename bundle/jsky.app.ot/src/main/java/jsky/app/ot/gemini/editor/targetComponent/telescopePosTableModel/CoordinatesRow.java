package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.target.SPCoordinates;

/**
 * A row that contains a Coordinates object. Should not be movable.
 */
public abstract class CoordinatesRow extends Row {
    private final SPCoordinates coordinates;
    private final Option<Double> distance;

    CoordinatesRow(final boolean enabled,
                   final boolean editable,
                   final String tag,
                   final String name,
                   final SPCoordinates coordinates,
                   final Option<Coordinates> baseCoords) {
        super(enabled,
                editable,
                tag,
                name,
                null,
                None.instance());
        this.coordinates = coordinates;

        this.distance = baseCoords.map(bc -> bc.angularDistance(coordinates.getCoordinates()).toArcmins());
    }

    public SPCoordinates coordinates() {
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
        return coordinates.getCoordinates().ra().toAngle().formatHMS();
    }

    public String decStringExtractor() {
        return coordinates.getCoordinates().dec().toAngle().formatHMS();
    }
}
