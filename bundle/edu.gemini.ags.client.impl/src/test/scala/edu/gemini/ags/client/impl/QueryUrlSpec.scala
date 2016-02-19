package edu.gemini.ags.client.impl

import org.specs2.mutable.Specification

class QueryUrlSpec extends Specification {
  "The QueryUrl class" should {
    "should guess the protocol from the port" in {
      val queryUrl = new QueryUrl("localhost", 8080)
      queryUrl.protocol must beEqualTo("http")
      val secureQueryUrl = new QueryUrl("localhost", 8443)
      secureQueryUrl.protocol must beEqualTo("https")
    }
  }
}
