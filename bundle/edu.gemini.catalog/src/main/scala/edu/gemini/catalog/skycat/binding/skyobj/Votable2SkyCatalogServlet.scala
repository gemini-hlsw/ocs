package edu.gemini.catalog.skycat.binding.skyobj

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.VoTableClient
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.pot.ModelConverters._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

@Deprecated
class Votable2SkyCatalogServlet extends HttpServlet {
  private def magnitudes(t: SiderealTarget):String = {
    UCAC4.magnitudeBands.map {b =>
      t.magnitudeIn(b).map(m => f"${m.value}%3.3f").getOrElse(" ")
    }.mkString("\t")
  }
  private def properMotion(t: SiderealTarget):String = {
    val raV = t.properMotion.map(p => f"${p.deltaRA.velocity.masPerYear}%+03.07f").getOrElse(" ")
    val decV = t.properMotion.map(p => f"${p.deltaDec.velocity.masPerYear}%+03.07f").getOrElse(" ")
    s"$raV\t$decV"
  }
  private def toRow(t: SiderealTarget):String = f"${t.name}%-10s\t${t.coordinates.ra.toAngle.toDegrees}%+03.07f\t${t.coordinates.dec.toDegrees}%+03.07f\t${properMotion(t)}\t${magnitudes(t)}"
  private def headers:String = s"4UC\tRA\tDEC\tpmRA\tpmDEC\t${UCAC4.magnitudeBands.map(_.name).mkString("\t")}\n-"

  private val lowLimitMagRegex = """(.*)magLL""".r
  private val highLimitMagRegex = """(.*)magHL""".r
  private val magFilterRegex = """(.*)mag""".r
  private val magRangeFilterRegex = """(.*)\.\.(.*)""".r

  private def extractBand(band: String, value: String) = {
    val b = band match {
      // Special case f. means R
      case "f." => \/.right(MagnitudeBand.R)
      case x    => \/.fromTryCatch(Band.valueOf(x)).map(_.toNewModel)
    }
    val v = value.parseDouble.disjunction
    for {
      b0 <- b
      v0 <- v
    } yield (b0, v0)
  }

  def magnitudeExtractor(bands: List[MagnitudeBand]) = (st: SiderealTarget) => bands.flatMap(st.magnitudeIn).headOption

  def candidateBands(band: MagnitudeBand): List[MagnitudeBand] = band match {
      case MagnitudeBand.R => List(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC)
      case _               => List(band)
    }

  override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val params:Map[String, String] = req.getParameterMap.asScala.map(t => t._1.toString -> t._2.asInstanceOf[Array[String]](0)).toMap
    // Extract compulsory parameters
    val ra = params.get("ra")
    val dec = params.get("dec")
    val r1 = params.get("r1")
    val r2 = params.get("r2")
    val max = params.get("max")

    // Extract magnitude limits from the request
    val lowMagLimit = params.flatMap {
      case (lowLimitMagRegex(x), v) => (x, v).some
      case _                        => None
    }.headOption.map(Function.tupled(extractBand))
    val highMagLimit = params.flatMap {
      case (highLimitMagRegex(x), v) => (x, v).some
      case _                        => None
    }.headOption.map(Function.tupled(extractBand))

    // Extract additional magnitude filters for request's parameters
    val mags = params.flatMap {
      case (magFilterRegex(b), magRangeFilterRegex(u, l)) => (b, l, u).some
      case _                                              => None
    }

    val out = (ra |@| dec |@| r1 |@| r2){(r, d, ra1, ra2) =>
      for {
        raAngle      <- Angle.parseHMS(r)
        decAngle     <- Angle.parseDMS(d)
        innerRadius  <- ra1.parseDouble.disjunction
        outterRadius <- ra2.parseDouble.disjunction
      } yield {
        val coordinates = Coordinates(RightAscension.fromAngle(raAngle), Declination.fromAngle(decAngle).getOrElse(Declination.zero))
        val rc = RadiusConstraint.between(Angle.fromArcmin(innerRadius), Angle.fromArcmin(outterRadius))
        // Reference band should be the same for low and high limit
        val referenceBand = lowMagLimit.map(x => x.map(_._1)) >>= (_.toOption)

        // Build magnitude range
        val mr = (lowMagLimit |@| highMagLimit){(l, h) =>
          for {
            l0 <- l
            h0 <- h
          } yield MagnitudeRange(FaintnessConstraint(h0._2), SaturationConstraint(l0._2).some)
        }

        // Secondary filters, ignore unparsable parameters
        val magFilters = mags.collect {
            case (b, u, l) => for {
                u0 <- u.parseDouble.disjunction
                l0 <- l.parseDouble.disjunction
                b0 <- \/.fromTryCatch(Band.valueOf(b)).map(_.toNewModel)
              } yield MagnitudeConstraints(b0, FaintnessConstraint(u0), SaturationConstraint(l0).some)
          }.collect {
            case \/-(mc) => mc
          }

        val query = referenceBand.map { b =>
            CatalogQuery.catalogQueryRangeOnBand(coordinates, rc, magnitudeExtractor(candidateBands(b)), mr >>= (_.toOption))
          }.getOrElse {
            CatalogQuery.catalogQuery(coordinates, rc, None)
          }

        // Execute query
        val result = VoTableClient.catalog(query).map { q =>
          if (q.result.containsError) {
            q.result.problems.mkString(", ")
          } else {
            // Apply additional magnitude filters
            // TODO OCSADV-404 Support multiple magnitude filters
            val filteredRows = magFilters.foldLeft(q.result.targets.rows) { (r, f) =>
              r.filter(f.filter)
            }
            // Adjust count of resulting rows
            val countAdjustedRows = filteredRows.takeRight(max.map(_.parseInt.getOrElse(Int.MaxValue)).getOrElse(Int.MaxValue))

            s"$headers\n${countAdjustedRows.map(toRow).mkString("\n")}"
          }
        }

        // Servlet is synchronous, we need to Await
        Await.result(result, 30.seconds)
      }
    }
    out.fold(resp.setStatus(400)){
      _.fold(
        _ => resp.setStatus(404),
        v => {
          resp.setContentType("text/plain")
          resp.setStatus(200)
          resp.getWriter.print(v)
        }
      )
    }
  }
}
