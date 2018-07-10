package edu.gemini.qpt.ui.action;

import edu.gemini.qpt.shared.util.TimeUtils;

public class DragLimit {

    private static int limit = 10; // hours, by default

    public static synchronized String caption() {
        return "Drag Limit " + TimeUtils.msToHHMMSS(value());
    }

    public static synchronized void lower() {
        if (limit > 1)
            --limit;
    }
    
    public static synchronized void higher() {
        limit++;
    }
    
    public static synchronized long value() {
        return TimeUtils.MS_PER_HOUR * limit;
    }
    
}
