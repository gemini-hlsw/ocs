package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.spModel.target.system.ConicTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.CoordinateTypes.Epoch
import edu.gemini.spModel.target.system.ITarget.Tag

final class JplMinorBodyDetailEditor extends ConicDetailEditor(Tag.JPL_MINOR_BODY) {
  import NumericPropertySheet.Prop

  lazy val props = NumericPropertySheet[ConicTarget](Some("Orbital Elements"), _.getTarget.asInstanceOf[ConicTarget],
    Prop("EPOCH", "Orbital Element Epoch (JD)",        _.getEpoch.getValue, (t,d) => t.setEpoch(new Epoch(d, Units.JD))),
    Prop("IN",    "Inclination (deg)",                 _.getInclination),
    Prop("OM",    "Longitude of Ascending Node (deg)", _.getANode),
    Prop("W",     "Argument of Perihelion (deg)",      _.getPerihelion),
    Prop("QR",    "Perihelion Distance (AU)",          _.getAQ),
    Prop("EC",    "Eccentricity",                      _.getE, _.setE(_)),
    Prop("TP",    "Time of Perihelion Passage (JD)",   _.getEpochOfPeri.getValue, (t, d) => t.setEpochOfPeri(new Epoch(d, Units.JD)))
  )

}