package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.spModel.target.system.{ConicTarget, ITarget}

final class MpcMinorPlanetDetailEditor extends ConicDetailEditor(ITarget.Tag.MPC_MINOR_PLANET) {
  import NumericPropertySheet.Prop

  lazy val props = NumericPropertySheet[ConicTarget](Some("Orbital Elements"), _.getTarget.asInstanceOf[ConicTarget],
    Prop("EPOCH", "Orbital Element Epoch (JD)",        _.getEpoch),
    Prop("IN",    "Inclination (deg)",                 _.getInclination),
    Prop("OM",    "Longitude of Ascending Node (deg)", _.getANode),
    Prop("W",     "Argument of Perihelion (deg)",      _.getPerihelion),
    Prop("A",     "Semi-major Axis (AU)",              _.getAQ),
    Prop("EC",    "Eccentricity",                      _.getE, _.setE(_)),
    Prop("MA",    "Mean Anomaly (deg)",                _.getLM)
  )

}
