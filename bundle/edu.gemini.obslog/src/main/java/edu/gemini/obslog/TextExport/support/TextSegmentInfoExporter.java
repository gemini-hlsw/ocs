package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.core.SPProgramID;

//
// Gemini Observatory/AURA
// $Id: TextSegmentInfoExporter.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class TextSegmentInfoExporter extends TextExportBase implements ITextExporter {
    //private static final Logger LOG = LogUtil.getLogger(TextLogInfoExporter.class);

    private static final int DIVIDER_WIDTH = 140;

    private OlLogOptions _options;
    private SPProgramID _planID;
    private String _title;

    public TextSegmentInfoExporter(OlLogOptions options, SPProgramID planID, String title) throws NullPointerException {
        _title = title;
        _options = options;
        _planID = planID;
    }

    public StringBuilder export(StringBuilder sb) {
        String siteTitle = _options.isSouth() ? "Gemini-South" : "Gemini-North";

        String caption = siteTitle + ": " + _title + " for: " + _planID.stringValue();

        _printCaption(sb, caption);

        _printDivider(sb, DIVIDER, DIVIDER_WIDTH);

        return sb;
    }
}
