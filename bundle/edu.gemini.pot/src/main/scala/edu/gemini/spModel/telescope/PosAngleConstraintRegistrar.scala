package edu.gemini.spModel.telescope

import edu.gemini.pot.sp.SPComponentType


object PosAngleConstraintRegistrar {
  private val defaultList = List(PosAngleConstraint.FIXED,
                                 PosAngleConstraint.FIXED_180,
                                 PosAngleConstraint.UNBOUNDED,
                                 PosAngleConstraint.PARALLACTIC_ANGLE)

  private val registrar = Map[SPComponentType, List[PosAngleConstraint]](
    SPComponentType.INSTRUMENT_GMOS       -> defaultList,
    SPComponentType.INSTRUMENT_FLAMINGOS2 -> defaultList,
    SPComponentType.INSTRUMENT_GNIRS      -> List(PosAngleConstraint.FIXED, PosAngleConstraint.PARALLACTIC_ANGLE)
  )

  def validPosAngleConstraintsForInstrument(key: SPComponentType) = registrar(key)
}
