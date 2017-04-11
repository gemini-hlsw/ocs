package jsky.app.ot.editor.seq

import edu.gemini.itc.shared.{AnalysisMethod, ObservingConditions}
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.core.{SpectralDistribution, SpatialProfile, Redshift}
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort

import edu.gemini.shared.util.immutable.{Option => GOption}

import scalaz._
import Scalaz._

/** Helper class that provides a set of parameters that has to be collected from different places and
  * are needed by the ITC code to do its calculations. This class mostly exists in order to disentangle
  * the outside world (EdIterFolder, ItcPanel etc) from the ItcTable and ItcTableModel. */
trait ItcParametersProvider {
  def sequence: ConfigSequence
  def instrument: Option[SPInstObsComp]
  def schedulingBlockStart: GOption[java.lang.Long]
  def observation: Option[ISPObservation]
  def analysisMethod: String \/ AnalysisMethod
  def conditions: String \/ ObservingConditions
  def instrumentPort: String \/ IssPort
  def targetEnvironment: String \/ TargetEnvironment
  def spectralDistribution: String \/ SpectralDistribution
  def spatialProfile: String \/ SpatialProfile
  def redshift: String \/ Redshift
}

object ItcParametersProvider {

  /** Creates a parameters provider that can extract ITC values from a given EdIteratorFolder and ItcPanel. */
  def apply(owner: EdIteratorFolder, itcPanel: ItcPanel) = new ItcParametersProvider {

    def instrument: Option[SPInstObsComp] =
      Option(owner.getContextInstrumentDataObject)

    def schedulingBlockStart: GOption[java.lang.Long] =
      owner.getSchedulingBlockStart

    def observation: Option[ISPObservation] =
      Option(owner.getContextObservation)

    def analysisMethod: String \/ AnalysisMethod =
      itcPanel.analysis.fold("Analysis method is invalid".left[AnalysisMethod])(_.right)

    def conditions: String \/ ObservingConditions =
      itcPanel.conditions.right

    def spatialProfile: String \/ SpatialProfile =
      for {
        tEnv <- targetEnvironment
        sp   <- tEnv.getAsterism.ifSingle.getSpatialProfile.fold("Spatial profile not available".left[SpatialProfile])(_.right)
      } yield sp

    def spectralDistribution: String \/ SpectralDistribution =
      for {
        tEnv <- targetEnvironment
        sd   <- tEnv.getAsterism.ifSingle.getSpectralDistribution.fold("Spectral distribution not available".left[SpectralDistribution])(_.right)
      } yield sd

    def instrumentPort: String \/ IssPort =
      Option(owner.getContextIssPort).fold("No port information available".left[IssPort])(_.right)

    def targetEnvironment: String \/ TargetEnvironment =
      Option(owner.getContextTargetEnv).fold("No target environment available".left[TargetEnvironment])(_.right)

    def redshift: String \/ Redshift =
      for {
        tEnv <- targetEnvironment
      } yield tEnv.getAsterism.ifSingle.getSiderealTarget.flatMap(_.redshift).getOrElse(Redshift.zero)

    def sequence: ConfigSequence = Option(owner.getContextObservation).fold(new ConfigSequence) {
      ConfigBridge.extractSequence(_, null, ConfigValMapInstances.IDENTITY_MAP, true)
    }

  }
}
