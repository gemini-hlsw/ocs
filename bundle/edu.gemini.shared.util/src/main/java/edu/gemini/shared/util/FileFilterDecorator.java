//
// $Id: FileFilterDecorator.java 4399 2004-01-30 18:24:42Z shane $
//

package edu.gemini.shared.util;

import java.io.FileFilter;
import java.io.File;

/**
 * A FileFilter decorator class.  It allows the subclass to perform its own
 * filtering, and to form part of a chain of decorators so that multiple
 * criteria may be applied to file filtering. A subclass should override the
 * {@link #accept} method to either:
 * <ul>
 * <li>perform its own filtering and then call <code>super.accept(file)</code>
 * if necessary to allow any other filters in the chain to operate, or</li>
 * <li>call <code>super.accept(file)</code> first and then perform whatever
 * filtering is needed</li>
 * </ul>
 *
 * <p>It is expected that the subclass will only call
 * <code>super.accept(file)</code> in the first case if its own filtering
 * passes the file, and in the second case it will only perform its own
 * filtering if <code>super.accept(file)</code> returns <code>true</code>.
 */
public class FileFilterDecorator implements FileFilter {

    private FileFilter _decorated;

    /**
     * Constructs without a decorated filter.  The decorated filter may be
     * set later with {@link #setDecoratedFilter}.  If never set, then this
     * decorator will mark the end of the chain of decorators.
     */
    public FileFilterDecorator() {
    }

    /**
     * Constructs with the file filter to apply either before or after the
     * local filtering in the subclass is applied.
     */
    public FileFilterDecorator(FileFilter decorated) {
        _decorated = decorated;
    }

    public FileFilter getDecoratedFilter() {
        return _decorated;
    }

    public void setDecoratedFilter(FileFilter decorated) {
        _decorated = decorated;
    }

    /**
     * Returns <code>true</code> if the given <code>file</code> should be
     * accepted; <code>false</code> otherwise. If no decorator is set,
     * returns <code>true</code> to signify that the given <code>file</code>
     * should be accepted.  Otherwise, returns whatever the decorated
     * file filter returns.
     *
     * @param file the file to test
     */
    public boolean accept(File file) {
        return (_decorated == null) ? true : _decorated.accept(file);
    }
}
