//
// $Id$
//

package jsky.app.ot.editor.seq;

import edu.gemini.spModel.config2.ItemKey;

import java.awt.*;

/**
 * Renders cells in the static configuration table of the sequence tab
 */
public class StaticConfigurationCellRenderer extends SequenceCellRenderer {
    private StaticConfigurationTableModel _model;

    StaticConfigurationCellRenderer(StaticConfigurationTableModel model) {
        _model = model;
    }

    protected Color lookupColor(int row, int column) {
        ItemKey key = _model.getItemKeyAt(row);
        return lookupColor(key);
    }
}
