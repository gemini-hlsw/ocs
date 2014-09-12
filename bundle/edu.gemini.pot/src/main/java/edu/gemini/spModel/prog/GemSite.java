package edu.gemini.spModel.prog;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Site;

/**
 * Gemini site.
 * @deprecated just use edu.gemini.spModel.core.Site
 */
@Deprecated
public enum GemSite {
    north() {
        public String getIdPrefix()   { return "GN"; }
        public Site getSiteDesc() { return Site.GN; }
    },
    south() {
        public String getIdPrefix()   { return "GS"; }
        public Site getSiteDesc() { return Site.GS; }
    },
    ;

    public abstract String getIdPrefix();
    public abstract Site getSiteDesc();

    /**
     * Parses the given string into a GemSite, if possible.  Treats "north",
     * "n", and "gn" as {@link #north}, while "south", "s", and "gs" are
     * assumed to be {@link #south}.  Case is ignored.
     *
     * @param s string indicating the Gemini site
     *
     * @return {@link Some}<GemSite> if the string parses into a GemSite,
     * {@link None} otherwise
     */
    public static Option<GemSite> parse(String s) {
        try {
            return new Some<GemSite>(GemSite.valueOf(s));
        } catch (Exception ex) {
            // ignore
        }

        if (s == null) return None.instance();
        s = s.toUpperCase();

        if ("GN".equals(s) || "N".equals(s) || "NORTH".equals(s)) {
            return new Some<GemSite>(north);
        }
        if ("GS".equals(s) || "S".equals(s) || "SOUTH".equals(s)) {
            return new Some<GemSite>(south);
        }

        return None.instance();
    }
}
