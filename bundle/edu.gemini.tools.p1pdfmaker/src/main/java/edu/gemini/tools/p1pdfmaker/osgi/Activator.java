package edu.gemini.tools.p1pdfmaker.osgi;

import edu.gemini.tools.p1pdfmaker.P1PdfMaker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        System.out.println("edu.gemini.tools.p1pdfmaker started.");
        // call the p1pdfmaker main method which will stop the OSGi framework after execution
        P1PdfMaker.main(context);
    }

    public void stop(BundleContext context) throws Exception {
        System.out.println("edu.gemini.tools.p1pdfmaker stopped.");
    }

}
