//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;

/**
 * An abstract implementation of {@link TelescopePosWatcher} that keeps track
 * of the {@link SPTarget} being edited.  Concrete subclasses are required to
 * implement the {@link #reinit} method to initialize their widgets based upon
 * the current target.  Of course, they also must define the
 * {@link #getComponent} method of TelescopePosWatcher.
 */
abstract class AbstractTelescopePosEditor implements TelescopePosEditor {
    protected final TelescopePosWatcher watcher = new TelescopePosWatcher() {
        @Override public void telescopePosLocationUpdate(WatchablePos tp) { }
        @Override public void telescopePosGenericUpdate(WatchablePos tp) {
            reinit();
        }
    };

    private SPTarget target;

    /**
     * Gets the telescope position currently being edited.
     */
    protected SPTarget getTarget() {
        return target;
    }

    /**
     * Implements edit to keep track of watching for telescope pos updates.
     * @param target new target to edit
     */
    @Override
    public void edit(final Option<ObsContext> ctx, final SPTarget target) {
        if (this.target == target) return;
        if (this.target != null) this.target.deleteWatcher(watcher);
        if (target != null) target.addWatcher(watcher);

        this.target = target;
        reinit();
    }

    /**
     * Called to reinitialize the widgets in this editor to match the current
     * target (which can be obtained via {@link #getTarget}).
     */
    protected abstract void reinit();
}
