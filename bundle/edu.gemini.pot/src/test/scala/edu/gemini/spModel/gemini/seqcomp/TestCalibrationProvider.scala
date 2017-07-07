package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.{CalibrationKey, CalibrationProvider, Calibration}

import java.util.stream.Stream

import scala.collection.JavaConverters._


final class TestCalibrationProvider(cals: List[Calibration]) extends CalibrationProvider {
  def getVersionInfo =
    null

  def getVersion(calType: Calibration.Type, instrument: java.lang.String) =
    null

  def export(calType: Calibration.Type, instrument: String): Stream[ImList[String]] =
    Stream.empty[ImList[String]]

  def getCalibrations(key: CalibrationKey) =
    cals.asJava
}