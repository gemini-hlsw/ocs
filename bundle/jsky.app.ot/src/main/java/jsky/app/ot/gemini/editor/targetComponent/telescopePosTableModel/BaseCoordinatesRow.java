package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.target.env.TargetEnvironment;

/**
 * A base position which is a set of Coordinates rather than a physical
 * target. This may come up in GHOST.
 */
final class BaseCoordinatesRow extends CoordinatesRow {
    BaseCoordinatesRow(final Coordinates coordinates) {
        super(true,
                true,
                TargetEnvironment.BASE_NAME,
                "Sky",
                coordinates,
                None.instance());
    }
}
