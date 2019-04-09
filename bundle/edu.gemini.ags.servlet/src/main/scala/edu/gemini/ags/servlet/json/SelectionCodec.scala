package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.ags.api.AgsStrategy.{ Assignment, Selection }
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.guide.{ GuideProbe, GuideProbeMap }


trait SelectionCodec {

  import edu.gemini.json.keyed._
  import siderealtarget._

  implicit val AngleCodec: CodecJson[Angle] =
    CodecJson.derived[Double].xmap(Angle.fromDegrees)(_.toDegrees)

  implicit val GuideProbeCodec: CodecJson[GuideProbe] =
    keyedCodec(_.getKey, (k: String) => Option(GuideProbeMap.instance.get(k)))

  implicit val AssignmentCodec: CodecJson[Assignment] =
    casecodec2(Assignment.apply, Assignment.unapply)(
      "probe",
      "star"
    )

  implicit val SelectionCodec: CodecJson[Selection] =
    casecodec2(Selection.apply, Selection.unapply)(
      "posAngle",
      "assignments"
    )

}

object selection extends SelectionCodec
