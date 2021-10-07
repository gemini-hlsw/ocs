// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.p2checker.api;

import java.util.Arrays;

public final class ConfigMatcher {

    @SuppressWarnings("unused")
    public static IConfigMatcher matchAll(IConfigMatcher... matchers) {
        return (config, step, elems) ->
            Arrays.stream(matchers)
                  .reduce(true, (b, m) -> b && m.matches(config, step, elems), (a, b) -> a && b);

    }

    @SuppressWarnings("unused")
    public static IConfigMatcher matchAny(IConfigMatcher... matchers) {
        return (config, step, elems) ->
            Arrays.stream(matchers)
                  .reduce(false, (b, m) -> b || m.matches(config, step, elems), (a, b) -> a || b);

    }

}
