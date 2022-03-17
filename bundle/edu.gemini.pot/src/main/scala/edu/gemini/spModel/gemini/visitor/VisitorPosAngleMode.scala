// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.visitor

import scalaz.Equal

sealed trait VisitorPosAngleMode extends Product with Serializable

object VisitorPosAngleMode {

  /**
   * Indicates that the position angle cannot be set for this visitor instrument.
   */
  case object Fixed0  extends VisitorPosAngleMode

  /**
   * Indicates that the position angle is settable for this visitor instrument.
   */
  case object Mutable extends VisitorPosAngleMode

  implicit val EqualVisitorPosAngleMode: Equal[VisitorPosAngleMode] =
    Equal.equalA

}