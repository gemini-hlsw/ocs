package edu.gemini.model.p1.submit

import org.junit.Assert._
import org.junit.Test

import ResponseParser._
import java.text.SimpleDateFormat
import java.util.TimeZone
import edu.gemini.model.p1.submit.SubmitResult.{Success, ServiceError}

class ResponseParserTest {

  private def verifyUnexpected(input: String) {
    parse(input) match {
      case ServiceError(None, 500, UNEXPECTED_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testNotEvenXml() {
    verifyUnexpected("foo")
  }

  @Test def testUnexpectedXml() {
    parse(<unexpected></unexpected>.toString())
  }

  @Test def testNonIntegerErrorCode() {
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

  @Test def testSuccess() {
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
      case ServiceError(_, code, message) => fail(message)
      case _ => fail()
    }
  }

  @Test def testNoPartnerRef() {
    parse(
      <body>
        <status>created</status>
        <timestamp>2012-01-22T10:59:00Z</timestamp>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(None, 500, NO_PARTNER_REF_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testNoTimestamp() {
    parse(
      <body>
        <status>created</status>
        <partnerReferenceKey>key</partnerReferenceKey>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(_, 500, NO_TIMESTAMP_MSG) => // ok
      case _ => fail()
    }
  }

  @Test def testBadTimestamp() {
    parse(
      <body>
        <status>created</status>
        <partnerReferenceKey>key</partnerReferenceKey>
        <timestamp>2012-01-22 10:59:00Z</timestamp>
        <partnerContactEmail>x@y.com</partnerContactEmail>
        <userMessage>hi</userMessage>
      </body>.toString()
    ) match {
      case ServiceError(_, 500, msg) =>
        assertEquals(BAD_TIMESTAMP_MSG("2012-01-22 10:59:00Z"), msg)
      case _ => fail()
    }
  }
  @Test def testValidError() {
    parse(
      <body>
        <status>error</status>
        <errorCode>401</errorCode>
        <errorText>my error text</errorText>
      </body>.toString()) match {
      case ServiceError(_, 401, "my error text") => // ok
      case _ => fail()
    }
  }

}