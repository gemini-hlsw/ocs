//
// $Id: Configs.java 6221 2005-05-29 23:51:37Z shane $
//

package edu.gemini.spModel.config2;

/**
 * Utility methods for working with {@link Config}s.
 */
public final class Configs {
    private Configs() {
    }

    /**
     * Returns an unmodifiable representation of the given {@link Config}.  All
     * methods of {@link Config} which would otherwise modify the {@link Config}
     * are implementated to throw UnsupportedOperationException.
     */
    public static Config unmodifiableConfig(Config c) {
        if (c instanceof ImmutableConfig) return c;
        return new ImmutableConfig(c);
    }
}
