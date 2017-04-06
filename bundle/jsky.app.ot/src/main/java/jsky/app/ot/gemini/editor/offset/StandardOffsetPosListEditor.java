package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.target.offset.OffsetPos;

/**
 * An editor for the list of standard {@link edu.gemini.spModel.target.offset.OffsetPos
 * offset positions}.
 */
public final class StandardOffsetPosListEditor extends AbstractOffsetPosListEditor<OffsetPos> {

    public StandardOffsetPosListEditor() {
        StandardOffsetPosListPanel pan = new StandardOffsetPosListPanel();
        StandardOffsetPosTableModel tableModel = new StandardOffsetPosTableModel();
        StandardOffsetPosTableEditor tableEditor = new StandardOffsetPosTableEditor(pan.getOffsetPosTablePanel(), tableModel);
        DefaultOffsetPosEditor<OffsetPos> posEditor = new DefaultOffsetPosEditor<OffsetPos>(pan.getStandardOffsetPosEditorUI());

        OffsetPosListEditorConfig<OffsetPos> config;
        config = new OffsetPosListEditorConfig<OffsetPos>(pan, tableEditor, posEditor);
        init(config);
    }
}
