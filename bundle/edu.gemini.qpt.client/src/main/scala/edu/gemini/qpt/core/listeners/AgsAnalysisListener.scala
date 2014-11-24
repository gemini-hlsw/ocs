package edu.gemini.qpt.core.listeners

import edu.gemini.ags.api.AgsGuideQuality._

import java.beans.PropertyChangeEvent

import edu.gemini.ags.api.AgsAnalysis
import edu.gemini.qpt.core.Marker.Severity
import edu.gemini.qpt.core.Variant
import edu.gemini.qpt.core.util.MarkerManager

import scala.collection.JavaConverters._


class AgsAnalysisListener extends MarkerModelListener[Variant] {
  import AgsAnalysisListener._

  override def propertyChange(evt: PropertyChangeEvent): Unit = {
    val variant = evt.getSource.asInstanceOf[Variant]
    val markerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)

    // Iterate over the observations in the variant and determine if they should generate
    // markers based on the AgsAnalysis.
    for (alloc <- variant.getAllocs.asScala if !alloc.getObs.getAgsAnalysis.isEmpty) {
      // Only analyses with a severity level should generate a marker.
      for {
        a <- alloc.getObs.getAgsAnalysis.asScala
        s <- severity(a)
      } yield {
        markerManager.addMarker(false, this, s, a.message(withProbe = true), variant, alloc)
      }
    }
  }

  override protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }
}

object AgsAnalysisListener {
  def severity(a: AgsAnalysis): Option[Severity] =
    a.quality match {
      case DeliversRequestedIq   => None
      case PossibleIqDegradation => Some(Severity.Warning)
      case IqDegradation         => Some(Severity.Warning)
      case PossiblyUnusable      => Some(Severity.Warning)
      case Unusable              => Some(Severity.Error)
    }
}