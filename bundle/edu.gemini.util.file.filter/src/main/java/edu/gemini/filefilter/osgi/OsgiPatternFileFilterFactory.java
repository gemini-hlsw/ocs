//
// $Id: OsgiPatternFileFilterFactory.java 244 2006-01-03 18:47:49Z shane $
//

package edu.gemini.filefilter.osgi;

import edu.gemini.filefilter.PatternFileFilter;
import org.osgi.framework.BundleContext;

import java.util.regex.Pattern;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 * Constructs a PatternFileFilter from the
 * {@link #INCLUDE_FILE_FILTER include} and {@link #EXCLUDE_FILE_FILTER exclude}
 * properties.
 */
public final class OsgiPatternFileFilterFactory {
    public static final String INCLUDE_FILE_FILTER = "edu.gemini.filefilter.includes";
    public static final String EXCLUDE_FILE_FILTER = "edu.gemini.filefilter.excludes";

    public static PatternFileFilter create(BundleContext ctx) {
        Collection<Pattern> includes = _getPatterns(ctx, INCLUDE_FILE_FILTER);
        Collection<Pattern> excludes = _getPatterns(ctx, EXCLUDE_FILE_FILTER);
        if ((includes == null) && (excludes == null)) return null;
        return new PatternFileFilter(includes, excludes);
    }

    private static Collection<Pattern> _getPatterns(BundleContext ctx, String prefix) {
        Set<Pattern> pset = new HashSet<Pattern>();

        String pStr = ctx.getProperty(prefix);
        if (pStr != null) pset.add(Pattern.compile(pStr));

        pStr = ctx.getProperty(prefix + ".1");
        if (pStr != null) pset.add(Pattern.compile(pStr));

        if (pset.size() == 0) return null;

        int start = 2;
        while (true) {
            pStr = ctx.getProperty(prefix + "." + start);
            if (pStr == null) break;
            pset.add(Pattern.compile(pStr));
            ++start;
        }

        return pset;
    }
}
