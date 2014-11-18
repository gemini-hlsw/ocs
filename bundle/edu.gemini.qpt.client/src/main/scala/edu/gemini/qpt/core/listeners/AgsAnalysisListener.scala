package edu.gemini.qpt.core.listeners

import java.beans.PropertyChangeEvent

import edu.gemini.ags.api.{AgsSeverity, AgsAnalysis}
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
        s <- a.severityLevel
      } yield {
        markerManager.addMarker(false, this, severityToQptSeverity(s),
                                AgsAnalysis.analysisToMessage(a, showGuideProbeName = true), variant, alloc)
      }
    }
  }

  override protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }
}


object AgsAnalysisListener {
  val severityToQptSeverity = Map[AgsSeverity,Severity](
    AgsSeverity.Warning -> Severity.Warning,
    AgsSeverity.Error   -> Severity.Error
  )
}