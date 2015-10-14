package edu.gemini.spModel.target

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.pio.Param
import edu.gemini.spModel.pio.codec._

import squants.motion.{ Velocity, KilometersPerSecond }

import scalaz._, Scalaz._

object TargetParamCodecs {

  implicit val RedshiftParamCodec: ParamCodec[Redshift] =
    ParamCodec[Double].xmap(Redshift(_), _.redshift)

  implicit val AngleParamCodec: ParamCodec[Angle] =
    ParamCodec[Double].xmap(Angle.fromDegrees, _.toDegrees)

  implicit val ParallaxParamCodec: ParamCodec[Parallax] =
    ParamCodec[Angle].xmap(Parallax(_), _.angle)

  implicit val VelocityParamCodec: ParamCodec[Velocity] =
    ParamCodec[Double].xmap(KilometersPerSecond(_), _.toKilometersPerSecond)

  implicit val RadialVelocityParamCodec: ParamCodec[RadialVelocity] =
    ParamCodec[Velocity].xmap(RadialVelocity(_), _.velocity)

  implicit val RaParamCodec: ParamCodec[RA] =
    ParamCodec[Angle].xmap(RA.fromAngle, _.toAngle)

  implicit val MagnitudeBParamCodec: ParamCodec[MagnitudeBand] =
    ParamCodec[String].xmap(MagnitudeBand.unsafeFromString, _.name)

  implicit val MagnitudeSysParamCodec: ParamCodec[MagnitudeSystem] =
    ParamCodec[String].xmap(MagnitudeSystem.unsafeFromString, _.name)

  implicit val AngularVelocParamCodec: ParamCodec[AngularVelocity] =
    ParamCodec[Double].xmap(AngularVelocity(_), _.masPerYear)

  implicit val RightAscensionAngularVelocParamCodec: ParamCodec[RightAscensionAngularVelocity] =
    ParamCodec[AngularVelocity].xmap(RightAscensionAngularVelocity(_), _.velocity)

  implicit val DeclinationAngularVelocParamCodec: ParamCodec[DeclinationAngularVelocity] =
    ParamCodec[AngularVelocity].xmap(DeclinationAngularVelocity(_), _.velocity)

  implicit val EpochParamCodec: ParamCodec[Epoch] = 
    ParamCodec[Double].xmap(Epoch(_), _.year)

  implicit val DecParamCodec: ParamCodec[Dec] =
    new ParamCodec[Dec] {
      def encode(key: String, a: Dec): Param = AngleParamCodec.encode(key, a.toAngle)
      def decode(p: Param): PioError \/ Dec =
        AngleParamCodec.decode(p).flatMap { a =>
          Dec.fromAngle(a) \/> ParseError(p.getName, a.toString, "Dec")
        }
    }

}

