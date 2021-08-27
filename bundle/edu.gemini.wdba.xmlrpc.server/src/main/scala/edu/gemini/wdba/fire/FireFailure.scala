// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.pot.sp.SPObservationID

import java.io.IOException

sealed trait FireFailure {

  def exception: Option[Throwable] =
    None

  def message: String

}

object FireFailure {

  final case class ObsNotFound(obsId: SPObservationID) extends FireFailure {

    override def message: String =
      s"Observation ${obsId.stringValue} was referenced in an event but does not exist in the ODB."

  }

  def obsNotFound(obsId: SPObservationID): FireFailure =
    ObsNotFound(obsId)

  final case class FireException(t: Throwable) extends FireFailure {

    override def exception: Option[Throwable] =
      Some(t)

    override def message: String =
      s"FireException with contained message ${t.getMessage}"

  }

  def fireException(ex: Throwable): FireFailure =
    FireException(ex)

  final case class PostError(code: Int, message: String) extends FireFailure

  def postError(code: Int, message: String): FireFailure =
    PostError(code, message)

  final case class InvalidResponse(message: String) extends FireFailure

  def invalidResponse(message: String): FireFailure =
    InvalidResponse(message)

}
