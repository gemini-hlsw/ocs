package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.env.TargetEnvironment;

/**
 * A base position which is a set of Coordinates rather than a physical
 * target. This may come up in GHOST.
 */
final class BaseCoordinatesRow extends CoordinatesRow {
    BaseCoordinatesRow(final SPCoordinates coordinates, boolean enabled) {
        super(enabled,
                true,
                TargetEnvironment.BASE_NAME,
                "Base",
                coordinates,
                None.instance());
    }
}
