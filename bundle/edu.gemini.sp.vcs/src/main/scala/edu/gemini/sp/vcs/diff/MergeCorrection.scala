package edu.gemini.sp.vcs.diff

import java.security.Permission

object MergeCorrection {

  /**
   * A `CorrectionFunction` is just a function that modifies an `MergePlan` to
   * correct some aspect of the merge.
   */
  type CorrectionFunction = MergePlan => TryVcs[MergePlan]

  type PermissionCheck    = Permission => VcsAction[Boolean]

  type CorrectionAction   = (MergePlan, PermissionCheck) => VcsAction[MergePlan]

  def apply(mc: MergeContext): CorrectionAction =
    (mp: MergePlan, hasPermission: PermissionCheck) =>
      for {
        on  <- ObsNumberCorrection(mc)(mp).liftVcs
        v   <- ValidityCorrection(mc)(on).liftVcs
        sof <- StaffOnlyFieldCorrection(mc)(v, hasPermission)
      } yield sof
}
