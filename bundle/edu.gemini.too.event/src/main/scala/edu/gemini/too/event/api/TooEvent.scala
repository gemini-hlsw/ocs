package edu.gemini.too.event.api

import edu.gemini.spModel.obs.ObsSchedulingReport
import edu.gemini.spModel.too.TooType

case class TooEvent(report: ObsSchedulingReport, tooType: TooType, timestamp: TooTimestamp = TooTimestamp.now)