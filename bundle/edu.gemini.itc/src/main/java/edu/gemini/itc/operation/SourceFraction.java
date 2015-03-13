package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public interface SourceFraction {

    public double getSourceFraction();

    public double getNPix();

    public String getTextResult(FormatStringWriter device, boolean sfPrint);

    public String getTextResult(FormatStringWriter device);


}
