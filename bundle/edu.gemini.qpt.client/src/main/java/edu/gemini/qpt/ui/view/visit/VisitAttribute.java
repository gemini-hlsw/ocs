package edu.gemini.qpt.ui.view.visit;

public enum VisitAttribute {

    Group(" "), Start, Dur, BG, Observation, Steps, Inst, Config, WFS, Target
    
    ;

    private String stringValue;

    private VisitAttribute() {        
    }

    private VisitAttribute(String stringValue) {
        this.stringValue = stringValue;
    }
    
    @Override
    public String toString() {
        return stringValue != null ? stringValue : super.toString();
    }

    
}
