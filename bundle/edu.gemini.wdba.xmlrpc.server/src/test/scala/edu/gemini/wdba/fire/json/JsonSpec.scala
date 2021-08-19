// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire
package json

import argonaut._
import Argonaut._
import org.specs2.mutable.Specification

import java.time.Instant
import java.util.UUID

object JsonSpec extends Specification {

  "Json encoders" should {

    "handle empty values as null / empty array" in {
      val fixedUuid = UUID.fromString("AA3DD6D6-3322-418A-A665-A9D8810BFF3D")
      val fixedTime = Instant.ofEpochMilli( 1628863911712L)
      val empty     = FireMessage.empty(fixedUuid, fixedTime,"empty")

      val actual   = empty.asJson.spaces2
      val expected = Parse.parse(
        """
           {
             "nature":         "empty",
             "id":             "aa3dd6d6-3322-418a-a665-a9d8810bff3d",
             "eventTime":      "2021-08-13T14:11:51.712Z",
             "programId":      null,
             "observationId":  null,
             "too":            null,
             "fileNames":      [],
             "visitStartTime": null,
             "sequence": {
               "totalDatasetCount":     0,
               "completedDatasetCount": 0,
               "totalStepCount":        0,
               "completedStepCount":    0,
               "durationMilliseconds":  0
             }
           }
        """).fold(identity, _.spaces2)

      actual should_== expected
    }

  }

}
