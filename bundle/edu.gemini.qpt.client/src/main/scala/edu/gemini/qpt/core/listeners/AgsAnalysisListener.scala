package edu.gemini.qpt.core.listeners

import java.beans.PropertyChangeEvent

import edu.gemini.ags.api.{AgsAnalysis, AgsAnalysisWithGuideProbe}
import edu.gemini.ags.api.AgsGuideQuality.{PossiblyUnusable, IqDegradation, PossibleIqDegradation, DeliversRequestedIq}
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
      val analysisConcerns = alloc.getObs.getAgsAnalysis.asScala.filterNot(_.qualityOption == Some(DeliversRequestedIq))
      analysisConcerns.foreach { analysis =>
        markerManager.addMarker(false, this, analysisSeverity(analysis), analysisMessage(analysis), variant, alloc)
      }
    }
  }

  override protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }
}


object AgsAnalysisListener {
  /**
   * Note that DeliversRequestedIq is already filtered out by this point, so we simply do not care
   * what Severity is returned for it since this should never happen.
   */
  def analysisSeverity(analysis: AgsAnalysis): Severity =
    analysis match {
      case _: AgsAnalysis.Usable             => Severity.Warning
      case _: AgsAnalysis.NoMagnitudeForBand => Severity.Warning
      case _                                 => Severity.Error
    }

  def analysisMessage(analysis: AgsAnalysis): String = (analysis match {
    case agp: AgsAnalysisWithGuideProbe => s"${agp.guideProbe.getKey}: "
    case _ => ""
  }) + analysis.message
}