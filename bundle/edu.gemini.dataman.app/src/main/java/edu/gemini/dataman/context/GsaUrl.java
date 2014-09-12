//
// $Id: GsaUrl.java 129 2005-09-14 15:40:53Z shane $
//

package edu.gemini.dataman.context;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Wraps the template URL used to determine dataset acceptance in the GSA.
 * The template URL containing the string %FILE% where the dataset basename
 * should appear in the URL.
 */
public final class GsaUrl {
    private String _urlTemplate;

    /**
     * Constructs with a template URL used to determine dataset acceptance in
     * the GSA.
     * @param template template URL containing the string %FILE% where the
     * dataset basename should appear in the URL
     */
    public GsaUrl(String template) {
        _urlTemplate = template;
    }

    /**
     * Creates a java.net.URL by filling in the given filename into the
     * template URL.
     *
     * @param filename DHS id or filename; strips off the ".fits" suffix if it
     * exists before applying the id to the template
     *
     * @return valid URL object that may be used to query the GSA
     */
    public URL toURL(String filename) {
        if (!filename.endsWith(".fits")) filename = filename + ".fits";
        String urlStr = _urlTemplate.replace("%FILE%", filename);

        try {
            return new URL(urlStr);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(filename);
        }
    }
}
