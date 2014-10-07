package edu.gemini.spModel.core

import org.specs2.mutable.Specification

object AffiliateSpec extends Specification {
  "Affiliates" should {
    "include Korea" in {
      // REL-2024 Add Korea as partner
      Affiliate.valueOf("KOREA") must not beNull
    }
  }
}
