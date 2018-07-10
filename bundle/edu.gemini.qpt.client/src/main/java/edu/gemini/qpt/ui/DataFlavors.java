package edu.gemini.qpt.ui;

import java.awt.datatransfer.DataFlavor;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.shared.sp.Obs;

public interface DataFlavors {

    DataFlavor OBS = new DataFlavor(Obs.class, "Observation");
    DataFlavor ALLOC = new DataFlavor(Alloc.class, "Visit");
    DataFlavor OFFSET = new DataFlavor(Long.class, "Offset");
    
}
