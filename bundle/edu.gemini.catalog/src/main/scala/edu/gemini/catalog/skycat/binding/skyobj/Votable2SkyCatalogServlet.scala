package edu.gemini.catalog.skycat.binding.skyobj

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import edu.gemini.catalog.api.{CatalogQuery, RadiusConstraint}
import edu.gemini.catalog.votable.VoTableClient
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._

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
  private def toRow(t: SiderealTarget):String = f"${t.name}%-10s\t${t.coordinates.ra.toAngle.toDegrees}%+03.07f\t${t.coordinates.dec.toDegrees}%+03.07f\t${magnitudes(t)}"
  private def headers:String = s"4UC\tRA\tDEC\t${UCAC4.magnitudeBands.map(_.name).mkString("\t")}\n-"

  override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val params:Map[String, String] = req.getParameterMap.asScala.map(t => t._1.toString -> t._2.asInstanceOf[Array[String]](0)).toMap
    val ra = params.get("ra")
    val dec = params.get("dec")
    val r1 = params.get("r1")
    val r2 = params.get("r2")
    val out = (ra |@| dec |@| r1 |@| r2){(r, d, ra1, ra2) =>
      for {
        raAngle      <- Angle.parseHMS(r)
        decAngle     <- Angle.parseDMS(d)
        innerRadius  <- ra1.parseDouble.disjunction
        outterRadius <- ra2.parseDouble.disjunction
      } yield {
        val coordinates = Coordinates(RightAscension.fromAngle(raAngle), Declination.fromAngle(decAngle).getOrElse(Declination.zero))

        val rc = RadiusConstraint.between(Angle.fromArcmin(innerRadius), Angle.fromArcmin(outterRadius))

        val query = CatalogQuery.catalogQuery(coordinates, rc, None)

        val result = VoTableClient.catalog(query).map { q =>
          if (q.result.containsError) {
            q.result.problems.mkString(", ")
          } else {
            s"$headers\n${q.result.targets.rows.map(toRow).mkString("\n")}"
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
