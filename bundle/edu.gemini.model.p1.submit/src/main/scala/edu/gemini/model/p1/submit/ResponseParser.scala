package edu.gemini.model.p1.submit

import xml.{XML, NodeSeq}
import java.util.logging.{Level, Logger}
import java.text.{ParseException, SimpleDateFormat}
import java.util.TimeZone
import edu.gemini.model.p1.submit.SubmitResult.{ServiceError, Success, Failure}

object ResponseParser {
  private val LOG = Logger.getLogger("edu.gemini.model.p1.submit.ResponseParser")

  val UNEXPECTED_MSG     = "The submission service returned an unexpected result."
  val NO_PARTNER_REF_MSG = "The submission service did not return a proposal reference."
  val NO_TIMESTAMP_MSG   = "The submission service did not return a timestamp."
  def BAD_TIMESTAMP_MSG(s: String) = "Couldn't parse the timestamp returned by the submission service: '%s'.".format(s)

  def parse(s: String): SubmitResult =
    toXml(s) match {
      case Left(failure) => failure
      case Right(n)      => read(n)
    }

  private def toXml(s: String): Either[Failure, NodeSeq] =
    try {
      Right(XML.loadString(s))
    } catch {
      case ex: Exception =>
        LOG.log(Level.INFO, "Couldn't parse submission response: %s".format(s), ex)
        Left(ServiceError(None, None, 500, UNEXPECTED_MSG))
    }

  private def read(n: NodeSeq): SubmitResult =
    if ("created" == (n \ "status").text)
      readSuccess(n)
    else
      readError(n)

  private def readSuccess(n: NodeSeq): SubmitResult = {
    val res = for {
      ref <- readPartnerRef(n).right
      timestamp <- readTimestamp(n).right
    } yield Success(ref, timestamp, (n \ "partnerContactEmail").text, (n \ "userMessage").text)

    res.right.getOrElse(res.left.get)
  }

  private def readPartnerRef(n: NodeSeq): Either[Failure, String] =
    (n \ "partnerReferenceKey").text match {
      case ""  => Left(ServiceError(None, None, 500, NO_PARTNER_REF_MSG))
      case ref => Right(ref)
    }

  private def readTimestamp(n: NodeSeq): Either[Failure, Long] =
    (n \ "timestamp").text match {
      case "" => Left(ServiceError(None, None, 500, NO_TIMESTAMP_MSG))
      case s  =>
        try {
          val FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
          FMT.setTimeZone(TimeZone.getTimeZone("UTC"))
          Right(FMT.parse(s).getTime)
        } catch {
          case ex: ParseException => Left(ServiceError(None, None, 500, BAD_TIMESTAMP_MSG(s)))
        }
    }

  private def readError(n: NodeSeq): SubmitResult = {
    val code = (n \ "errorCode").text
    val msg  = (n \ "errorText").text
    val url  = (n \ "referenceUrl").text

    LOG.info("Received submission error response: code=%s, msg=%s, url=%s:\n%s".format(code, msg, url, n.toString()))

    if (code.isEmpty)
      ServiceError(None, None, 500, UNEXPECTED_MSG)
    else
      try {
        ServiceError(None, None, code.toInt, msg)
      } catch {
        case _: Exception => ServiceError(None, None, 500, UNEXPECTED_MSG)
      }
  }
}