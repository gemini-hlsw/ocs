package edu.gemini.qpt.core.listeners

import java.beans.PropertyChangeEvent

import edu.gemini.lch.services.model.Observation
import edu.gemini.qpt.core.util.{LttsServicesClient, MarkerManager}
import edu.gemini.qpt.core.{Marker, Variant}
import edu.gemini.qpt.shared.sp.Obs

import scala.collection.JavaConverters._


/**
 * Listener for model changes which adds markers for observations without clearance windows.
 */
class PropagationWindowsListener extends MarkerModelListener[Variant] {

  def propertyChange(evt : PropertyChangeEvent) = evt.getSource match {
    case variant: Variant => addMarkers(variant)
    case _ => throw new RuntimeException("Listener can only be used with variants.")
  }

  // add markers as needed
  private def addMarkers(variant: Variant): Unit = {
    val markerManager : MarkerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)

    val lgsAllocs = variant.getAllocs.asScala.filter(_.getObs.getLGS)
    lgsAllocs.filter(a => hasNoClearanceWindows(a.getObs)).foreach { a =>
      markerManager.addMarker(false, this, Marker.Severity.Error, "LGS observation has no clearance windows.", variant, a)
    }
  }

  // check if qpt obs object is missing clearance windows
  private def hasNoClearanceWindows(obs: Obs): Boolean = {
    val lttsObs = Option(LttsServicesClient.getInstance.getObservation(obs))
    lttsObs.isEmpty || hasNoClearanceWindows(lttsObs.get)
  }

  // check if the LTTS obs object is missing clearance windows
  private def hasNoClearanceWindows(lttsObs: Observation): Boolean =
    lttsObs.getLaserTargets.asScala.exists(_.getClearanceWindows.isEmpty)


  protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }

}
