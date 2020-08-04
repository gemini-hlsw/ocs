package edu.gemini.model.p1.immutable

import org.specs2.mutable._

class PartnersSpec extends Specification {

  "The Partners object" should {
    "not include the UK, REL-622" in {
      NgoPartner.values must have size 7
      NgoPartner.forName("UK") must throwA[Exception]
    }
    "must include Korea, REL-2019" in {
      NgoPartner.values must have size 7
      NgoPartner.forName("KR") must beEqualTo(NgoPartner.KR)
    }
  }
}