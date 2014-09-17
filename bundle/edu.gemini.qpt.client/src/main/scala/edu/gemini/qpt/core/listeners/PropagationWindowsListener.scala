package edu.gemini.qpt.core.listeners

import java.beans.PropertyChangeEvent

import edu.gemini.lch.services.model.Observation
import edu.gemini.qpt.core.util.{LttsServicesClient, MarkerManager}
import edu.gemini.qpt.core.{Marker, Variant}
import edu.gemini.qpt.shared.sp.Obs

import scala.collection.JavaConverters._


/**
 * Listener for model changes which adds markers for missing propagation windows.
 */
class PropagationWindowsListener extends MarkerModelListener[Variant] {

  def propertyChange(evt : PropertyChangeEvent): Unit = {
    val variant : Variant = evt.getSource.asInstanceOf[Variant]
    val markerManager : MarkerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)

    // Check if there are any laser observations for which we don't have clearance windows.
    val lgsAllocs = variant.getAllocs.asScala.filter(_.getObs.getLGS)
    lgsAllocs.filter(a => hasNoClearanceWindows(a.getObs)).foreach { a =>
      markerManager.addMarker(true, this, Marker.Severity.Error, "Target has no clearance windows.", variant, a)
    }
  }

  // check if qpt obs object is missing clearance windows
  def hasNoClearanceWindows(obs: Obs): Boolean = {
    val lttsObs = Option(LttsServicesClient.getInstance.getObservation(obs))
    lttsObs.isEmpty || hasNoClearanceWindows(lttsObs.get)
  }

  // check if the LTTS obs object is missing clearance windows
  def hasNoClearanceWindows(lttsObs: Observation): Boolean =
    lttsObs.getLaserTargets.asScala.exists(_.getClearanceWindows.isEmpty)


  protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }

}
