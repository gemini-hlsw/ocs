package edu.gemini.qpt.ui.find;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.ui.gface.GFilter;
import edu.gemini.ui.gface.GViewer;

public class FindFilter implements GFilter<Schedule, FindElement> {

    private GViewer<Schedule, FindElement> viewer;
    private String pattern;
    
    public boolean accept(FindElement element) {
        if (pattern == null || pattern.length() < 1) return false;
        return element.getTarget().toString().contains(pattern);
    }

    public void modelChanged(GViewer<Schedule, FindElement> viewer, Schedule oldModel, Schedule newModel) {
        this.viewer = viewer;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern.toUpperCase();
        if (viewer != null)
            viewer.refresh();
    }
    
}
