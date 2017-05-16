package edu.gemini.spModel.target

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.pio.{ Pio, ParamSet }
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.TargetParamCodecs._

import scalaz._, Scalaz._

object TargetParamSetCodecs {

  implicit val CoordinatesParamSetCodec: ParamSetCodec[Coordinates] =
    ParamSetCodec.initial(Coordinates.zero)
      .withParam("ra",  Coordinates.ra)
      .withParam("dec", Coordinates.dec)

  implicit val ProperMotionParamSetCodec: ParamSetCodec[ProperMotion] =
    ParamSetCodec.initial(ProperMotion.zero)
      .withParam("delta-ra",  ProperMotion.deltaRA)
      .withParam("delta-dec", ProperMotion.deltaDec)
      .withParam("epoch",     ProperMotion.epoch)

  implicit val MagnitudeParamSetCodec: ParamSetCodec[Magnitude] =
    ParamSetCodec.initial(Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega))
      .withParam("value",         Magnitude.value)
      .withParam("band",          Magnitude.band)
      .withParam("system",        Magnitude.system)
      .withOptionalParam("error", Magnitude.error)

  implicit val SpectralDistributionParamSetCodec: ParamSetCodec[SpectralDistribution] =
    new ParamSetCodec[SpectralDistribution] {
      val pf = new PioXmlFactory
      def encode(key: String, a: SpectralDistribution): ParamSet =
        pf.createParamSet(key) <| (_.addParamSet(SourcePio.toParamSet(a, pf)))
      def decode(ps: ParamSet): \/[PioError, SpectralDistribution] =
        SourcePio.distributionFromParamSet(ps) \/> GeneralError("SpectralDistribution")
    }

  implicit val SpatialProfileParamSetCodec: ParamSetCodec[SpatialProfile] =
    new ParamSetCodec[SpatialProfile] {
      val pf = new PioXmlFactory
      def encode(key: String, a: SpatialProfile): ParamSet =
        pf.createParamSet(key) <| (_.addParamSet(SourcePio.toParamSet(a, pf)))
      def decode(ps: ParamSet): \/[PioError, SpatialProfile] =
        SourcePio.profileFromParamSet(ps) \/> GeneralError("SpectralDistribution")
    }

  implicit val SiderealTargetParamSetCodec: ParamSetCodec[SiderealTarget] = 
    ParamSetCodec.initial(SiderealTarget.empty)
      .withParam("name",                             SiderealTarget.name)
      .withOptionalParam("redshift",                 SiderealTarget.redshift)
      .withOptionalParam("parallax",                 SiderealTarget.parallax)
      .withManyParamSet("magnitude",                 SiderealTarget.magnitudes)
      .withParamSet("coordinates",                   SiderealTarget.coordinates)
      .withOptionalParamSet("proper-motion",         SiderealTarget.properMotion)
      .withOptionalParamSet("spectral-distribution", SiderealTarget.spectralDistribution)
      .withOptionalParamSet("spatial-profile",       SiderealTarget.spatialProfile)

  implicit val TooTargetParamSetCodec: ParamSetCodec[TooTarget] =
    ParamSetCodec.initial(TooTarget.empty)
      .withParam("name", TooTarget.name)

  implicit val CometParamSetCodec: ParamSetCodec[HorizonsDesignation.Comet] =
    ParamSetCodec.initial(HorizonsDesignation.Comet(""))
      .withParam("des", HorizonsDesignation.Comet.des)
      
  implicit val AsteroidParamSetCodec: ParamSetCodec[HorizonsDesignation.AsteroidNewStyle] =
    ParamSetCodec.initial(HorizonsDesignation.AsteroidNewStyle(""))
      .withParam("des", HorizonsDesignation.AsteroidNewStyle.des)
      
  implicit val AsteroidOldStyleParamSetCodec: ParamSetCodec[HorizonsDesignation.AsteroidOldStyle] =
    ParamSetCodec.initial(HorizonsDesignation.AsteroidOldStyle(0))
      .withParam("num", HorizonsDesignation.AsteroidOldStyle.num)
      
  implicit val MajorBodyParamSetCodec: ParamSetCodec[HorizonsDesignation.MajorBody] =
    ParamSetCodec.initial(HorizonsDesignation.MajorBody(0))
      .withParam("num", HorizonsDesignation.MajorBody.num)
      
  implicit val HorizonsDesignationParamSetCodec: ParamSetCodec[HorizonsDesignation] =
    new ParamSetCodec[HorizonsDesignation] {
      val pf = new PioXmlFactory
      def encode(key: String, a: HorizonsDesignation): ParamSet = {
        val (tag, ps) = a match {
          case d: HorizonsDesignation.Comet            => ("comet", CometParamSetCodec.encode(key, d))
          case d: HorizonsDesignation.AsteroidNewStyle => ("asteroid", AsteroidParamSetCodec.encode(key, d))
          case d: HorizonsDesignation.AsteroidOldStyle => ("asteroid-old-style", AsteroidOldStyleParamSetCodec.encode(key, d))
          case d: HorizonsDesignation.MajorBody        => ("major-body", MajorBodyParamSetCodec.encode(key, d))
        }
        Pio.addParam(pf, ps, "tag", tag)
        ps
      }    
      def decode(ps: ParamSet): PioError \/ HorizonsDesignation = {
        (Option(ps.getParam("tag")).map(_.getValue) \/> MissingKey("tag")) flatMap {
          case "comet"              => CometParamSetCodec.decode(ps)
          case "asteroid"           => AsteroidParamSetCodec.decode(ps)
          case "asteroid-old-style" => AsteroidOldStyleParamSetCodec.decode(ps)
          case "major-body"         => MajorBodyParamSetCodec.decode(ps)
          case hmm                  => UnknownTag(hmm, "HorizonsDesignation").left
        }
      }
    }

  implicit val EphemerisParamSetCodec: ParamSetCodec[Ephemeris] =
    ParamSetCodec.initial(Ephemeris.empty)
      .withParam("site", Ephemeris.site)
      .withParam("data", Ephemeris.compressedData)

  implicit val NonSiderealTargetParamSetCodec: ParamSetCodec[NonSiderealTarget] =
    ParamSetCodec.initial(NonSiderealTarget.empty)
      .withParam("name",                             NonSiderealTarget.name)
      .withOptionalParamSet("horizons-designation",  NonSiderealTarget.horizonsDesignation)
      .withParamSet("ephemeris",                     NonSiderealTarget.ephemeris)
      .withManyParamSet("magnitude",                 NonSiderealTarget.magnitudes)
      .withOptionalParamSet("spectral-distribution", NonSiderealTarget.spectralDistribution)
      .withOptionalParamSet("spatial-profile",       NonSiderealTarget.spatialProfile)

  implicit val TargetParamSetCodec: ParamSetCodec[Target] =
    new ParamSetCodec[Target] {
      val pf = new PioXmlFactory
      def encode(key: String, a: Target): ParamSet = {
        val (tag, ps) = a match {
          case t: SiderealTarget    => ("sidereal",    SiderealTargetParamSetCodec.encode(key, t))
          case t: TooTarget         => ("too",         TooTargetParamSetCodec.encode(key, t))
          case t: NonSiderealTarget => ("nonsidereal", NonSiderealTargetParamSetCodec.encode(key, t))
        }
        Pio.addParam(pf, ps, "tag", tag)
        ps
      }    
      def decode(ps: ParamSet): PioError \/ Target = {
        (Option(ps.getParam("tag")).map(_.getValue) \/> MissingKey("tag")) flatMap {
          case "sidereal"    => SiderealTargetParamSetCodec.decode(ps)
          case "too"         => TooTargetParamSetCodec.decode(ps)
          case "nonsidereal" => NonSiderealTargetParamSetCodec.decode(ps)
          case hmm           => UnknownTag(hmm, "Target").left
        }
      }
    }

  implicit val SPTargetParamSetCodec: ParamSetCodec[SPTarget] =
    TargetParamSetCodec.xmap(new SPTarget(_), _.getTarget)

}




