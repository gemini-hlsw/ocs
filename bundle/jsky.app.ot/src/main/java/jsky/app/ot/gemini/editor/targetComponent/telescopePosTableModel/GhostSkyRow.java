package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.target.SPCoordinates;

final class GhostSkyRow extends CoordinatesRow {
    GhostSkyRow(final String tag,
                final SPCoordinates coordinates,
                final Option<Coordinates> baseCoords) {
        super(true,
                true,
                tag,
                "Sky",
                coordinates,
                baseCoords);
    }
}
