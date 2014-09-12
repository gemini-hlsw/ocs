//
// $Id: PioPath.java 4937 2004-08-14 21:35:20Z shane $
//
package edu.gemini.spModel.pio;

/**
 * The PioPath can be used to refer to nodes in a {@link Document} or,
 * in general, {@link PioNode} tree.  It is modeled after the
 * <code>java.io.File</code> class.
 *
 * <p>The path is composed of a series of names separated by the '/' character.
 * Because names are optional for {@link Container}s, and because names are
 * not guaranteed to be unique, a path may identify more than one PioNode.
 *
 * <p>The question mark character, '?', may be used to refer to anonymous
 * {@link Container}s.
 */
public class PioPath implements Comparable {

    /**
     * Character used to separate the {@link PioNamedNode} names.
     */
    public static final char separatorChar = '/';

    /**
     * String containing the character used to separate {@link PioNamedNode}s.
     */
    public static final String separator   = "/";

    /**
     * String that represents an anonymous {@link Container}.
     */
    public static final String anonymousContainer = "?";

    private String _path;
    private int _prefixLength;

    /**
     * Constructs with the full path to the node.  If the path contains
     * multiple contiguous separator or ends with a trailing separator, it
     * will be normalized.  For example, <tt>/a//b/c/</tt> is converted to
     * <tt>/a/b/c</tt>.
     *
     * @param path string representation of the path to the node
     *
     * @throws NullPointerException if <code>path</code> is <code>null</code>
     */
    public PioPath(String path) {
        if (path == null) throw new NullPointerException();
        _path = _normalize(path);
        _prefixLength = _path.lastIndexOf(separatorChar);
    }

    /**
     * Constructs with the parent path and the child path, which are combined to
     * create a single path to the node in question.  Typically, the
     * <code>child</code> path will be a single name to be appended to the
     * <code>parent</code>, but it may be a longer path of its own right
     * containing {@link #separatorChar}s.
     *
     * @param parent path that will be extended by the <code>child</code> path;
     * may be <code>null</code> in which case the behavior will be the same as
     * calling {@link #PioPath(String)} with the <code>child</code> path
     *
     * @param child path that will be appened to the parent; if this path is
     * absolute then the leading {@link #separatorChar} is stripped before it
     * is added to the parent; may <em>not</em> be <code>null</code>
     *
     * @throws NullPointerException if <code>child</code> is <code>null</code>
     */
    public PioPath(String parent, String child) {
        if (child == null) throw new NullPointerException();

        String path;
        if (parent == null) {
            path = child;
        } else {
            if (!(parent.endsWith(separator) || (child.startsWith(separator)))) {
                path = parent + separator + child;
            } else {
                path = parent + child;
            }
        }
        _path = _normalize(path);
        _prefixLength = _path.lastIndexOf(separatorChar);
    }

    /**
     * Constructs a PioPath by appending the given <code>child</code> path to
     * the <code>parent</code>. Typically, the <code>child</code> path will be
     * a single name to be appended to the <code>parent</code>, but it may be
     * a longer path of its own right containing {@link #separatorChar}s.
     *
     * @param parent path that will be extended by the <code>child</code> path;
     * may not be <code>null</code>
     *
     * @param child path that will be appened to the parent; if this path is
     * absolute then the leading {@link #separatorChar} is stripped before it
     * is added to the parent; may <em>not</em> be <code>null</code>
     *
     * @throws NullPointerException if either <code>parent</code> or
     * <code>child</code> is <code>null</code>
     */
    public PioPath(PioPath parent, String child) {
        this(parent.toString(), child);
    }


    /**
     * Removes multiple continguos path separators and any trailing separators.
     *
     * @param pathstr path to normalize
     *
     * @return normalized path with single path separators and no trailing
     * separator
     */
    private static String _normalize(String pathstr) {
        if (pathstr == null) return "";
        pathstr = pathstr.trim();

        StringBuffer buf = new StringBuffer();

        // We will build up the normalized string in a buffer.  First, if this
        // was an absolute path, prepend the separator character.
        if (pathstr.startsWith(separator)) {
            buf.append(separator);
        }

        // Split the string over the separator character.  Loop over the
        // parts ignoring empty parts which occur if there are multiple
        // separator characters.
        String[] parts = pathstr.split(separator);
        for (int i=0; i<parts.length; ++i) {
            String part = parts[i].trim();
            if ("".equals(part)) continue;
            buf.append(part).append(separator);
        }

        // If the only character in the buffer is "/", then that was the
        // whole normalized path.  Otherwise, we'll be left with a trailing
        // separator character to remove.
        pathstr = buf.toString();
        if (separator.equals(pathstr)) return pathstr;
        return pathstr.substring(0, pathstr.length() - 1);

    }

    /**
     * Splits the path into its constituent parts.
     * @return names that make up the path
     */
    public String[] split() {
        if (separator.equals(_path)) return new String[0];

        String path = _path;
        if (_path.startsWith(separator)) {
            path = _path.substring(1);
        }
        return path.split("/");

    }

    /**
     * Gets the path to the parent (if there is one) of the node referenced by
     * this path as a String.
     *
     * @return path string of the parent node named by this path, or
     * <code>null</code> if this node does not have a parent
     */
    public String getParent() {
        if (_prefixLength <= 0) return null;
        return _path.substring(0, _prefixLength);
    }

    /**
     * Gets the PioPath to the parent (if there is one) of the node referenced
     * by this path.
     *
     * @return PioPath of the parent node named by this path, or
     * <code>null</code> if this node does not have a parent
     */
    public PioPath getParentPath() {
        String parentStr = getParent();
        if (parentStr == null) return null;
        return new PioPath(getParent());
    }

    /**
     * Gets the name of the node referenced by this path.  This is just the
     * last name in the path's sequence.  If the path's sequence is empty
     * (as for a path to a {@link Document} node), then the empty string is
     * returned.
     *
     * @return name of the node denoted by this path, or the empty string if
     * this path's name sequence is empty
     */
    public String getName() {
        if (_prefixLength < 0) return _path;

        // _path is normalized and so cannot end with separator char
        return _path.substring(_prefixLength+1);
    }

    /**
     * Returns <code>true</code> if this path starts at a root node (in other
     * words, begins with a {@link #separatorChar}.
     *
     * @return <code>true</code> if this an absolute path from a root node,
     * <code>false</code> otherwise
     */
    public boolean isAbsolute() {
        return _path.startsWith(separator);
    }


    /**
     * Returns the PioPath as a String, where the names that make up the
     * path are separated by {@link #separatorChar}s.
     */
    public String toString() {
        return _path;
    }

    public int compareTo(Object o) {
        PioPath that = (PioPath) o;
        return _path.compareTo(that._path);
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != PioPath.class) return false;

        PioPath that = (PioPath) other;
        return _path.equals(that._path);
    }

    public int hashCode() {
        return _path.hashCode();
    }
}
