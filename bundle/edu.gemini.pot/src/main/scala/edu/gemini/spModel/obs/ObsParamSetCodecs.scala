package edu.gemini.spModel.obs

import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{ParamSet, Pio}

import scalaz.Scalaz._
import scalaz._

object ObsParamSetCodecs {

  implicit val UnstatedParamSetCodec: ParamSetCodec[Unstated.type] =
    ParamSetCodec.initial(Unstated)

  implicit val ExplicitParamSetCodec: ParamSetCodec[Explicit] =
    ParamSetCodec.initial(Explicit(0L))
      .withParam("ms", Explicit.ms)

  implicit val ComputedParamSetCodec: ParamSetCodec[Computed] =
    ParamSetCodec.initial(Computed(0L))
      .withParam("ms", Computed.ms)

  implicit val DurationParamSetCodec: ParamSetCodec[Duration] =
    new ParamSetCodec[Duration] {
      val pf = new PioXmlFactory
      def encode(key: String, d: Duration): ParamSet = {
        val (tag, ps) = d match {
          case u @ Unstated     => ("unstated", UnstatedParamSetCodec.encode(key, Unstated))
          case e @ Explicit(ms) => ("explicit", ExplicitParamSetCodec.encode(key, e))
          case c @ Computed(ms) => ("computed", ComputedParamSetCodec.encode(key, c))
        }
        Pio.addParam(pf, ps, "tag", tag)
        ps
      }
      def decode(ps: ParamSet): PioError \/ Duration = {
        (Option(ps.getParam("tag")).map(_.getValue) \/> MissingKey("tag")) flatMap {
          case "unstated" => UnstatedParamSetCodec.decode(ps)
          case "explicit" => ExplicitParamSetCodec.decode(ps)
          case "computed" => ComputedParamSetCodec.decode(ps)
          case hmm        => UnknownTag(hmm, "Target").left
        }
      }
    }

  implicit val SchedulingBlockParamSetCodec: ParamSetCodec[SchedulingBlock] =
    ParamSetCodec.initial(SchedulingBlock(0L, Unstated))
      .withParam("start", SchedulingBlock.start)
      .withParamSet("duration", SchedulingBlock.duration)

}




