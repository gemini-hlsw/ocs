package edu.gemini.qpt.ui.view.problem;

enum ProblemAttribute { 
    
    Severity(" "), Description, Resource 
    
    ;

    private String stringValue;

    private ProblemAttribute() {        
    }

    private ProblemAttribute(String stringValue) {
        this.stringValue = stringValue;
    }
    
    @Override
    public String toString() {
        return stringValue != null ? stringValue : super.toString();
    }

}
