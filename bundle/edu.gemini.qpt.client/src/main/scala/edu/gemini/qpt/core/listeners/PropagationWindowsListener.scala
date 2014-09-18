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

<<<<<<< HEAD
  def propertyChange(evt : PropertyChangeEvent) = evt.getSource match {
    case variant: Variant => addMarkers(variant)
    case _ => throw new RuntimeException("Listener can only be used with variants.")
  }

  // add markers as needed
  private def addMarkers(variant: Variant): Unit = {
    val markerManager : MarkerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)

=======
  def propertyChange(evt : PropertyChangeEvent): Unit = {
    val variant : Variant = evt.getSource.asInstanceOf[Variant]
    val markerManager : MarkerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)

    // Check if there are any laser observations for which we don't have clearance windows.
>>>>>>> 2fb8c91297394db849f1cdb4feb49496861dee53
    val lgsAllocs = variant.getAllocs.asScala.filter(_.getObs.getLGS)
    lgsAllocs.filter(a => hasNoClearanceWindows(a.getObs)).foreach { a =>
      markerManager.addMarker(false, this, Marker.Severity.Error, "LGS observation has no clearance windows.", variant, a)
    }
  }

  // check if qpt obs object is missing clearance windows
<<<<<<< HEAD
  private def hasNoClearanceWindows(obs: Obs): Boolean = {
=======
  def hasNoClearanceWindows(obs: Obs): Boolean = {
>>>>>>> 2fb8c91297394db849f1cdb4feb49496861dee53
    val lttsObs = Option(LttsServicesClient.getInstance.getObservation(obs))
    lttsObs.isEmpty || hasNoClearanceWindows(lttsObs.get)
  }

  // check if the LTTS obs object is missing clearance windows
<<<<<<< HEAD
  private def hasNoClearanceWindows(lttsObs: Observation): Boolean =
=======
  def hasNoClearanceWindows(lttsObs: Observation): Boolean =
>>>>>>> 2fb8c91297394db849f1cdb4feb49496861dee53
    lttsObs.getLaserTargets.asScala.exists(_.getClearanceWindows.isEmpty)


  protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }

}
