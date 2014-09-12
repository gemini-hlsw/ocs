package jsky.app.ot.gemini.gpi;

import edu.gemini.spModel.gemini.gpi.GpiOffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosTableEditor;
import jsky.app.ot.gemini.editor.offset.OffsetPosGrid;
import jsky.app.ot.gemini.editor.offset.OffsetPosRandom;
import jsky.app.ot.gemini.editor.offset.OffsetPosTablePanel;

import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.Window;
import java.util.List;

/**
 * Table editor for GPI offset positions.
 */
public class GpiOffsetPosTableEditor extends AbstractOffsetPosTableEditor<GpiOffsetPos> {
    protected GpiOffsetPosTableEditor(OffsetPosTablePanel pan, GpiOffsetPosTableModel tableModel) {
        super(pan, tableModel);
    }

    @Override
    protected void addPosition() {
        OffsetPosList<GpiOffsetPos> opl = getPositionList();
        List<GpiOffsetPos> selList = getAllSelectedPos();

        GpiOffsetPos pos;
        if (selList.size() == 0) {
            pos = opl.addPosition(0);
        } else {
            GpiOffsetPos lastSelPos =  selList.get(selList.size()-1);
            int i = opl.getPositionIndex(lastSelPos);
            pos = opl.addPosition(i + 1);
        }
        select(pos);
    }

    @Override
    protected void showOffsetGridDialog() {
        Window w = SwingUtilities.windowForComponent(getPan());
        OffsetPosGrid.showDialog((Frame) w, getPositionList(), 0.5);
    }

    @Override
    protected void showRandomDialog() {
        Window w = SwingUtilities.windowForComponent(getPan());
        OffsetPosRandom.showDialog((Frame) w, getPositionList(), 1.0);
    }

}
