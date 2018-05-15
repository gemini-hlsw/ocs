package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;

final class GhostSkyRow extends CoordinatesRow {
    GhostSkyRow(final String tag,
                final Coordinates coordinates,
                final Option<Coordinates> baseCoords) {
        super(true,
                true,
                tag,
                "Sky",
                coordinates,
                baseCoords);
    }
}
