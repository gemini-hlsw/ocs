package edu.gemini.wdba.fire

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.Dataset
import edu.gemini.spModel.too.TooType

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration._

final case class FireMessage(
  uuid:             UUID,                    // SRS-ODBM-C012
  time:             Instant,                 // SRS-ODBM-C011
  nature:           String,                  // SRS-ODBM-C010
  observationId:    Option[SPObservationID], // SRS-ODBM-C003
  too:              Option[TooType],         // SRS-ODBM-C009
  visitStart:       Option[Instant],         // From example message
  datasets:         List[Dataset],
  sequence:         FireMessage.Sequence
) {

  // SRS-ODBM-C002
  def programId: Option[SPProgramID] =
    observationId.flatMap(oid => Option(oid.getProgramID))

  // SRS-ODBM-C004
  def fileNames: List[String] =
    datasets.map(_.getDhsFilename)

}

object FireMessage {

  final case class Sequence(
    totalStepCount:     Int,           // SRS-ODBM-C006, SRS-ODBM-C013
    completedStepCount: Int,           // SRS-ODBM-C008, SRS-ODBM-C014
    duration:           FiniteDuration // SRS-ODBM-C007
  ) {

    def seconds: Long =
      duration.toSeconds

  }

  object Sequence {

    val Empty: Sequence =
      Sequence(
        totalStepCount     = 0,
        completedStepCount = 0,
        duration           = 0.milliseconds
      )

  }

  def empty(
    uuid:   UUID,
    time:   Instant,
    nature: String
  ): FireMessage =
    FireMessage(
      uuid          = uuid,
      time          = time,
      nature        = nature,
      observationId = Option.empty,
      too           = Option.empty,
      visitStart    = Option.empty,
      datasets      = List.empty,
      sequence      = Sequence.Empty
    )

  def emptyNow(nature: String): FireAction[FireMessage] =
    for {
      u <- FireAction(UUID.randomUUID)
      t <- FireAction(Instant.now)
    } yield FireMessage.empty(u, t, nature)

  def emptyAt(when: Instant, nature: String): FireAction[FireMessage] =
    FireAction(UUID.randomUUID).map(u => FireMessage.empty(u, when, nature))

}
