//
// $Id: PioEntityResolver.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.shared.util.GeminiRuntimeException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.File;
import java.net.URL;

/**
 * Entity resolver used to lookup the SpXML2.dtd.
 */
class PioEntityResolver implements EntityResolver {

    static final PioEntityResolver INSTANCE = new PioEntityResolver();

    public static URL getResource(String fileName) {
        String path = "/resources/" +fileName;
        URL u = PioEntityResolver.class.getResource(path);
        if (u == null) {
            throw new GeminiRuntimeException("Could not locate resource: " + fileName);
        }
        return u;
    }

    private PioEntityResolver() {
    }

    public InputSource resolveEntity(String publicId, String systemId) {
        try {
            String fileName = systemId;
            if (fileName.startsWith("jar:")
                    || fileName.startsWith("file:")
                    || fileName.startsWith("http:")) {
                fileName = new File(new URL(fileName).getFile()).getName();
            }
            URL url = getResource(fileName);
            return new InputSource(url.openStream());
        } catch (Exception e) {
            throw new RuntimeException("Error resolving entity: " + systemId, e);
        }
    }
}
