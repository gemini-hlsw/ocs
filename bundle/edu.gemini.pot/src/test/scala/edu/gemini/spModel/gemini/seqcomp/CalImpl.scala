package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.shared.util.immutable.{ImCollections, ImList}
import edu.gemini.spModel.gemini.calunit.CalUnitParams.{Diffuser, Filter, Shutter, Lamp}
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration

import java.lang.{Boolean => JBoolean, Double => JDouble, Integer => JInteger}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

final case class CalImpl(lamps: Set[Lamp],
           @BeanProperty shutter: Shutter,
           @BeanProperty filter: Filter,
           @BeanProperty diffuser: Diffuser,
                         observe: Int,
                         exposureTime: Double,
                         coadds: Int,
                         arc: Boolean,
                         basecalNight: Boolean = false,
                         basecalDay:   Boolean = false
) extends Calibration {

  // Satisfy the Calibration interface

  def isFlat: JBoolean               = !isArc
  def isArc: JBoolean                = arc
  def isBasecalNight: JBoolean       = basecalNight
  def isBasecalDay: JBoolean         = basecalDay

  def getLamps: java.util.Set[Lamp] = lamps.asJava
  def getObserve: JInteger          = observe
  def getExposureTime: JDouble      = exposureTime
  def getCoadds: JInteger           = coadds

  def export: ImList[String]        = ImCollections.emptyList[String]
}
