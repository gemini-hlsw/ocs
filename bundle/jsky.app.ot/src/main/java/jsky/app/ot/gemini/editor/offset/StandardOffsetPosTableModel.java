package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.target.offset.OffsetPos;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * An offset pos table model for standard offset positions.
 */
public final class StandardOffsetPosTableModel extends AbstractOffsetPosTableModel<OffsetPos> {
    private static final List<Column<OffsetPos>> FIXED_COLUMNS;

    static {
        List<Column<OffsetPos>> lst = new ArrayList<Column<OffsetPos>>(3);
        lst.add(new PColumn<OffsetPos>());
        lst.add(new QColumn<OffsetPos>());
        lst.add(new DefaultGuideColumn<OffsetPos>());
        FIXED_COLUMNS = Collections.unmodifiableList(lst);
    }

    protected StandardOffsetPosTableModel() {
        super(FIXED_COLUMNS);
    }
}
