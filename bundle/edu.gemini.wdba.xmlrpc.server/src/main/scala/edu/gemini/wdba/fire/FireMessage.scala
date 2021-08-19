package edu.gemini.wdba.fire

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.Dataset
import edu.gemini.spModel.too.TooType
import monocle.Lens

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

object FireMessage extends FireMessageOptics {

  final case class Sequence(
    totalStepCount:     Int,           // SRS-ODBM-C006, SRS-ODBM-C013
    completedStepCount: Int,           // SRS-ODBM-C008, SRS-ODBM-C014
    duration:           FiniteDuration // SRS-ODBM-C007
  ) {

    def seconds: Long =
      duration.toSeconds

  }

  object Sequence extends SequenceOptics {

    val Empty: Sequence =
      Sequence(
        totalStepCount     = 0,
        completedStepCount = 0,
        duration           = 0.milliseconds
      )

  }

  sealed trait SequenceOptics { self: Sequence.type =>

    val totalStepCount: Lens[Sequence, Int] =
      Lens.apply[Sequence, Int](_.totalStepCount)(a => _.copy(totalStepCount = a))

    val completedStepCount: Lens[Sequence, Int] =
      Lens.apply[Sequence, Int](_.completedStepCount)(a => _.copy(completedStepCount = a))

    val duration: Lens[Sequence, FiniteDuration] =
      Lens.apply[Sequence, FiniteDuration](_.duration)(a => _.copy(duration = a))

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

sealed trait FireMessageOptics { self: FireMessage.type =>

  val uuid: Lens[FireMessage, UUID] =
    Lens.apply[FireMessage, UUID](_.uuid)(a => _.copy(uuid = a))

  val time: Lens[FireMessage, Instant] =
    Lens.apply[FireMessage, Instant](_.time)(a => _.copy(time = a))

  val observationId: Lens[FireMessage, Option[SPObservationID]] =
    Lens.apply[FireMessage, Option[SPObservationID]](_.observationId)(a => _.copy(observationId = a))

  val too: Lens[FireMessage, Option[TooType]] =
    Lens.apply[FireMessage, Option[TooType]](_.too)(a => _.copy(too = a))

  val visitStart: Lens[FireMessage, Option[Instant]] =
    Lens.apply[FireMessage, Option[Instant]](_.visitStart)(a => _.copy(visitStart = a))

  val datasets: Lens[FireMessage, List[Dataset]] =
    Lens.apply[FireMessage, List[Dataset]](_.datasets)(a => _.copy(datasets = a))

  val sequence: Lens[FireMessage, Sequence] =
    Lens.apply[FireMessage, Sequence](_.sequence)(a => _.copy(sequence = a))

  val totalStepCount: Lens[FireMessage, Int] =
    sequence ^|-> Sequence.totalStepCount

  val completedStepCount: Lens[FireMessage, Int] =
    sequence ^|-> Sequence.completedStepCount

  val duration: Lens[FireMessage, FiniteDuration] =
    sequence ^|-> Sequence.duration

}
