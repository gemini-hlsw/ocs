package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor;

final class ForwardingTelescopePosWatcher implements TelescopePosEditor, TelescopePosWatcher {

    private final TelescopePosEditor tpe;

    private SPTarget spTarget;
    private Option<ObsContext> ctx;


    ForwardingTelescopePosWatcher(TelescopePosEditor tpe) {
        this.tpe = tpe;
    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {

        // If this is a new target, switch our watchers
        if (this.spTarget != spTarget) {
            if (this.spTarget != null) {
                this.spTarget.deleteWatcher(this);
            }
            this.spTarget = spTarget;
            this.spTarget.addWatcher(this);
        }

        // Remember the context and target so `telescopePosUpdate` can call `edit`
        this.ctx = obsContext;
        this.spTarget = spTarget;

    }

    public void telescopePosUpdate(WatchablePos tp) {
        if (this.ctx != null)
            tpe.edit(ctx, spTarget);
    }

}
