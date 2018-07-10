package edu.gemini.qpt.ui.action.nudge;

import edu.gemini.qpt.shared.util.TimeUtils;

class Resolution {

    private static long[] intervals = {        
        1000 * 1, 
        1000 * 2, 
        1000 * 5, 
        1000 * 10, 
        1000 * 15, 
        1000 * 30, 
        1000 * 60, 
        1000 * 120, 
        1000 * 300, 
        1000 * 600, 
        1000 * 900, 
        1000 * 1800, 
        1000 * 3600, 
    };
    
    private static int index = 8;

    static synchronized String caption() {
        return "Nudge Size " +  TimeUtils.msToHHMMSS(intervals[index]);
    }

    static synchronized void lower() {
        if (index < intervals.length - 1) index++;
    }
    
    static synchronized void higher() {
        if (index > 0) index--;
    }
    
    static synchronized long value() {
        return intervals[index];
    }
    
}
