package edu.gemini.qpt.core.listeners

import edu.gemini.qpt.core.Alloc
import edu.gemini.qpt.core.Marker
import edu.gemini.qpt.core.Variant
import edu.gemini.qpt.core.util.MarkerManager
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.env.GuideGroup
import java.beans.PropertyChangeEvent
import scala.collection.JavaConverters._

object TargetEnvironmentListener {

  // Turn the Gemini Java Option into a real Option
  implicit def pimpLameOption[T](o: edu.gemini.shared.util.immutable.Option[T]) = new Object {
    def asScala: Option[T] =
      if (o.isEmpty) None
      else Some(o.getValue)
  }

  // We use a set of GuideProbes to check for some specific exception cases.
  private val canopusGuiders: Set[GuideProbe] = Canopus.Wfs.values().toSet
  private val odgwGuiders: Set[GuideProbe]    = GsaoiOdgw.values().toSet

  private val gsaoiGuiders: Set[GuideProbe]  = canopusGuiders ++ odgwGuiders
  private val f2GemsGuiders: Set[GuideProbe] = canopusGuiders + Flamingos2OiwfsGuideProbe.instance
  private def isGemsConfiguration(s: Set[GuideProbe]): Boolean =
    s.subsetOf(gsaoiGuiders) || s.subsetOf(f2GemsGuiders)
}

import TargetEnvironmentListener._

/**
 * Generates Alloc markers if there is anything special or unusual about the target environment.
 * @author sraaphor
 */
class TargetEnvironmentListener extends MarkerModelListener[Variant] {

  def propertyChange(evt: PropertyChangeEvent): Unit = {
    val variant = evt.getSource.asInstanceOf[Variant]
    val markerManager = variant.getSchedule.getMarkerManager
    markerManager.clearMarkers(this, variant)


    // Iterate over the observations in the variant and determine if they should generate markers.
    for (a: Alloc <- variant.getAllocs.asScala if a.getObs.getTargetEnvironment != null) {
      val targetEnvironment = a.getObs.getTargetEnvironment

      // Check for blind offsetting. This is when user targets are supplied.
      if (targetEnvironment.getUserTargets.size() > 0) {
        markerManager.addMarker(false, this, Marker.Severity.Warning, "Observation may use blind offsetting.", variant, a)
      }

      // Check for non-sidereal targets.
      if (targetEnvironment.getAsterism.isNonSidereal) {
        markerManager.addMarker(false, this, Marker.Severity.Warning, "Observation has a non-sidereal target.", variant, a)
      }

      // We now check for the GuideEnvironment cases that should generate warnings. These are:
      // 1. Multiple guide groups have targets.
      // 2. A guide group has multiple guide probe targets, unless those happen to all be GSAOI ODGW and/or Canopus CWFS,
      //    or multiple Canopus CWFS with F2 OIWFS.
      // 3. If any guide probe targets instance has multiple targets.
      // 4. Primary group uses NIFS + OIWFS.

      // UPDATE SW: let's ignore multiple groups (case 1) for now, they are
      // mostly a GeMS phenomenon. We'll just work with the primary guide group,
      // if any. That may well be what they want anyway.

      def addGuideGroupWarnings(guideGroup: GuideGroup): Unit = {
        // List[(GuideProbe, Int)] where Int is > 0
        // Note, we know that a GuideGroup will not have multiple GuideProbeTargets
        // instances for the same GuideProbe so each GuideProbe is represented
        // at most 1X
        val guideProbeTargetCounts = guideGroup.getAll.toList.asScala.collect {
          case gpt if gpt.getTargets.size > 0 => gpt.getGuider -> gpt.getTargets.size
        }

        // Case 4: warn specifically about NIFS OIWFS
        if (guideProbeTargetCounts.exists(_._1 == NifsOiwfsGuideProbe.instance)) {
          markerManager.addMarker(false, this, Marker.Severity.Warning, "NIFS + OIWFS.", variant, a)
        }

        // Case 3: add warnings for each guide probe with multiple options
        guideProbeTargetCounts.foreach {
          case (guideProbe, count) =>
            if (count > 1) markerManager.addMarker(false, this, Marker.Severity.Warning, s"Multiple ${guideProbe.getKey} options.", variant, a)
        }

        // Case 2: multiple guiders in use (which is okay for Gems)
        val allGuideProbesEmployed = guideProbeTargetCounts.map(_._1).toSet
        if ((allGuideProbesEmployed.size > 1) && !isGemsConfiguration(allGuideProbesEmployed)) {
          val probes = allGuideProbesEmployed.map(_.getKey).toList.sorted.mkString(", ")
          markerManager.addMarker(false, this, Marker.Severity.Warning, s"Multiple guide probes in use: $probes", variant, a)
        }
      }

      addGuideGroupWarnings(targetEnvironment.getGuideEnvironment.getPrimary)
    }
  }

  protected def getMarkerManager(t: Variant): MarkerManager = {
    t.getSchedule.getMarkerManager
  }
}