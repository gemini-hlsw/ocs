package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;

/**
 * An interface for editors for the various portions of an
 * {@link SPTarget}.
 */
@FunctionalInterface
public interface TelescopePosEditor<T extends SPSkyObject> {

    /**
     * Informs the editor of which target position to edit.
     */
    void edit(Option<ObsContext> ctx, T obj, ISPNode node);
}
