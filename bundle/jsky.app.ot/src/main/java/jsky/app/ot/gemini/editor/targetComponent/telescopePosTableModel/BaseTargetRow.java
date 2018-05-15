package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;

/**
 * A row representing a base target.
 * Note: there is no distance from a base position to itself, so None
 *       is returned.
 */
final class BaseTargetRow extends TargetRow {
    BaseTargetRow(final SPTarget target,
                  final Option<Long> when) {
        super(true,
                true,
                TargetEnvironment.BASE_NAME,
                target,
                None.instance(),
                when);
    }

    @Override
    public boolean movable() {
        return false;
    }
}
