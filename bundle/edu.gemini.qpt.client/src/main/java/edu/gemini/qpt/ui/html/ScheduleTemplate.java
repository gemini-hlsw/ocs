package edu.gemini.qpt.ui.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Logger;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.RuntimeConstants;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.ProgressModel;

public class ScheduleTemplate {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(ScheduleTemplate.class.getName());

    private static final VelocityEngine ve = new VelocityEngine();
    static {
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
        ve.setProperty("class.resource.loader.class", ContextClasspathResourceLoader.class.getName());                
        ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, DelegatingLogSystem.class.getName());
        try {
            ve.init();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    public static void mergeTemplateToFile(Schedule sched, ProgressModel pi, File file, String prefix, boolean showQcMarkers, boolean utc) throws Throwable {
        OutputStream os = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(os);
        ScheduleDocument doc = new ScheduleDocument(sched, pi, file.getParentFile(), prefix, showQcMarkers, utc);
        VelocityContext vc = new VelocityContext();
        vc.put("doc", doc);        
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ScheduleTemplate.class.getClassLoader());

//            // Very annoying. We need the full path to the Template.fo file. We can't
//            // rely on Class.getPackage() or getCanonicalName() to work, so we need to 
//            // up the class name.
            String cname = ScheduleTemplate.class.getName().replace('.', '/');
            int pos = cname.lastIndexOf("/");
            String rname = "/" + cname.substring(0, pos + 1) + "template.vm";

            ve.mergeTemplate(rname, vc, writer);
//            ve.mergeTemplate("template.vm", vc, writer);

        } catch (MethodInvocationException mie) {
            throw mie.getWrappedThrowable();
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
        writer.close();
        os.close();
    }
    
    
    
}
