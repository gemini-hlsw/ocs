package edu.gemini.qpt.ui.html;

import java.io.InputStream;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Identical to the standard Velocity ClasspathResourceLoader, but uses the
 * thread's context Classloader instead of the class's Classloader. This in
 * effect gives the user the ability to pass in a Classloader by associating it
 * with the calling thread.
 * <p>
 * Bitches all. Velocity sucks.
 * @author rnorris
 */
public class ContextClasspathResourceLoader extends ClasspathResourceLoader {

    @Override
    public synchronized InputStream getResourceStream(String name)    throws ResourceNotFoundException {
        if (name == null || name.length() == 0) {
            throw new ResourceNotFoundException("No template name provided");
        }
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream(name);
            return is;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

}
