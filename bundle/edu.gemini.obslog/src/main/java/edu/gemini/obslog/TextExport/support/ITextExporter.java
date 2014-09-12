package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.IObservingLogSegment;

/**
 * Gemini Observatory/AURA
 * $Id: ITextExporter.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
 */
public interface ITextExporter {
    
    StringBuilder export(StringBuilder sb);

    interface ITextExporterFactory {
        ITextExporter create(IObservingLogSegment segment);
    }
}
