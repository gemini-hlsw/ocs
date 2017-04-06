package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.target.offset.OffsetPos;

/**
 * The offset position table editor for standard
 * {@link edu.gemini.spModel.target.offset.OffsetPos offset positions}.
 */
public final class StandardOffsetPosTableEditor extends AbstractOffsetPosTableEditor<OffsetPos> {
    protected StandardOffsetPosTableEditor(OffsetPosTablePanel pan, StandardOffsetPosTableModel tableModel) {
        super(pan, tableModel);
    }
}
