package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.OlLogInformation;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.core.SPProgramID;

//
// Gemini Observatory/AURA
// $Id: TextLogInfoExporter.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

/**
 * This exporter handles exporting the nightly plan info
 */
public class TextLogInfoExporter extends TextExportBase implements ITextExporter {
    //private static final Logger LOG = LogUtil.getLogger(TextLogInfoExporter.class);

    private static final int DIVIDER_WIDTH = 140;

    private OlLogInformation _logInfo;
    private OlLogOptions _options;
    private SPProgramID _planID;

    public TextLogInfoExporter(OlLogInformation logInfo, OlLogOptions options, SPProgramID planID) throws NullPointerException {
        if (logInfo == null) throw new NullPointerException("LogInfo argument is null for export");
        _logInfo = logInfo;
        _options = options;
        _planID = planID;
    }

    public StringBuilder export(StringBuilder sb) {
        String siteTitle = _options.isSouth() ? "Gemini-South" : "Gemini-North";

        String caption = siteTitle + ':' + " Electronic Observing Log for: " + _planID.stringValue();

        _printCaption(sb, caption);

        _printDivider(sb, DIVIDER, DIVIDER_WIDTH);

        _printCaption(sb, "Day Observers      : " + _logInfo.getDayobserver());
        _printCaption(sb, "");

        _printCaption(sb, "Night Observers    : " + _logInfo.getNightObservers());
        _printCaption(sb, "Night SSA          : " + _logInfo.getSsas());
        _printCaption(sb, "Dataproc Observer  : " + _logInfo.getDataproc());
        _printCaption(sb, "Observer Comment   : " + _logInfo.getNightComment());
        _printCaption(sb, "");

        _printCaption(sb, "File Prefix        : " + _logInfo.getFilePrefix());
        _printCaption(sb, "");

        _printCaption(sb, "CC Software Version: " + _logInfo.getCCVersion());
        _printCaption(sb, "DC Software Version: " + _logInfo.getDCVersion());
        _printCaption(sb, "Software Comment   : " + _logInfo.getSoftwareComment());
        _printCaption(sb, "");

        return sb;
    }
}
