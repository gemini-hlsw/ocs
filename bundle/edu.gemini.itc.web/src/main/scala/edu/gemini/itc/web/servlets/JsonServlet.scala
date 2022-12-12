package edu.gemini.itc.web.servlets

import argonaut._, Argonaut._
import edu.gemini.itc.shared.{ ItcParameters, ItcResult, ItcService }
import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.web.json.{ ItcParametersCodec, ItcResultCodec }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import javax.servlet.http.HttpServletResponse.{ SC_BAD_REQUEST, SC_OK }
import scala.io.Source
import scalaz._, Scalaz._

/**
 * This object is used to expose the itc core calculation as if it were the servlet
 * but called locally via reflection.
 * To avoid conflicts params are passed as Json encoded strings and the return is 
 * a json encoded string too.
 * This is certainly not the most efficient and other encodings maybe tested. In
 * particular passing the graph data as doubles should be explored
 *
 * This will be used by the scala-3 based itc graphql server for gpp
 */
object ItcCalculation extends ItcParametersCodec with ItcResultCodec {
  def calculation(json: String, versionToken: String): String = {
    val itc: ItcService = new ItcServiceImpl

    (for {
      itcReq <- Parse.decodeEither[ItcParameters](json)
      itcRes <- itc.calculateCharts(itcReq).toEither.leftMap(_.msg)
    } yield (itcRes.asJson.->:(("versionToken", jString(versionToken)))).nospaces).toString
  }
}

/**
 * Servlet that accepts a JSON-encoded `ItcParameters` as its POST payload (no other methods are
 * supported) and responds with a JSON-encoded `ItcResult` on success, or `SC_BAD_REQUEST` with
 * an error message on failure. JSON codecs are defined in package `edu.gemini.itc.web.json`.
 */
class JsonServlet(versionToken: String) extends HttpServlet with ItcParametersCodec with ItcResultCodec {
  def this() = this("")

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) = {

    // I don't know if this is threadsafe so we'll forge one per-request.
    val itc: ItcService = new ItcServiceImpl

    // Read the body, which with some luck is a JSON string
    val enc  = Option(req.getCharacterEncoding).getOrElse("UTF-8")
    val src  = Source.fromInputStream(req.getInputStream, enc)
    val json = try src.mkString finally src.close

    // Do the things.
    val result: Either[String, ItcResult] =
      for {
        itcReq <- Parse.decodeEither[ItcParameters](json)
        itcRes <- itc.calculate(itcReq, true).toEither.leftMap(_.msg)
      } yield itcRes

    // Send our result back.
    result match {
      case Left(err)     => res.sendError(SC_BAD_REQUEST, err)
      case Right(itcRes) =>
        res.setStatus(SC_OK)
        res.setContentType("text/json; charset=UTF-8")
        val writer = res.getWriter // can only be called once :-\
        val json: Json =
          if (versionToken.isEmpty)
            itcRes.asJson
          else
            itcRes.asJson.->:(("versionToken", jString(versionToken)))
        writer.write(json.spaces2)
        writer.close
    }

  }

}

/**
 * Servlet that accepts a JSON-encoded `ItcParameters` as its POST payload
 * and responds with a JSON-encoded `ItcResult` on success, or `SC_BAD_REQUEST` with
 * an error message on failure. JSON codecs are defined in package `edu.gemini.itc.web.json`.
 */
class JsonChartServlet(versionToken: String = "") extends HttpServlet with ItcParametersCodec with ItcResultCodec {
  def this() = this("")

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) = {

    val itc: ItcService = new ItcServiceImpl

    // Read the body, which with some luck is a JSON string
    val enc  = Option(req.getCharacterEncoding).getOrElse("UTF-8")
    val src  = Source.fromInputStream(req.getInputStream, enc)
    val json = try src.mkString finally src.close

    // Calculate the chart
    val result: Either[String, ItcResult] =
      for {
        itcReq <- Parse.decodeEither[ItcParameters](json)
        itcRes <- itc.calculateCharts(itcReq).toEither.leftMap(_.msg)
      } yield itcRes

    // Send our result back.
    result match {
      case Left(err)     => res.sendError(SC_BAD_REQUEST, err)
      case Right(itcRes) =>
        res.setStatus(SC_OK)
        res.setContentType("text/json; charset=UTF-8")
        val writer = res.getWriter
        val json: Json =
          if (versionToken.isEmpty)
            itcRes.asJson
          else
            itcRes.asJson.->:(("versionToken", jString(versionToken)))
        writer.write(json.spaces2)
        writer.close
    }

  }

}
