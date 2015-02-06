package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public interface Calculatable {
    public void calculate();

    public String getTextResult(FormatStringWriter device);
}
