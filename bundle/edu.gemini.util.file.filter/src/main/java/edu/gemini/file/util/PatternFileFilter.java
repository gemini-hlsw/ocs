//
// $Id: PatternFileFilter.java 244 2006-01-03 18:47:49Z shane $
//

package edu.gemini.file.util;

import java.io.Serializable;
import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;
import java.util.ArrayList;

/**
 * A FileFilter implementation based on include and exclude regular expression
 * patterns.  A file is accepted if it matches a least one include pattern, but
 * none of the exclude patterns.
 */
public final class PatternFileFilter implements FileFilter, Serializable {
    private Collection<Pattern> _includes;
    private Collection<Pattern> _excludes;

    /**
     * Constructs with the include and exclude patterns that will be evaluated
     * for each file encountered.
     *
     * @param includes list of patterns, one of which must be matched in order
     * to consider a file in the monitoered directory; if <code>null</code> all
     * files are considered
     *
     * @param excludes list of patterns, none of which may be matched or else
     * the file will be ignored in the  monitored directory; if
     * <code>null</code> no files are explicitly excluded
     */
    public PatternFileFilter(Collection<Pattern> includes, Collection<Pattern> excludes) {
        if (includes != null) {
            _includes = new ArrayList<Pattern>(includes);
        }
        if (excludes != null) {
            _excludes = new ArrayList<Pattern>(excludes);
        }
    }

    public boolean accept(File file) {
        String fname = file.getName();

        // Make sure it matches at least one include pattern, if specified.
        boolean include = false;
        if (_includes == null) {
            include = true;
        } else {
            for (Pattern p : _includes) {
                Matcher m = p.matcher(fname);
                if (m.matches()) {
                    include = true;
                    break;
                }
            }
        }

        if (!include) return false;

        // Make sure it doesn't match any exclude pattern, if specified.
        boolean exclude = false;
        if (_excludes != null) {
            for (Pattern p : _excludes) {
                Matcher m = p.matcher(fname);
                if (m.matches()) {
                    exclude = true;
                    break;
                }
            }
        }

        return !exclude;
    }

}
