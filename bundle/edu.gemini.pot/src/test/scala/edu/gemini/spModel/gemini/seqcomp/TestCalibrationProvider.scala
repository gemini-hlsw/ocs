package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.gemini.calunit.smartgcal.{CalibrationKey, CalibrationProvider, Calibration}

import scala.collection.JavaConverters._


final class TestCalibrationProvider(cals: List[Calibration]) extends CalibrationProvider {
  def getVersionInfo =
    null

  def getVersion(calType: Calibration.Type, instrument: java.lang.String) =
    null

  def getCalibrations(key: CalibrationKey) =
    cals.asJava
}