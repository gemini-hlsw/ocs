package edu.gemini.qpt.ui.view.lchWindow;

import java.util.Date;

/**
 * Represents a clearance or shutter window with the name of the associated target.
 */
public class LchWindow {
    private Date start;
    private Date end;
    private String targetType;
    private String targetName;

    public LchWindow(Date start, Date end, String targetType, String targetName) {
        this.start = start;
        this.end = end;
        this.targetType = targetType;
        this.targetName = targetName;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }
}
