package edu.gemini.obslog.TextExport.support;

//
// Gemini Observatory/AURA
// $Id: TextExportBase.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class TextExportBase {

    protected static final String COLUMN_SPACE = "  ";
    protected static final String NEWLINE = "\n";
    protected static final char DIVIDER = '-';

    protected static final String MISSING_VALUE = " - ";

    void _printDivider(StringBuilder sb, char c, int width) {
        for (int i = 0; i < width; i++) {
            sb.append(c);
        }
        sb.append(AbstractTextSegmentExporter.NEWLINE);
    }

    void _printSpaces(StringBuilder sb, int width) {
        for (int i = 0; i < width; i++) sb.append(' ');
    }

    protected void _printCaption(StringBuilder sb, String caption) {
        sb.append(caption);
        sb.append(AbstractTextSegmentExporter.NEWLINE);
    }

    protected void _printJustifiedText(StringBuilder sb, int width, String text) {
        int textLength = text.length();
        if (textLength <= width) {
            sb.append(text);
            _printSpaces(sb, width - textLength);
        } else {
            sb.append(text.substring(0, width));
        }
        sb.append(AbstractTextSegmentExporter.COLUMN_SPACE);
    }

}
