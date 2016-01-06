package jsky.util.gui;

import java.io.File;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.filechooser.*;

/**
 * A convenience implementation of FileFilter that filters out
 * all files except for those type extensions that it knows about.
 *
 * Extensions are of the type ".foo", which is typically found on
 * Windows and Unix boxes, but not on Macinthosh. Case is ignored.
 *
 * Example - create a new filter that filters out all files
 * but gif and jpg image files:
 *
 *     JFileChooser chooser = new JFileChooser();
 *     ExampleFileFilter filter = new ExampleFileFilter(
 *                   new String{"gif", "jpg"}, "JPEG & GIF Images")
 *     chooser.addChoosableFileFilter(filter);
 *     chooser.showOpenDialog(this);
 */
public class ExampleFileFilter extends FileFilter {

    private Hashtable<String, FileFilter> filters = null;
    private String description = null;
    private String fullDescription = null;

    /**
     * Creates a file filter. If no filters are added, then all
     * files are accepted.
     *
     */
    public ExampleFileFilter() {
        this.filters = new Hashtable<>();
    }

    /**
     * Creates a file filter that accepts the given file type.
     * Example: new ExampleFileFilter("jpg", "JPEG Image Images");
     *
     * Note that the "." before the extension is not needed. If
     * provided, it will be ignored.
     */
    public ExampleFileFilter(final String extension, final String description) {
        this();
        if (extension != null) addExtension(extension);
        if (description != null) setDescription(description);
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new ExampleFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public ExampleFileFilter(String[] filters, String description) {
        this();
        for (final String filter : filters) {
            addExtension(filter);
        }
        if (description != null) setDescription(description);
    }

    /**
     * Return true if this file should be shown in the directory pane,
     * false if it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     */
    public boolean accept(final File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            final String extension = getExtension(f);
            if (extension != null && filters.get(getExtension(f)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the extension portion of the file's name .
     */
    public String getExtension(final File f) {
        if (f != null) {
            final String filename = f.getName();
            final int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   ExampleFileFilter filter = new ExampleFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public void addExtension(final String extension) {
        if (filters == null) {
            filters = new Hashtable<>(5);
        }
        filters.put(extension.toLowerCase(), this);
        fullDescription = null;
    }


    /**
     * Returns the human readable description of this filter. For
     * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
     *
     */
    public String getDescription() {
        if (fullDescription == null) {
            if (description == null) {
                fullDescription = "(";
                // build the description from the extension list
                Enumeration<String> extensions = filters.keys();
                if (extensions != null) {
                    fullDescription += "." + extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", " + extensions.nextElement();
                    }
                }
                fullDescription += ")";
            } else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For
     * example: filter.setDescription("Gif and JPG Images");
     */
    public void setDescription(final String description) {
        this.description = description;
        fullDescription = null;
    }
}
