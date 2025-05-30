package edu.gemini.ictd.dao

import edu.gemini.pot.sp.Instrument

import doobie.imports._

import scalaz.Scalaz._

import shapeless.tag.@@
import shapeless.{ HNil, tag }

/** DAO for standard instrument features like filters and gratings. */
object FeatureTablesDao {

  final case class Feature(
    name:       String,
    id:         Option[String],
    instrument: Instrument,
    location:   Location
  )

  // Tag the various kinds of Feature to distinguish them.  They are otherwise
  // identical triplets of (name, Instrument, Location).
  type Circular   = Feature @@ CircularTag
  type Filter     = Feature @@ FilterTag
  type Grating    = Feature @@ GratingTag
  type Ifu        = Feature @@ IfuTag
  type Longslit   = Feature @@ LongslitTag
  type NSLongslit = Feature @@ NSLongslitTag

  val select: ConnectionIO[Map[Instrument, FeatureTables]] =
    for {
      cs <- Statements.selectCircular.list
      fs <- Statements.selectFilter.list
      gs <- Statements.selectGrating.list
      is <- Statements.selectIfu.list
      ls <- Statements.selectLongslit.list
      ns <- Statements.selectNsLongslit.list
    } yield featureTables(cs, fs, gs, is, ls, ns)

  private def featureTables(
    cs: List[Circular],
    fs: List[Filter],
    gs: List[Grating],
    is: List[Ifu],
    ls: List[Longslit],
    ns: List[NSLongslit]
  ): Map[Instrument, FeatureTables] = {

    def toAvailabilityTable[T](features: List[Feature @@ T]): AvailabilityTable @@ T =
      tag[T][AvailabilityTable](features.map(f => s"${f.id.fold(f.name)(id => s"${f.name}_$id")}" -> f.location.availability).toMap)

    def group[T](features: List[Feature @@ T]): Instrument => AvailabilityTable @@ T =
      features.groupBy(_.instrument)
              .mapValues(toAvailabilityTable)
              .withDefaultValue(tag[T][AvailabilityTable](Map.empty))

    val circular   = group(cs)
    val filter     = group(fs)
    val grating    = group(gs)
    val ifu        = group(is)
    val longslit   = group(ls)
    val nsLongslit = group(ns)

    Instrument.values.toList.fproduct { i =>
      FeatureTables(circular(i), filter(i), grating(i), ifu(i), longslit(i), nsLongslit(i))
    }.toMap

  }

  object Statements {

    import InstrumentMeta._
    import LocationMeta._

    def selectFeatureWithId[T](tableName: String): Query0[Feature @@ T] =
      Query[HNil, Feature](
        s"""
           SELECT f.Name,
                  f.GeminiID,
                  c.Instrument,
                  c.Location
             FROM $tableName f
                  LEFT OUTER JOIN Component c
                    ON c.ComponentID = f.ComponentID
         """).toQuery0(HNil).map(tag[T][Feature])

    def selectFeature[T](tableName: String): Query0[Feature @@ T] =
      Query[HNil, Feature](
        s"""
           SELECT f.Name,
                  null as GeminiID,
                  c.Instrument,
                  c.Location
             FROM $tableName f
                  LEFT OUTER JOIN Component c
                    ON c.ComponentID = f.ComponentID
         """).toQuery0(HNil).map(tag[T][Feature])

    val selectCircular: Query0[Circular] =
      selectFeature[CircularTag]("Circular")

    val selectFilter: Query0[Filter] =
      selectFeature[FilterTag]("Filter")

    val selectGrating: Query0[Grating] =
      selectFeatureWithId[GratingTag]("Grating")

    val selectIfu: Query0[Ifu] =
      selectFeature[IfuTag]("IFU")

    val selectLongslit: Query0[Longslit] =
      selectFeature[LongslitTag]("Longslit")

    val selectNsLongslit: Query0[NSLongslit] =
      selectFeature[NSLongslitTag]("NSLongslit")

  }

}
