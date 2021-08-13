// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.pot.sp.SPObservationID

sealed trait FireFailure {

  def exception: Throwable

}

object FireFailure {

  final case class ObsNotFound(obsId: SPObservationID) extends FireFailure {
    override def exception: Throwable =
      new RuntimeException(s"Observation ${obsId.stringValue} was referenced in an event but does not exist in the ODB.")
  }

  def obsNotFound(obsId: SPObservationID): FireFailure =
    ObsNotFound(obsId)

  final case class FireException(exception: Throwable) extends FireFailure

  def fireException(ex: Throwable): FireFailure =
    FireException(ex)

}
