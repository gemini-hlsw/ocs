package jsky.app.ot.gemini.editor.offset;


/**
 * Offset position list panel for standard
 * {@link edu.gemini.spModel.target.offset.OffsetPos offset positions}.
 */
public final class StandardOffsetPosListPanel extends AbstractOffsetPosListPanel {
    public StandardOffsetPosListPanel() {
        super(new StandardOffsetPosPanel());
    }

    public StandardOffsetPosPanel getStandardOffsetPosEditorUI() {
        return (StandardOffsetPosPanel) getOffsetPosEditorUI();
    }
}
