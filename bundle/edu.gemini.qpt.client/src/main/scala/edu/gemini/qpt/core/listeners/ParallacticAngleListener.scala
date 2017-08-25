package edu.gemini.qpt.core.listeners

import edu.gemini.qpt.core.{Marker, Alloc, Variant}
import edu.gemini.qpt.core.util.MarkerManager

import java.beans.PropertyChangeEvent
import scala.collection.JavaConverters._



class ParallacticAngleListener extends MarkerModelListener[Variant] {
  def propertyChange(evt : PropertyChangeEvent): Unit = {
    val variant : Variant = evt.getSource.asInstanceOf[Variant]
    val markerManager : MarkerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)


    // Iterate over the observations in the variant and determine if they should generate markers.
    val parAngleAllocs = variant.getAllocs.asScala.filter{ _.getObs.usesMeanParallacticAngle() }
    for (a : Alloc <- parAngleAllocs) {
      markerManager.addMarker(false, this, Marker.Severity.Notice, "Observation uses mean parallactic angle.", variant, a)
    }
  }

  protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }
}
