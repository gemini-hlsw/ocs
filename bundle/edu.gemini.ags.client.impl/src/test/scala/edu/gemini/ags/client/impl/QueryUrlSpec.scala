package edu.gemini.ags.client.impl

import org.specs2.mutable.SpecificationWithJUnit

class QueryUrlSpec extends SpecificationWithJUnit {
  "The QueryUrl class" should {
    "should guess the protocl from the port" in {
      val queryUrl = new QueryUrl("localhost", 8080)
      queryUrl.protocol must beEqualTo("http")
      val secureQueryUrl = new QueryUrl("localhost", 8443)
      secureQueryUrl.protocol must beEqualTo("https")
    }
  }
}
