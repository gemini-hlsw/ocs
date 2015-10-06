package jsky.app.ot.editor.seq;

import edu.gemini.spModel.config2.ItemKey;
import jsky.app.ot.util.OtColor;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders cells in the sequence tab.
 */
final class DynamicSequenceCellRenderer extends SequenceCellRenderer {
    private static final Color VERY_LIGHT_GREY = new Color(247, 243, 239);

    private static final Set<ItemKey> FIXED_ITEMS = new HashSet<ItemKey>() {{
        Collections.addAll(this, DynamicSequenceTableModel.SORT_ORDER);
    }};

    private DynamicSequenceTableModel _model;

    DynamicSequenceCellRenderer(final DynamicSequenceTableModel model) {
        _model  = model;
    }

    protected Color lookupTextColor(final int row, final int column) {
        return _model.matchesNodeId(row) ?
                super.lookupTextColor(row, column) :
                Color.LIGHT_GRAY;
    }

    protected Color lookupColor(final int row, final int column) {
        final ItemKey key = _model.getItemKeyAt(column);
        Color color = VERY_LIGHT_GREY;
        if (!FIXED_ITEMS.contains(key)) color = lookupColor(key);
        if (_model.isError(row)) {
            color = OtColor.LIGHT_SALMON;
        }

        if (_model.isComplete(row)) {
            color = color.darker();
        }
        return color;
    }

}
