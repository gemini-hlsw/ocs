//
// $
//

package jsky.app.ot.editor.eng;

import java.awt.*;

/**
 * An interface that can be implemented by OT editors in order to provide
 * support for engineering components.
 */
public interface EngEditor {

    /**
     * Gets the engineering component to display.
     */
    Component getEngineeringComponent();
}
