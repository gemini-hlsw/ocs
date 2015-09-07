package jsky.app.ot.gemini.gpi;

import edu.gemini.spModel.gemini.gpi.GpiOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosListEditor;
import jsky.app.ot.gemini.editor.offset.OffsetPosListEditorConfig;

/**
 * This is the editor for Offset Iterator component.  It allows a list of
 * offset positions to be entered and ordered.
 */
public final class GpiOffsetPosListEditor extends AbstractOffsetPosListEditor<GpiOffsetPos> {

    /**
     * The constructor initializes the user interface.
     */
    public GpiOffsetPosListEditor() {
        GpiOffsetPosListPanel pan = new GpiOffsetPosListPanel();
        GpiOffsetPosTableModel tableModel = new GpiOffsetPosTableModel();
        GpiOffsetPosTableEditor tableEditor = new GpiOffsetPosTableEditor(pan.getOffsetPosTablePanel(), tableModel);
        GpiOffsetPosEditor posEditor = new GpiOffsetPosEditor(pan.getGpiOffsetPosEditorUI());

        OffsetPosListEditorConfig<GpiOffsetPos> config;
        config = new OffsetPosListEditorConfig<GpiOffsetPos>(pan, tableEditor, posEditor);
        init(config);
    }

}
