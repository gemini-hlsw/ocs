package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;

final class GhostTargetRow extends TargetRow {
    GhostTargetRow(final String tag,
                   final GhostAsterism.GhostTarget target,
                   final Option<Coordinates> baseCoords,
                   final Option<Long> when) {
        super(true,
                true,
                tag,
                target.spTarget(),
                baseCoords,
                when);
    }
}