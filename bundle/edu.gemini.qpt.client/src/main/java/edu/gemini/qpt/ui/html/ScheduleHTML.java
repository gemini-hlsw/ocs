package edu.gemini.qpt.ui.html;

import java.io.File;
import java.util.logging.Logger;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.CancelledException;
import edu.gemini.qpt.ui.util.ProgressModel;

public class ScheduleHTML {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(ScheduleHTML.class.getName());

    /**
     * Given a root directory, creates and returns the schedule html files. This method will
     * fail with an IllegalStateException if the directory is not empty.
     * 
     * ProgressInfo should be initialized with a max of at least variants.size() + 1.
     * @param schedule
     * @param pi
     * @return
     * @throws Exception
     */
    public static File writeHTML(File root, Schedule schedule, ProgressModel pi, String prefix, boolean showQcMarkers, boolean utc) throws CancelledException {

        assert schedule != null;
        assert pi != null;
            
//        if (root.list().length > 0)
//            throw new IllegalStateException("Root directory is not empty.");
        
        try {
        
            if (pi.isCancelled()) throw new CancelledException();
            
            // Merge into html file
            pi.work();
            pi.setMessage("Merging template...");                    
            File html = new File(root, prefix + ".html");
            ScheduleTemplate.mergeTemplateToFile(schedule, pi, html, prefix, showQcMarkers, utc);
            if (pi.isCancelled()) throw new CancelledException();
    
            // Clean up
            return html;
            
        } catch (CancelledException ce) {
            throw ce;
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
}
