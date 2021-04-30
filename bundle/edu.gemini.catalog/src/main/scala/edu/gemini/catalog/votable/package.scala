// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.catalog

/**
 *
 */
package object votable {

  private[votable] implicit class StringOps(s: String) {

    /**
     * A predicate that determines whether a String is both non-empty and not
     * "NaN".
     */
    def nonEmptyNonNan: Boolean =
      s.nonEmpty && !"NaN".equals(s)

  }

}
