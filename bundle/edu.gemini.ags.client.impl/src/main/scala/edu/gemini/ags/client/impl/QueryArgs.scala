package edu.gemini.ags.client.impl

import edu.gemini.model.p1.immutable._

/**
 * Support for extracting AGS service query arguments from an observation
 * and a time.
 */
object QueryArgs {
  type Arg = (String, String)

  // We are only sending the coordinates at a given time, but the target type
  // is used in some cases to select the guider to use.
  private def targetType(target:Target):Either[String, String] =
    target match {
      case _:NonSiderealTarget => Right("nonsidereal")
      case _:SiderealTarget    => Right("sidereal")
      case _:TooTarget         => Left("Cannot estimate ToO targets")
    }

  def targetArgs(obs:Observation, time:Long):Either[String, Seq[Arg]] =
    for {
      target <- obs.target.toRight("Missing Target").right
      coords <- target.coords(time).toRight("Missing Coordinates").right
      ttype  <- targetType(target).right
    } yield Seq("ra" -> coords.ra.toAngle.formatHMS, "dec" -> coords.dec.formatDMS, "targetType" -> ttype)

  def conditionArgs(obs:Observation):Either[String, Seq[Arg]] =
    for {
      conds <- obs.condition.toRight("Missing Observing Conditions").right
    } yield Seq("cc" -> conds.cc.name(), "iq" -> conds.iq.name(), "sb" -> conds.sb.name())

  def altair(blue: BlueprintBase): Option[Altair] = blue match {
    case gnirs: GnirsBlueprintBase => Some(gnirs.altair)
    case nifs: NifsBlueprintAo     => Some(nifs.altair)
    case niri: NiriBlueprint       => Some(niri.altair)
    case _                         => None
  }

  def altairParamValue(a: Altair): String = a match {
    case AltairNone          => "NO"
    case AltairLGS(p1, _, _) => if (p1) "LGS_P1" else "LGS"
    case AltairNGS(fl)       => if (fl) "NGS_FL" else "NGS"
  }
  def altairArg(blue: BlueprintBase): Seq[Arg] = altair(blue).map(a => "altair" -> altairParamValue(a)).toList

  def instSpecificArgs(blue: BlueprintBase): Seq[Arg] = (blue match {
    case visitor: VisitorBlueprint => Some("niriCamera" -> NiriCamera.F6.name()) // REL-1090
    case niri: NiriBlueprint       => Some("niriCamera" -> niri.camera.name())
    case _ => None
  }).toList

  def instId(b: BlueprintBase): Either[String, String] = b match {
    case g: AlopekeBlueprint    => Right(Instrument.Nifs.id) // REL-3351: treat Alopeke like NIFS until Visitor PWFS is changeable.
    case g: TexesBlueprint      => Right(Instrument.Nifs.id) // REL-1062
    case g: DssiBlueprint       => Right(Instrument.Nifs.id) // REL-1061
    case g: VisitorBlueprint    => Right(Instrument.Niri.id) // REL-1090
    case g: GeminiBlueprintBase => Right(g.instrument.id)
    case _ => Left("Not a Gemini Instrument")
  }

  def instArgs(obs:Observation):Either[String, Seq[Arg]] =
    for {
      blue <- obs.blueprint.toRight("Missing Blueprint").right
      id <- instId(blue).right
    } yield Seq("inst" -> id) ++ instSpecificArgs(blue) ++ altairArg(blue)

  def fixedArgs:Seq[Arg] = Seq("pac" -> "UNKNOWN")

  /**
   * Obtains either all the query arguments or an error message if anything
   * is missing.
   */
  def all(obs:Observation, time:Long):Either[String, Seq[Arg]] =
    for {
      target <- targetArgs(obs, time).right
      conds <- conditionArgs(obs).right
      inst <- instArgs(obs).right
    } yield target ++ conds ++ inst ++ fixedArgs
}