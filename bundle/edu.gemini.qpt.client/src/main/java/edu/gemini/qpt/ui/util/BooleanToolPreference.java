package edu.gemini.qpt.ui.util;



public enum BooleanToolPreference implements PreferenceManager.Key<Boolean> {

    TOOL_MAINTAIN_SPACING(true),
    TOOL_SNAP(true);
    
    ;
    
    final Boolean defaultValue;
    
    BooleanToolPreference(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
        
    public Boolean getDefaultValue() {
        return defaultValue;
    }
    
    public Boolean get() {
        return PreferenceManager.get(this);
    }

}
