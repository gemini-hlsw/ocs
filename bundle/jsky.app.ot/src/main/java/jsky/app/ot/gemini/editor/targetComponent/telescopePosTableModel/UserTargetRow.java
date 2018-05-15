package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.target.env.UserTarget;

final class UserTargetRow extends TargetRow {
    UserTargetRow(final int index,
                  final UserTarget u,
                  final Option<Coordinates> baseCoords,
                  final Option<Long> when) {
        super(true,
                true,
                String.format("%s (%d)", u.type.displayName, index),
                u.target,
                baseCoords,
                when);
    }
}
