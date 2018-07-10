package edu.gemini.qpt.ui.util;

public enum ElevationPreference {

    ELEVATION,
    AIRMASS,
    
    ;
    
    public static EnumBox<ElevationPreference> BOX = new EnumBox<ElevationPreference>(ELEVATION);
    
}
