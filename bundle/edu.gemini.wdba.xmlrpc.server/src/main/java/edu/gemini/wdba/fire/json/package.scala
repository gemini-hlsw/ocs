// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.pot.sp.SPObservationID
import argonaut._
import Argonaut._
import edu.gemini.spModel.too.TooType

import java.time.format.DateTimeFormatter.ISO_INSTANT


package object json {

  implicit def EncodeObservationId: EncodeJson[SPObservationID] =
    EncodeJson { oid => Option(oid.stringValue).asJson }

  implicit def EncodeProgramId: EncodeJson[SPProgramID] =
    EncodeJson { pid => Option(pid.stringValue).asJson }

  implicit def EncodeTooType: EncodeJson[TooType] =
    EncodeJson { _.getDisplayValue.asJson }

  implicit def EncodeFireMessageSequence: EncodeJson[FireMessage.Sequence] =
    EncodeJson { s =>
      ("durationMilliseconds"  := s.duration.toMillis)  ->:
      ("completedStepCount"    := s.completedStepCount) ->:
      ("totalStepCount"        := s.totalStepCount)     ->:
      ("completedDatasetCount" := s.completedStepCount) ->:
      ("totalDatasetCount"     := s.totalStepCount)     ->:  // for now # steps == # datasets
      jEmptyObject
    }

  implicit def EncodeFireMessage: EncodeJson[FireMessage] =
    EncodeJson { fm =>
      ("sequence"       := fm.sequence)                           ->:
      ("visitStartTime" := fm.visitStart.map(ISO_INSTANT.format)) ->:
      ("fileNames"      := fm.fileNames)                          ->:
      ("too"            := fm.too)                                ->:
      ("observationId"  := fm.observationId)                      ->:
      ("programId"      := fm.programId)                          ->:
      ("eventTime"      := ISO_INSTANT.format(fm.time))           ->:
      ("id"             := fm.uuid.toString)                      ->:
      ("nature"         := fm.nature)                             ->:
      jEmptyObject
    }


}
