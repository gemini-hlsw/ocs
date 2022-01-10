package edu.gemini.model.p1.submit

import org.junit.Assert._
import org.junit.Test

import ResponseParser._
import java.text.SimpleDateFormat
import java.util.TimeZone
import edu.gemini.model.p1.submit.SubmitResult.{Success, ServiceError}

class ResponseParserTest {

  private def verifyUnexpected(input: String): Unit = {
    parse(input) match {
      case ServiceError(None, _, 500, UNEXPECTED_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testNotEvenXml(): Unit = {
    verifyUnexpected("foo")
  }

  @Test def testUnexpectedXml(): Unit = {
    parse(<unexpected></unexpected>.toString())
  }

  @Test def testNonIntegerErrorCode(): Unit = {
    verifyUnexpected(
      <body>
        <status>error</status>
        <errorCode>x</errorCode>
        <errorText>y</errorText>
      </body>.toString())
  }

  private def parseTime(s: String): Long = {
    val FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    FMT.setTimeZone(TimeZone.getTimeZone("UTC"))
    FMT.parse(s).getTime
  }

  @Test def testSuccess(): Unit = {
    parse(
      <body>
        <status>created</status>
        <partnerReferenceKey>key</partnerReferenceKey>
        <timestamp>2012-01-22T10:59:00Z</timestamp>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case Success("key", time, "x@y.com", "hi") =>
        assertEquals(time, parseTime("2012-01-22T10:59:00Z"))
      case ServiceError(_, _, code, message) => fail(message)
      case _ => fail()
    }
  }

  @Test def testNoPartnerRef(): Unit = {
    parse(
      <body>
        <status>created</status>
        <timestamp>2012-01-22T10:59:00Z</timestamp>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(None, _, 500, NO_PARTNER_REF_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testNoTimestamp(): Unit = {
    parse(
      <body>
        <status>created</status>
        <partnerReferenceKey>key</partnerReferenceKey>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(_, _, 500, NO_TIMESTAMP_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testBadTimestamp(): Unit = {
    parse(
      <body>
        <status>created</status>
        <partnerReferenceKey>key</partnerReferenceKey>
        <timestamp>2012-01-22 10:59:00Z</timestamp>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(_, _, 500, msg) =>
        assertEquals(BAD_TIMESTAMP_MSG("2012-01-22 10:59:00Z"), msg)
      case _ => fail()
    }
  }

  @Test def testValidError(): Unit = {
    parse(
      <body>
        <status>error</status>
        <errorCode>401</errorCode>
        <errorText>my error text</errorText>
      </body>.toString()) match {
      case ServiceError(_, _, 401, "my error text") => // ok
      case _ => fail()
    }
  }

}
