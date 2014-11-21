//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

import java.awt.*;

/**
 * An interface for editors for the various portions of an
 * {@link SPTarget}.
 */
interface TelescopePosEditor {
    /**
     * Gets the UI component used to edit the target position.
     */
    Component getComponent();

    /**
     * Informs the editor of which target position to edit.
     */
    void edit(Option<ObsContext> ctx, SPTarget target);
}
