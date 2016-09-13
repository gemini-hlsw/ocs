package edu.gemini.lchquery.servlet

import java.io.{BufferedOutputStream, ByteArrayOutputStream, IOException, PrintWriter}
import java.rmi.RemoteException
import java.security.Principal
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import javax.xml.bind.{JAXBContext, Marshaller}

import edu.gemini.odb.browser.QueryResult
import edu.gemini.pot.spdb.IDBDatabaseService

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * A web service that allows for simple get requests with some
  * query parameters that replies with an XML containing the program / observation / target data.
  *
  * <p>The queries can have any combination of the following parameters, the
  * values should allow the same wildcards (*,?) and OR operators (|) as does
  * the OT browser and missing parameters equal to "Any":</p>
  *
  * programSemester
  * programTitle
  * programReference
  * programActive
  * programCompleted
  * programNotifyPi
  * programRollover
  *
  * observationTooStatus
  * observationName
  * observationStatus
  * observationInstrument
  * observationAo
  * observationClass
  *
  * <p>A typical query will be something like this (queries all GN observations
  * and targets for Semester 2012B with the given observation status and using
  * a LGS):</p>
  *
  * <p>
  * http://localhost:8296/lchquery?programSemester=2012B&programReference=GN*&observationStatus=Phase2|For Review|In Review|For Activation|On Hold|Ready|Ongoing*&observationAo=Altair + LGS
  * </p>
  *
  * <p>The program active and completed flags and the too status might be
  * important at a later stage, also the program title and observation name in
  * case they keep "marking" the engineering programs by using distinct
  * program and/or observation names.</p>
  *
  * <p>See LCH-63:</p>
  */
final case class LchQueryServlet(odb: IDBDatabaseService, user: Set[Principal]) extends HttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    import LchQueryServlet.QueryResultToXml

    // Get the params from the HTTP request that exist and correspond to LchQueryParams.
    def extractParams[A <: LchQueryParam[_]](lst: List[A]): List[(A,String)] =
      for {
        p <- lst
        v <- Option(request.getParameter(p.name))
      } yield (p,v)

    val out: BufferedOutputStream = new BufferedOutputStream(response.getOutputStream)

    // Handle IllegalArgumentException.
    def illegalArgument(ex: IllegalArgumentException) = {
      response.setStatus(LchQueryServlet.HttpResponseCodes.InvalidRequest.code)
      response.setContentType("text/plain")
      out.write(s"ERROR: ${ex.getMessage}".getBytes)
    }

    // Handle other Throwables that may arise.
    def otherThrowable(t: Throwable, status: LchQueryServlet.HttpResponseCode): Unit = {
      LchQueryServlet.Log.log(Level.WARNING, "could not process request", t)
      response.setStatus(status.code)
      response.setContentType("text/plain")
      out.write(s"ERROR: ${t.getClass.getName}\n\n".getBytes)
      Option(t.getMessage).foreach(s => out.write(s"$s\n\n".getBytes))

      val writer = new PrintWriter(out)
      t.printStackTrace(writer)
      writer.close()
    }


    val pathInfo = request.getPathInfo
    LchQueryFunctor.queryType(pathInfo) match {
      case Some(queryType) =>
        val programParams     = extractParams(LchQueryParam.ProgramParams)
        val observationParams = extractParams(LchQueryParam.ObservationParams)

        Try {
          // Check that no illegal params have been supplied.
          for {
            p <- request.getParameterNames.asScala.map(_.asInstanceOf[String])
            if !LchQueryParam.validParamName(p)
          } throw new IllegalArgumentException(s"Invalid parameter: $p")

          // Handle the query.
          response.setStatus(LchQueryServlet.HttpResponseCodes.AllOK.code)
          response.setContentType("application/xml")

          odb.getQueryRunner(user.asJava).
            queryPrograms(new LchQueryFunctor(queryType, programParams, observationParams)).
            queryResult.toXml

        } recover {
          case ex: IllegalArgumentException           => illegalArgument(ex)
          case ex@(_:RemoteException | _:IOException) => otherThrowable(ex, LchQueryServlet.HttpResponseCodes.ServerError)
          case ex                                     => otherThrowable(ex, LchQueryServlet.HttpResponseCodes.InvalidRequest)
        }

      case None =>
        response.setStatus(LchQueryServlet.HttpResponseCodes.InvalidRequest.code)
        response.setContentType("text/plain")
        out.write(s"ERROR: Invalid query selector: $pathInfo. Should start with /programs, /observations or /targets".getBytes)
    }

    out.close()
  }
}

object LchQueryServlet {
  val Log = Logger.getLogger(LchQueryServlet.getClass.getName)

  sealed abstract class HttpResponseCode(val code: Int)
  object HttpResponseCodes {
    case object AllOK extends HttpResponseCode(200)
    case object InvalidRequest extends HttpResponseCode(400)
    case object ServerError extends HttpResponseCode(500)
  }

  private[LchQueryServlet] implicit class QueryResultToXml(val queryResult: QueryResult) extends AnyVal {
    def toXml: Array[Byte] = {
      val marshaller = JAXBContext.newInstance(classOf[QueryResult].getName.substring(0, classOf[QueryResult].getName.lastIndexOf("."))).createMarshaller()
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
      val bos = new ByteArrayOutputStream()
      marshaller.marshal(queryResult, bos)
      bos.toByteArray
    }
  }
}
