package edu.gemini.spModel.dataset

import edu.gemini.spModel.core.catchingNonFatal
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{PioParseException, ParamSet, Param}
import edu.gemini.spModel.pio.codec._

import java.time.temporal.{TemporalAccessor, TemporalQuery}
import java.time.{ZoneId, Instant}
import java.time.format.DateTimeFormatter
import java.util.UUID

import scalaz._, Scalaz._

object DatasetCodecs {

  def explainPioError(e: PioError): String = e match {
    case MissingKey(n)       => s"Missing key $n"
    case NullValue(n)        => s"Null value for key $n"
    case ParseError(n, v, d) => s"Problem parsing $n, of type $d: $v"
    case UnknownTag(t, d)    => s"Encountered unknown tag $t while parsing a $d"
    case GeneralError(d)     => s"Problem while parsing a $d"
  }

  /** Decodes a param set to the expected type or else throws a
    * `PioParseException`.  This is sometimes useful when working with pre-codec
    * PIO code.
    */
  def unsafeDecode[A](ps: ParamSet)(implicit psc: ParamSetCodec[A]): A =
    psc.decode(ps).valueOr(e => throw new PioParseException(explainPioError(e)))

  implicit val ParamSetCodecDataset: ParamSetCodec[Dataset] =
    new ParamSetCodec[Dataset] {
      val pf = new PioXmlFactory

      def encode(key: String, a: Dataset): ParamSet =
        a.toParamSet(pf) <| (_.setName(key))

      def decode(ps: ParamSet): PioError \/ Dataset =
        catchingNonFatal {
          new Dataset(ps)
        }.leftMap(ex => ParseError(ps.getName, ex.getMessage, "Dataset"))
    }

  implicit val ParamCodecDatasetQaState: ParamCodec[DatasetQaState] =
    new ParamCodec[DatasetQaState] {
      def encode(key: String, a: DatasetQaState): Param =
        ParamCodec[String].encode(key, a.name)

      def decode(p: Param): PioError \/ DatasetQaState =
        ParamCodec[String].decode(p).flatMap { s =>
          Option(DatasetQaState.parseType(s)) \/> ParseError(p.getName, s, "DatasetQaState")
        }
    }

  val ParamCodecInstant: ParamCodec[Instant] =
    new ParamCodec[Instant] {
      val dtf = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("Z"))

      def encode(key: String, a: Instant): Param =
        ParamCodec[String].encode(key, dtf.format(a))

      def decode(p: Param): PioError \/ Instant =
        ParamCodec[String].decode(p).flatMap { s =>
          catchingNonFatal {
            dtf.parse(s, new TemporalQuery[Instant]() {
                        override def queryFrom(ta: TemporalAccessor): Instant = Instant.from(ta)
                      })
          }.leftMap(_ => ParseError(p.getName, s, "Instant"))
        }
    }

  val ParamCodecUuid: ParamCodec[UUID] =
    new ParamCodec[UUID] {
      def encode(key: String, a: UUID): Param =
        ParamCodec[String].encode(key, a.toString)

      def decode(p: Param): PioError \/ UUID =
        ParamCodec[String].decode(p).flatMap { s =>
          catchingNonFatal {
            UUID.fromString(s)
          }.leftMap(_ => ParseError(p.getName, s, "UUID"))
        }
    }
}
