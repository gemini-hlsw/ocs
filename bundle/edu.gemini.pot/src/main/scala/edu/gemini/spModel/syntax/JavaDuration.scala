// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.syntax

import java.time.Duration

final class JavaDurationOps(self: Duration) {

  /**
   * Converts a duration to fractional seconds, which for some reason was the
   * "standard" we used for time in the early days and which is now firmly
   * entrenched in the sequence model.
   */
  def fractionalSeconds: Double =
    self.toMillis.toDouble / 1000.0

}

trait ToJavaDurationOps {
  implicit def ToJavaDurationOps(d: Duration): JavaDurationOps =
    new JavaDurationOps(d)
}

object duration extends ToJavaDurationOps
