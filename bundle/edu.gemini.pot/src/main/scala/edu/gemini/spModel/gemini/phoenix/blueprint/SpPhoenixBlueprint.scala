package edu.gemini.spModel.gemini.phoenix.blueprint

import edu.gemini.spModel.gemini.phoenix.InstPhoenix
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.{ Mask, Filter }
import edu.gemini.spModel.pio.codec.{ParamCodec, ParamSetCodec}
import edu.gemini.spModel.pio.{PioFactory, ParamSet}

import edu.gemini.spModel.template.SpBlueprint

import scalaz._, Scalaz._

case class SpPhoenixBlueprint(fpu: Mask, filter: Filter) extends SpBlueprint {

  val instrumentType = InstPhoenix.SP_TYPE
  val paramSetName   = SpPhoenixBlueprint.PARAM_SET_NAME

  /** Secondary constructor with tupled args, needed below. */
  def this(p: (Mask, Filter)) =
    this(p._1, p._2)

  /** Secondary constructor used via reflective access in SpBlueprintFactory. */
  def this(ps: ParamSet) =
    this {
      ParamSetCodec[SpPhoenixBlueprint].decode(ps) match {
        case -\/(e) => sys.error(e.toString)
        case \/-(b) => (b.fpu, b.filter)
      }
    }

  def toParamSet(factory: PioFactory): ParamSet =
    ParamSetCodec[SpPhoenixBlueprint].encode(paramSetName, this)

}

object SpPhoenixBlueprint {

  val PARAM_SET_NAME = "phoenix-blueprint"

  val fpu:    SpPhoenixBlueprint @> Mask   = Lens.lensu((a, b) => a.copy(fpu = b),    _.fpu)
  val filter: SpPhoenixBlueprint @> Filter = Lens.lensu((a, b) => a.copy(filter = b), _.filter)

  implicit val PhoenixMaskParamCodec: ParamCodec[Mask] =
    ParamCodec[String].xmap(Mask.valueOf, _.name)

  implicit val PhoenixFilterCodec: ParamCodec[Filter] =
    ParamCodec[String].xmap(Filter.valueOf, _.name)

  implicit val PhoenixParamSetCodec: ParamSetCodec[SpPhoenixBlueprint] =
    ParamSetCodec.initial(SpPhoenixBlueprint(Mask.DEFAULT, Filter.DEFAULT))
      .withParam("fpu", fpu)
      .withParam("filter", filter)

}
