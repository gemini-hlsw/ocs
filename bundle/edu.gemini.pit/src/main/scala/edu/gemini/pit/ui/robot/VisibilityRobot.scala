package edu.gemini.pit.ui.robot

import edu.gemini.pit.model.Model
import edu.gemini.spModel.core.Coordinates

import scalaz.Lens
import edu.gemini.model.p1.visibility.TargetVisibilityCalc
import edu.gemini.model.p1.immutable._

object VisibilityRobot extends ObservationMetaRobot[(ProposalClass, BlueprintBase, Coordinates), TargetVisibility] {

  protected val valueLens: Lens[ObservationMeta, Option[TargetVisibility]] =
    Lens.lensu((a, b) => a.copy(visibility = b), _.visibility)

  protected def key(o: Observation): Option[(ProposalClass, BlueprintBase, Coordinates)] =
    for {
      m <- model
      t <- o.target
      b <- o.blueprint
      c <- t.coords(m.proposal.semester.midPoint)
    } yield (m.proposal.proposalClass, b, c)

  protected def query(o: Observation): Option[TargetVisibility] = {
    model.flatMap {
      m =>
        if (m.proposal.proposalClass.isSpecial) TargetVisibilityCalc.getOnDec(m.proposal.semester, o) else TargetVisibilityCalc.get(m.proposal.semester, o)
    }
  }

  // Override caching rules so we update visibility every time a proposal changes
  override def missing(m: Model): List[((ProposalClass, BlueprintBase, Coordinates), Observation)] =
    obsLens.get(m) collect {
      case o if key(o).isDefined => (key(o).get, o)
    }

  override def doRefresh(m: Model): Unit = {
    // Mark all checks as pending so the visibility is updated after every change
    // As Visibility calculation is very fast this doesn't affect performance
    model.foreach {
      m =>
        // Cache the result and update the model
        for {
          o <- m.proposal.observations
          k = key(o)
          if k.isDefined
        } state = state + (key(o).get -> Result.Pending)
    }

    super.doRefresh(m)
  }
}
