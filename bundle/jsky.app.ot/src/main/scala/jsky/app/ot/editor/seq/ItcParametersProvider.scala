package jsky.app.ot.editor.seq

import edu.gemini.itc.shared.{AnalysisMethod, ObservingConditions, SpatialProfile, SpectralDistribution}
import edu.gemini.pot.sp.{ISPObservation, SPComponentType}
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort

import scalaz._
import Scalaz._

/** Helper class that provides a set of parameters that has to be collected from different places and
  * are needed by the ITC code to do its calculations. This class mostly exists in order to disentangle
  * the outside world (EdIterFolder, ItcPanel etc) from the ItcTable and ItcTableModel. */
trait ItcParametersProvider {
  def sequence: ConfigSequence
  def instrument: Option[SPComponentType]
  def observation: Option[ISPObservation]
  def analysisMethod: AnalysisMethod
  def conditions: String \/ ObservingConditions
  def instrumentPort: String \/ IssPort
  def targetEnvironment: String \/ TargetEnvironment
  def spectralDistribution: String \/ SpectralDistribution
  def spatialProfile: String \/ SpatialProfile
  def redshift: String \/ Double
}

object ItcParametersProvider {

  /** Creates a parameters provider that can extract ITC values from a given EdIteratorFolder and ItcPanel. */
  def apply(owner: EdIteratorFolder, itcPanel: ItcPanel) = new ItcParametersProvider {

    def instrument: Option[SPComponentType] =
      Option(owner.getContextInstrument).map(_.getType)

    def observation: Option[ISPObservation] =
      Option(owner.getContextObservation)

    def analysisMethod: AnalysisMethod =
      itcPanel.analysis

    def conditions: String \/ ObservingConditions =
      itcPanel.conditions.right

    def spatialProfile: String \/ SpatialProfile =
      itcPanel.spatialProfile.fold("Spatial profile not available".left[SpatialProfile])(_.right)

    def spectralDistribution: String \/ SpectralDistribution =
      itcPanel.spectralDistribution.fold("Spectral distribution not available".left[SpectralDistribution])(_.right)

    def instrumentPort: String \/ IssPort =
      Option(owner.getContextIssPort).fold("No port information available".left[IssPort])(_.right)

    def targetEnvironment: String \/ TargetEnvironment =
      Option(owner.getContextTargetEnv).fold("No target environment available".left[TargetEnvironment])(_.right)

    def redshift: String \/ Double =
      0.0.right // TODO: get this from target editor!

    def sequence: ConfigSequence = Option(owner.getContextObservation).fold(new ConfigSequence) {
      ConfigBridge.extractSequence(_, null, ConfigValMapInstances.IDENTITY_MAP, true)
    }



  }
}
